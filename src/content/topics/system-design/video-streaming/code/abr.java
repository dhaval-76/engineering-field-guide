record Rendition(String name, int kbps) {}

/**
 * Adaptive bitrate selection, run before each segment download. Throughput: exponentially
 * weighted average of measured segment download rates. Buffer rules: below a panic level
 * drop to the lowest; step up only one rung at a time, with buffer to spare.
 */
class AbrController {
    private final List<Rendition> ladder;                 // sorted by bitrate, ascending
    private double estimateKbps = -1;
    private int current = 0;

    AbrController(List<Rendition> ladder) { this.ladder = ladder; }

    void onSegmentDownloaded(long bits, double seconds) {
        double sample = bits / 1000.0 / seconds;
        // smooth spikes
        estimateKbps = estimateKbps < 0 ? sample : 0.7 * estimateKbps + 0.3 * sample;
    }

    Rendition next(double bufferSeconds) {
        // start low: fast first frame
        if (estimateKbps < 0) return ladder.get(current = 0);
        // about to stall: safest rung
        if (bufferSeconds < 5) return ladder.get(current = 0);
        // keep 20% headroom
        double budget = 0.8 * estimateKbps;
        int best = 0;
        for (int i = 0; i < ladder.size(); i++) if (ladder.get(i).kbps() <= budget) best = i;
        // climb slowly
        if (best > current) best = bufferSeconds > 15 ? current + 1 : current;
        // drops happen immediately
        current = best;
        return ladder.get(current);
    }
}
