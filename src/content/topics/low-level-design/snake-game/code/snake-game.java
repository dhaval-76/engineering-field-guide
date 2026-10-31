enum Direction {
    UP(-1, 0), DOWN(1, 0), LEFT(0, -1), RIGHT(0, 1);
    final int dr, dc;
    Direction(int dr, int dc) { this.dr = dr; this.dc = dc; }
    boolean isOpposite(Direction o) { return dr == -o.dr && dc == -o.dc; }
}

record Cell(int row, int col) { Cell step(Direction d) { return new Cell(row + d.dr, col + d.dc); } }

enum Status { MOVED, ATE, GAME_OVER, WON }

final class SnakeGame {
    private final int rows, cols;
    private final Deque<Cell> body = new ArrayDeque<>();      // head = first, tail = last
    private final Set<Cell> occupied = new HashSet<>();       // O(1) self-collision
    private final FoodSpawner food;
    private Direction direction = Direction.RIGHT, pending = Direction.RIGHT;
    private Cell foodAt;
    private int score;
    private boolean over;

    SnakeGame(int rows, int cols, FoodSpawner food) {
        this.rows = rows; this.cols = cols; this.food = food;
        Cell start = new Cell(rows / 2, cols / 2);
        body.addFirst(start);
        occupied.add(start);
        foodAt = food.spawn(rows, cols, occupied);
    }

    /**
     * Input is buffered and applied on the next tick; reversing into yourself is ignored.
     */
    void turn(Direction d) { if (!d.isOpposite(direction)) pending = d; }

    Status tick() {
        if (over) return Status.GAME_OVER;
        direction = pending;
        Cell next = body.peekFirst().step(direction);

        if (next.row() < 0 || next.row() >= rows || next.col() < 0 || next.col() >= cols) return end();

        boolean eats = next.equals(foodAt);
        // the tail moves away THIS tick…
        if (!eats) {
            Cell tail = body.removeLast();
            occupied.remove(tail);
        }
        // …so moving into the old tail cell is legal
        if (occupied.contains(next)) return end();
        body.addFirst(next);
        occupied.add(next);

        if (!eats) return Status.MOVED;
        score++;
        if (occupied.size() == rows * cols) { over = true; return Status.WON; }
        foodAt = food.spawn(rows, cols, occupied);
        return Status.ATE;
    }

    private Status end() { over = true; return Status.GAME_OVER; }
    int score() { return score; }
    List<Cell> body() { return List.copyOf(body); }
}

interface FoodSpawner { Cell spawn(int rows, int cols, Set<Cell> occupied); }

/**
 * Rejection sampling: fast while the board is mostly empty; fall back to scanning when
 * crowded.
 */
final class RandomFood implements FoodSpawner {
    private final Random rnd;
    // seeded Random → reproducible tests
    RandomFood(Random rnd) { this.rnd = rnd; }
    public Cell spawn(int rows, int cols, Set<Cell> occupied) {
        for (int tries = 0; tries < 32; tries++) {
            Cell c = new Cell(rnd.nextInt(rows), rnd.nextInt(cols));
            if (!occupied.contains(c)) return c;
        }
        List<Cell> free = new ArrayList<>();
        for (int r = 0; r < rows; r++) for (int c = 0; c < cols; c++) { Cell x = new Cell(r, c); if (!occupied.contains(x)) free.add(x); }
        return free.get(rnd.nextInt(free.size()));
    }
}
