String reorganizeString(String s) {
    int[] count = new int[26];
    int most = 0;
    for (char c : s.toCharArray()) {
        if (++count[c - 'a'] > count[most]) most = c - 'a';
    }
    if (count[most] > (s.length() + 1) / 2) return "";
    char[] out = new char[s.length()];
    int i = 0;
    // The most frequent letter takes the even slots first, so it can't touch itself.
    for (; count[most] > 0; count[most]--, i += 2) out[i] = (char) ('a' + most);
    for (int letter = 0; letter < 26; letter++) {
        for (; count[letter] > 0; count[letter]--, i += 2) {
            if (i >= out.length) i = 1; // even slots are full; move to the odd ones
            out[i] = (char) ('a' + letter);
        }
    }
    return new String(out);
}
