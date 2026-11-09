int romanToInt(String s) {
    Map<Character, Integer> value = Map.of(
            'I', 1, 'V', 5, 'X', 10, 'L', 50, 'C', 100, 'D', 500, 'M', 1000);
    int total = 0;
    for (int i = 0; i < s.length(); i++) {
        int v = value.get(s.charAt(i));
        boolean beforeBigger = i + 1 < s.length() && v < value.get(s.charAt(i + 1));
        total += beforeBigger ? -v : v;
    }
    return total;
}
