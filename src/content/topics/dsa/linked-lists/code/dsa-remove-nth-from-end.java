ListNode removeNthFromEnd(ListNode head, int n) {
    ListNode dummy = new ListNode(0, head);           // handles removing the head itself
    ListNode lead = dummy, trail = dummy;
    // gap of n + 1 between lead and trail
    for (int i = 0; i <= n; i++) lead = lead.next;
    while (lead != null) { lead = lead.next; trail = trail.next; }
    trail.next = trail.next.next;                     // trail is just before the target
    return dummy.next;
}
// [1,2,3,4,5], n = 2 → [1,2,3,5]. One pass, O(1) space.
