from dataclasses import dataclass

ORTHO = [(1, 0), (-1, 0), (0, 1), (0, -1)]
DIAG = [(1, 1), (1, -1), (-1, 1), (-1, -1)]
KNIGHT = [(1, 2), (2, 1), (-1, 2), (-2, 1), (1, -2), (2, -1), (-1, -2), (-2, -1)]

def on_board(f: int, r: int) -> bool:
    return 0 <= f < 8 and 0 <= r < 8

@dataclass(frozen=True)
class Piece:
    kind: str          # king | queen | rook | bishop | knight | pawn
    color: str         # WHITE | BLACK

    def candidate_moves(self, board, sq: tuple[int, int]) -> list[tuple[int, int]]:
        """Pseudo-legal moves: the piece's movement rules, ignoring whether the king is left in check."""
        gen = {"rook": lambda: self._slide(board, sq, ORTHO), "bishop": lambda: self._slide(board, sq, DIAG),
               "queen": lambda: self._slide(board, sq, ORTHO + DIAG), "knight": lambda: self._steps(board, sq, KNIGHT),
               "king": lambda: self._steps(board, sq, ORTHO + DIAG), "pawn": lambda: self._pawn(board, sq)}
        return gen[self.kind]()

    def _slide(self, board, sq, dirs):
        out = []
        for df, dr in dirs:
            f, r = sq[0] + df, sq[1] + dr
            while on_board(f, r):
                p = board.at((f, r))
                if p is None:
                    out.append((f, r))
                else:
                    if p.color != self.color:
                        out.append((f, r))          # capture, then stop
                    break
                f, r = f + df, r + dr
        return out

    def _steps(self, board, sq, deltas):
        return [(sq[0] + df, sq[1] + dr) for df, dr in deltas
                if on_board(sq[0] + df, sq[1] + dr) and getattr(board.at((sq[0] + df, sq[1] + dr)), "color", None) != self.color]

    def _pawn(self, board, sq):
        f, r = sq
        d, start = (1, 1) if self.color == "WHITE" else (-1, 6)
        out = []
        if on_board(f, r + d) and board.at((f, r + d)) is None:
            out.append((f, r + d))
            if r == start and board.at((f, r + 2 * d)) is None:
                out.append((f, r + 2 * d))
        for df in (-1, 1):                          # diagonal captures only
            p = board.at((f + df, r + d)) if on_board(f + df, r + d) else None
            if p is not None and p.color != self.color:
                out.append((f + df, r + d))
        # en passant & promotion handled by Game
        return out
