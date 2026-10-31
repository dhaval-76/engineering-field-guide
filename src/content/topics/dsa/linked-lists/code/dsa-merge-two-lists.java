ListNode mergeTwoLists(ListNode a, ListNode b) {
    // dummy: no special case for the first node
    ListNode dummy = new ListNode(0), tail = dummy;
    while (a != null && b != null) {
        if (a.val <= b.val) { tail.next = a; a = a.next; }
        else { tail.next = b; b = b.next; }
        tail = tail.next;
    }
    tail.next = a != null ? a : b;                    // attach the remainder
    return dummy.next;
}
