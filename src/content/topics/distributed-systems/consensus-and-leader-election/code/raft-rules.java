/**
 * Two core Raft rules: granting votes and advancing the commit index. (Networking, timers
 * omitted.)
 */
class RaftNode {
    long currentTerm = 0;
    String votedFor = null;
    // term of each entry; log index i is logTerms.get(i - 1)
    final List<Long> logTerms = new ArrayList<>();

    long lastLogIndex() { return logTerms.size(); }
    long lastLogTerm()  { return logTerms.isEmpty() ? 0 : logTerms.get(logTerms.size() - 1); }

    /**
     * RequestVote: grant at most one vote per term, and only to a candidate whose log is
     * at least as up to date.
     */
    boolean handleRequestVote(long term, String candidate, long candLastIndex, long candLastTerm) {
        if (term < currentTerm) return false;                         // stale candidate
        // newer term: step down, new vote
        if (term > currentTerm) { currentTerm = term; votedFor = null; }
        boolean upToDate = candLastTerm > lastLogTerm()
                || (candLastTerm == lastLogTerm() && candLastIndex >= lastLogIndex());
        if ((votedFor == null || votedFor.equals(candidate)) && upToDate) {
            // persist before replying
            votedFor = candidate;
            return true;
        }
        return false;
    }

    /**
     * Leader: an entry is committed once a majority stores it — but only entries from the
     * leader's current term are committed by counting (older ones commit indirectly).
     * matchIndex holds the highest replicated index on every node, including the leader.
     */
    long advanceCommitIndex(long[] matchIndex, long commitIndex) {
        long[] sorted = matchIndex.clone();
        Arrays.sort(sorted);
        // replicated on a majority
        long majorityIndex = sorted[(sorted.length - 1) / 2];
        if (majorityIndex > commitIndex && logTerms.get((int) majorityIndex - 1) == currentTerm)
            return majorityIndex;
        return commitIndex;
    }
}
