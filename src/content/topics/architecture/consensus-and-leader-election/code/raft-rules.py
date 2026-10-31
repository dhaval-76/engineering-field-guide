class RaftNode:
    """Two core Raft rules: granting votes and advancing the commit index. (Networking, timers omitted.)"""

    def __init__(self):
        self.current_term = 0
        self.voted_for: str | None = None
        self.log_terms: list[int] = []        # term of each entry; log index i is log_terms[i - 1]

    def last_log_index(self) -> int:
        return len(self.log_terms)

    def last_log_term(self) -> int:
        return self.log_terms[-1] if self.log_terms else 0

    def handle_request_vote(self, term: int, candidate: str, cand_last_index: int, cand_last_term: int) -> bool:
        if term < self.current_term:
            return False                                   # stale candidate
        if term > self.current_term:
            self.current_term, self.voted_for = term, None # newer term: new vote
        up_to_date = (cand_last_term > self.last_log_term() or
                      (cand_last_term == self.last_log_term() and cand_last_index >= self.last_log_index()))
        if self.voted_for in (None, candidate) and up_to_date:
            self.voted_for = candidate                     # persist before replying
            return True
        return False

    def advance_commit_index(self, match_index: list[int], commit_index: int) -> int:
        """match_index: highest replicated index on every node, including the leader."""
        majority_index = sorted(match_index)[(len(match_index) - 1) // 2]
        if majority_index > commit_index and self.log_terms[majority_index - 1] == self.current_term:
            return majority_index
        return commit_index
