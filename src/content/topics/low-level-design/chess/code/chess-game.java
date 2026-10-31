final class Board {
    private final Piece[][] grid = new Piece[8][8];

    Optional<Piece> at(Square s) {
        return Optional.ofNullable(grid[s.file()][s.rank()]);
    }

    void put(Square s, Piece p) {
        grid[s.file()][s.rank()] = p;
    }

    Piece remove(Square s) {
        Piece p = grid[s.file()][s.rank()];
        grid[s.file()][s.rank()] = null;
        return p;
    }

    Square kingOf(Color c) {
        for (int f = 0; f < 8; f++)
            for (int r = 0; r < 8; r++)
                if (grid[f][r] instanceof King k && k.color == c) return new Square(f, r);
        throw new IllegalStateException("no king");
    }

    boolean isAttacked(Square target, Color by) {
        for (int f = 0; f < 8; f++)
            for (int r = 0; r < 8; r++) {
                Piece p = grid[f][r];
                if (p != null
                        && p.color == by
                        && p.candidateMoves(this, new Square(f, r)).contains(target)) return true;
            }
        return false;
    }
}

/** A move is a Command: it knows how to apply and fully revert itself. */
record Move(Square from, Square to, Piece moved, Piece captured) {
    void apply(Board b) {
        b.remove(from);
        b.put(to, moved);
    }

    void revert(Board b) {
        b.put(from, moved);
        b.put(to, captured);
    }
}

enum GameStatus {
    ACTIVE,
    CHECK,
    CHECKMATE,
    STALEMATE
}

final class Game {
    private final Board board;
    private Color turn = Color.WHITE;
    private final Deque<Move> history = new ArrayDeque<>();

    Game(Board board) {
        this.board = board;
    }

    /** Legal = the piece's own rules AND the mover's king is not left in check. */
    List<Move> legalMoves(Square from) {
        Piece p =
                board.at(from)
                        .filter(x -> x.color == turn)
                        .orElseThrow(() -> new IllegalArgumentException("not your piece"));
        List<Move> legal = new ArrayList<>();
        for (Square to : p.candidateMoves(board, from)) {
            Move m = new Move(from, to, p, board.at(to).orElse(null));
            m.apply(board); // simulate…
            boolean safe = !board.isAttacked(board.kingOf(turn), turn.opponent());
            m.revert(board); // …and undo
            if (safe) legal.add(m);
        }
        return legal;
    }

    GameStatus play(Square from, Square to) {
        Move m =
                legalMoves(from).stream()
                        .filter(x -> x.to().equals(to))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("illegal move"));
        m.apply(board);
        history.push(m);
        turn = turn.opponent();
        return status();
    }

    void undo() {
        if (history.isEmpty()) return;
        history.pop().revert(board);
        turn = turn.opponent();
    }

    GameStatus status() {
        boolean inCheck = board.isAttacked(board.kingOf(turn), turn.opponent());
        boolean anyMove =
                allSquares()
                        .anyMatch(
                                s ->
                                        board.at(s).filter(p -> p.color == turn).isPresent()
                                                && !legalMoves(s).isEmpty());
        if (anyMove) return inCheck ? GameStatus.CHECK : GameStatus.ACTIVE;
        return inCheck ? GameStatus.CHECKMATE : GameStatus.STALEMATE;
    }

    private static Stream<Square> allSquares() {
        return IntStream.range(0, 64).mapToObj(i -> new Square(i % 8, i / 8));
    }
}
