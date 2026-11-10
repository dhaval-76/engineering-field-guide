String longestCommonPrefix(String[] words) {
    String prefix = words[0];
    for (String w : words) {
        int i = 0;
        while (i < prefix.length() && i < w.length() && prefix.charAt(i) == w.charAt(i)) i++;
        prefix = prefix.substring(0, i); // shrink to what this word shares
        if (prefix.isEmpty()) break;
    }
    return prefix;
}
