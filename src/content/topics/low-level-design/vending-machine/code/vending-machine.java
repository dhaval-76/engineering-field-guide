record Product(String code, String name, int price) {}

/** Either a vended product with change, or a message and nothing taken. */
record Result(Product product, List<Integer> change, String message) {
    static Result vended(Product p, List<Integer> change) { return new Result(p, change, "Enjoy your " + p.name()); }
    static Result refused(String message) { return new Result(null, List.of(), message); }
}

class VendingMachine {
    enum State { IDLE, HAS_CREDIT, OUT_OF_SERVICE }
    // coin denominations (₹)
    static final Set<Integer> ACCEPTED = Set.of(1, 2, 5, 10, 20);

    private final Map<String, Product> products = new HashMap<>();
    private final Map<String, Integer> stock = new HashMap<>();
    // denomination → count
    private final Map<Integer, Integer> coinBox = new TreeMap<>();
    // this customer's coins
    private final List<Integer> inserted = new ArrayList<>();
    private State state = State.IDLE;

    // ---- customer actions ----
    synchronized boolean insertCoin(int coin) {
        // drops to the return tray
        if (state == State.OUT_OF_SERVICE || !ACCEPTED.contains(coin)) return false;
        inserted.add(coin);
        state = State.HAS_CREDIT;
        return true;
    }

    synchronized Result select(String code) {
        if (state != State.HAS_CREDIT) return Result.refused("Insert money first");
        Product p = products.get(code);
        if (p == null || stock.getOrDefault(code, 0) == 0) return Result.refused("Sold out");
        int credit = credit();
        if (credit < p.price()) return Result.refused("Insert ₹" + (p.price() - credit) + " more");

        // change may reuse the customer's coins
        Map<Integer, Integer> pool = new TreeMap<>(coinBox);
        inserted.forEach(c -> pool.merge(c, 1, Integer::sum));
        Optional<Map<Integer, Integer>> change = makeChange(credit - p.price(), pool);
        // keep credit; customer can cancel
        if (change.isEmpty()) return Result.refused("Exact change only");

        // Commit: everything below happens together.
        inserted.forEach(c -> coinBox.merge(c, 1, Integer::sum));
        List<Integer> coinsOut = new ArrayList<>();
        change.get().forEach((coin, n) -> {
            coinBox.merge(coin, -n, Integer::sum);
            for (int i = 0; i < n; i++) coinsOut.add(coin);
        });
        stock.merge(code, -1, Integer::sum);
        inserted.clear();
        state = State.IDLE;
        // hardware: dispense, then pay out
        return Result.vended(p, coinsOut);
    }

    synchronized List<Integer> cancel() {
        // return the exact coins inserted
        List<Integer> refund = List.copyOf(inserted);
        inserted.clear();
        if (state == State.HAS_CREDIT) state = State.IDLE;
        return refund;
    }

    synchronized int credit() { return inserted.stream().mapToInt(Integer::intValue).sum(); }

    // ---- operator actions ----
    synchronized void addProduct(Product p, int quantity) { products.put(p.code(), p); stock.merge(p.code(), quantity, Integer::sum); }
    synchronized void loadCoins(int coin, int count) { coinBox.merge(coin, count, Integer::sum); }
    synchronized void setOutOfService(boolean off) { state = off ? State.OUT_OF_SERVICE : State.IDLE; }

    /**
     * Fewest coins for `amount` from a box with limited counts (greedy can fail here).
     */
    static Optional<Map<Integer, Integer>> makeChange(int amount, Map<Integer, Integer> box) {
        List<Map.Entry<Integer, Integer>> coins = new ArrayList<>(box.entrySet());
        int d = coins.size(), inf = Integer.MAX_VALUE / 2;
        int[][] best = new int[d + 1][amount + 1], take = new int[d + 1][amount + 1];
        for (int[] row : best) Arrays.fill(row, inf);
        best[0][0] = 0;
        for (int i = 0; i < d; i++) {
            int coin = coins.get(i).getKey(), count = coins.get(i).getValue();
            for (int a = 0; a <= amount; a++)
                for (int k = 0; k <= Math.min(count, a / coin); k++)
                    if (best[i][a - k * coin] + k < best[i + 1][a]) {
                        best[i + 1][a] = best[i][a - k * coin] + k;
                        // how many of coin i we used
                        take[i + 1][a] = k;
                    }
        }
        if (best[d][amount] >= inf) return Optional.empty();
        Map<Integer, Integer> change = new TreeMap<>(Comparator.reverseOrder());
        for (int i = d, a = amount; i > 0; i--) {
            int k = take[i][a];
            if (k > 0) change.put(coins.get(i - 1).getKey(), k);
            a -= k * coins.get(i - 1).getKey();
        }
        return Optional.of(change);
    }
}
