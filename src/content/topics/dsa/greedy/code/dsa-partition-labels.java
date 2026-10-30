/** Split s into as many parts as possible so that each letter appears in at most one part. */
List<Integer> partitionLabels(String s) {
    int[] last = new int[26];
    for (int i = 0; i < s.length(); i++) last[s.charAt(i) - 'a'] = i;
    List<Integer> sizes = new ArrayList<>();
    int start = 0, end = 0;
    for (int i = 0; i < s.length(); i++) {
        end = Math.max(end, last[s.charAt(i) - 'a']);   // this part must reach every letter's last occurrence
        if (i == end) {                                 // nothing inside extends further: close the part
            sizes.add(end - start + 1);
            start = i + 1;
        }
    }
    return sizes;
}
// "ababcbacadefegdehijhklij" → [9,7,8]
