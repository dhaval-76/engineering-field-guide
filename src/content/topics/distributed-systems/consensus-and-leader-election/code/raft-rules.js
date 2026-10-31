// Two core Raft rules: granting votes and advancing the commit index. (Networking, timers
// omitted.)
export class RaftNode {
  currentTerm = 0;
  votedFor = null;
  logTerms = []; // term of each entry; log index i is logTerms[i - 1]

  lastLogIndex() {
    return this.logTerms.length;
  }
  lastLogTerm() {
    return this.logTerms.at(-1) ?? 0;
  }

  handleRequestVote(term, candidate, candLastIndex, candLastTerm) {
    if (term < this.currentTerm) return false;
    if (term > this.currentTerm) {
      this.currentTerm = term;
      this.votedFor = null;
    }
    const upToDate =
      candLastTerm > this.lastLogTerm() ||
      (candLastTerm === this.lastLogTerm() && candLastIndex >= this.lastLogIndex());
    if ((this.votedFor === null || this.votedFor === candidate) && upToDate) {
      this.votedFor = candidate;
      return true;
    }
    return false;
  }

  // matchIndex: highest replicated index on every node, including the leader
  advanceCommitIndex(matchIndex, commitIndex) {
    const sorted = [...matchIndex].sort((a, b) => a - b);
    const majorityIndex = sorted[Math.floor((sorted.length - 1) / 2)];
    if (majorityIndex > commitIndex && this.logTerms[majorityIndex - 1] === this.currentTerm) return majorityIndex;
    return commitIndex;
  }
}
