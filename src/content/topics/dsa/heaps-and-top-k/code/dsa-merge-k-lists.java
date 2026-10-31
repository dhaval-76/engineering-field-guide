ListNode mergeKLists(ListNode[] lists) {
    PriorityQueue<ListNode> heap = new PriorityQueue<>(Comparator.comparingInt(n -> n.val));
    for (ListNode head : lists) if (head != null) heap.offer(head);
    ListNode dummy = new ListNode(0), tail = dummy;
    while (!heap.isEmpty()) {
        ListNode smallest = heap.poll();
        tail.next = smallest;
        tail = smallest;
        // its successor joins the race
        if (smallest.next != null) heap.offer(smallest.next);
    }
    return dummy.next;
}
// N total nodes, k lists: O(N log k) time, O(k) heap space
