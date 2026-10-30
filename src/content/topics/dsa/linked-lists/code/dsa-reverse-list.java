ListNode reverse(ListNode head) {
    ListNode prev = null, cur = head;
    while (cur != null) {
        ListNode next = cur.next;   // 1. save
        cur.next = prev;            // 2. re-point
        prev = cur;                 // 3. advance
        cur = next;
    }
    return prev;                    // new head
}

ListNode reverseRecursive(ListNode head) {   // O(n) stack space
    if (head == null || head.next == null) return head;
    ListNode newHead = reverseRecursive(head.next);
    head.next.next = head;          // the node after head now points back to head
    head.next = null;
    return newHead;
}
