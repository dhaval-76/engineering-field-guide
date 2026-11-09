int maxProfit(int[] prices) {
    int cheapest = Integer.MAX_VALUE, best = 0;
    for (int price : prices) {
        cheapest = Math.min(cheapest, price);
        best = Math.max(best, price - cheapest);
    }
    return best;
}
