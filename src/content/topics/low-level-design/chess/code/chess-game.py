# uses Piece from the model above; squares are (file, rank)
from dataclasses import dataclass

class Board:
    def __init__(self):
        self._grid: dict[tuple[int, int], "Piece"] = {}

    def at(self, sq):
        return self._grid.get(sq)

    def put(self, sq, piece) -> None:
        if piece is None:
            self._grid.pop(sq, None)
        else:
            self._grid[sq] = piece

    def remove(self, sq):
        return self._grid.pop(sq, None)

    def king_of(self, color: str):
        return next(sq for sq, p in self._grid.items() if p.kind == "king" and p.color == color)

    def is_attacked(self, target, by: str) -> bool:
        return any(p.color == by and target in p.candidate_moves(self, sq) for sq, p in list(self._grid.items()))

@dataclass(frozen=True)
class Move:
    """A Command: it knows how to apply and fully revert itself."""
    frm: tuple
    to: tuple
    moved: "Piece"
    captured: "Piece | None"

    def apply(self, b: Board) -> None:
        b.remove(self.frm)
        b.put(self.to, self.moved)

    def revert(self, b: Board) -> None:
        b.put(self.frm, self.moved)
        b.put(self.to, self.captured)

def other(color: str) -> str:
    return "BLACK" if color == "WHITE" else "WHITE"

class Game:
    def __init__(self, board: Board):
        self.board, self.turn, self._history = board, "WHITE", []

    def legal_moves(self, frm) -> list[Move]:
        """Legal = the piece's own rules AND the mover's king is not left in check."""
        p = self.board.at(frm)
        if p is None or p.color != self.turn:
            raise ValueError("not your piece")
        legal = []
        for to in p.candidate_moves(self.board, frm):
            m = Move(frm, to, p, self.board.at(to))
            m.apply(self.board)                                        # simulate…
            safe = not self.board.is_attacked(self.board.king_of(self.turn), other(self.turn))
            m.revert(self.board)                                       # …and undo
            if safe:
                legal.append(m)
        return legal

    def play(self, frm, to) -> str:
        m = next((x for x in self.legal_moves(frm) if x.to == to), None)
        if m is None:
            raise ValueError("illegal move")
        m.apply(self.board)
        self._history.append(m)
        self.turn = other(self.turn)
        return self.status()

    def undo(self) -> None:
        if self._history:
            self._history.pop().revert(self.board)
            self.turn = other(self.turn)

    def status(self) -> str:
        in_check = self.board.is_attacked(self.board.king_of(self.turn), other(self.turn))
        any_move = any(p.color == self.turn and self.legal_moves(sq) for sq, p in list(self.board._grid.items()))
        if any_move:
            return "CHECK" if in_check else "ACTIVE"
        return "CHECKMATE" if in_check else "STALEMATE"
