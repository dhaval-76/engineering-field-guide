/** accounts[i] = [name, email1, email2, ...]. Merge accounts sharing any email. */
List<List<String>> accountsMerge(List<List<String>> accounts) {
    Map<String, Integer> ownerOf = new HashMap<>();        // email → first account index that had it
    UnionFind uf = new UnionFind(accounts.size());
    for (int i = 0; i < accounts.size(); i++)
        for (String email : accounts.get(i).subList(1, accounts.get(i).size())) {
            Integer j = ownerOf.putIfAbsent(email, i);
            if (j != null) uf.union(i, j);                  // shared email → same person
        }

    Map<Integer, TreeSet<String>> emailsByRoot = new HashMap<>();
    ownerOf.forEach((email, i) -> emailsByRoot.computeIfAbsent(uf.find(i), k -> new TreeSet<>()).add(email));

    List<List<String>> out = new ArrayList<>();
    emailsByRoot.forEach((root, emails) -> {
        List<String> account = new ArrayList<>();
        account.add(accounts.get(root).get(0));            // the name
        account.addAll(emails);                             // sorted
        out.add(account);
    });
    return out;
}
