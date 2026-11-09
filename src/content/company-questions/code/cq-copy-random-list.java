class RandomNode {
    int val;
    RandomNode next, random;

    RandomNode(int val) {
        this.val = val;
    }
}

RandomNode copyRandomList(RandomNode head) {
    Map<RandomNode, RandomNode> copy = new HashMap<>();
    for (RandomNode n = head; n != null; n = n.next) copy.put(n, new RandomNode(n.val));
    for (RandomNode n = head; n != null; n = n.next) {
        copy.get(n).next = copy.get(n.next);
        copy.get(n).random = copy.get(n.random);
    }
    return copy.get(head);
}
