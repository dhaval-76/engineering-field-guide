enum Color { WHITE, BLACK; Color opponent() { return this == WHITE ? BLACK : WHITE; } }

record Square(int file, int rank) {                         // file 0–7 = a–h, rank 0–7 = 1–8
    boolean onBoard() { return file >= 0 && file < 8 && rank >= 0 && rank < 8; }
    Square plus(int df, int dr) { return new Square(file + df, rank + dr); }
}

abstract sealed class Piece permits King, Queen, Rook, Bishop, Knight, Pawn {
    final Color color;
    Piece(Color color) { this.color = color; }

    /** Squares this piece attacks/moves to by its own rules — ignoring whether the king is left in check. */
    abstract List<Square> candidateMoves(Board board, Square from);

    /** Shared helper for sliding pieces (rook, bishop, queen). */
    protected List<Square> slide(Board b, Square from, int[][] dirs) {
        List<Square> out = new ArrayList<>();
        for (int[] d : dirs) {
            Square s = from.plus(d[0], d[1]);
            while (s.onBoard()) {
                Optional<Piece> p = b.at(s);
                if (p.isEmpty()) { out.add(s); }
                else { if (p.get().color != color) out.add(s); break; }   // capture, then stop
                s = s.plus(d[0], d[1]);
            }
        }
        return out;
    }
}

final class Rook extends Piece {
    Rook(Color c) { super(c); }
    List<Square> candidateMoves(Board b, Square from) { return slide(b, from, new int[][]{{1,0},{-1,0},{0,1},{0,-1}}); }
}
final class Bishop extends Piece {
    Bishop(Color c) { super(c); }
    List<Square> candidateMoves(Board b, Square from) { return slide(b, from, new int[][]{{1,1},{1,-1},{-1,1},{-1,-1}}); }
}
final class Queen extends Piece {
    Queen(Color c) { super(c); }
    List<Square> candidateMoves(Board b, Square from) {
        return slide(b, from, new int[][]{{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}});
    }
}
final class Knight extends Piece {
    Knight(Color c) { super(c); }
    List<Square> candidateMoves(Board b, Square from) {
        int[][] jumps = {{1,2},{2,1},{-1,2},{-2,1},{1,-2},{2,-1},{-1,-2},{-2,-1}};
        List<Square> out = new ArrayList<>();
        for (int[] j : jumps) {
            Square s = from.plus(j[0], j[1]);
            if (s.onBoard() && b.at(s).map(p -> p.color != color).orElse(true)) out.add(s);   // jumps; no path check
        }
        return out;
    }
}
final class King extends Piece {
    King(Color c) { super(c); }
    List<Square> candidateMoves(Board b, Square from) {
        List<Square> out = new ArrayList<>();
        for (int df = -1; df <= 1; df++) for (int dr = -1; dr <= 1; dr++) {
            if (df == 0 && dr == 0) continue;
            Square s = from.plus(df, dr);
            if (s.onBoard() && b.at(s).map(p -> p.color != color).orElse(true)) out.add(s);
        }
        return out;                                             // castling handled by Game (needs history)
    }
}
final class Pawn extends Piece {
    Pawn(Color c) { super(c); }
    List<Square> candidateMoves(Board b, Square from) {
        int dir = color == Color.WHITE ? 1 : -1, startRank = color == Color.WHITE ? 1 : 6;
        List<Square> out = new ArrayList<>();
        Square one = from.plus(0, dir);
        if (one.onBoard() && b.at(one).isEmpty()) {
            out.add(one);
            Square two = from.plus(0, 2 * dir);
            if (from.rank() == startRank && b.at(two).isEmpty()) out.add(two);
        }
        for (int df : new int[]{-1, 1}) {                       // diagonal captures only
            Square s = from.plus(df, dir);
            if (s.onBoard() && b.at(s).map(p -> p.color != color).orElse(false)) out.add(s);
        }
        return out;                                             // en passant & promotion handled by Game
    }
}
