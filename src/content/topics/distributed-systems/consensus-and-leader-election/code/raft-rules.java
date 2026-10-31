/** Two core Raft rules: granting votes and advancing the commit index. (Networking, timers omitted.) */
class RaftNode {
    long currentTerm = 0;
    String votedFor = null;
    final List<Long> logTerms = new ArrayList<>();      // term of each entry; log index i is logTerms.get(i - 1)

    long lastLogIndex() { return logTerms.size(); }
    long lastLogTerm()  { return logTerms.isEmpty() ? 0 : logTerms.get(logTerms.size() - 1); }

    /** RequestVote: grant at most one vote per term, and only to a candidate whose log is at least as up to date. */
    boolean handleRequestVote(long term, String candidate, long candLastIndex, long candLastTerm) {
        if (term < currentTerm) return false;                         // stale candidate
        if (term > currentTerm) { currentTerm = term; votedFor = null; }  // newer term: step down, new vote
        boolean upToDate = candLastTerm > lastLogTerm()
                || (candLastTerm == lastLogTerm() && candLastIndex >= lastLogIndex());
        if ((votedFor == null || votedFor.equals(candidate)) && upToDate) {
            votedFor = candidate;                                     // persist before replying
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
        long majorityIndex = sorted[(sorted.length - 1) / 2];           // replicated on a majority
        if (majorityIndex > commitIndex && logTerms.get((int) majorityIndex - 1) == currentTerm)
            return majorityIndex;
        return commitIndex;
    }
}
