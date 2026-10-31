record Rendition(String name, int kbps) {}

/**
 * Adaptive bitrate selection, run before each segment download.
 * Throughput: exponentially weighted average of measured segment download rates.
 * Buffer rules: below a panic level drop to the lowest; step up only one rung at a time, with buffer to spare.
 */
class AbrController {
    private final List<Rendition> ladder;                 // sorted by bitrate, ascending
    private double estimateKbps = -1;
    private int current = 0;

    AbrController(List<Rendition> ladder) { this.ladder = ladder; }

    void onSegmentDownloaded(long bits, double seconds) {
        double sample = bits / 1000.0 / seconds;
        estimateKbps = estimateKbps < 0 ? sample : 0.7 * estimateKbps + 0.3 * sample;   // smooth spikes
    }

    Rendition next(double bufferSeconds) {
        if (estimateKbps < 0) return ladder.get(current = 0);          // start low: fast first frame
        if (bufferSeconds < 5) return ladder.get(current = 0);         // about to stall: safest rung
        double budget = 0.8 * estimateKbps;                            // keep 20% headroom
        int best = 0;
        for (int i = 0; i < ladder.size(); i++) if (ladder.get(i).kbps() <= budget) best = i;
        if (best > current) best = bufferSeconds > 15 ? current + 1 : current;   // climb slowly
        current = best;                                                // drops happen immediately
        return ladder.get(current);
    }
}
