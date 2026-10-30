boolean hasCycle(ListNode head) {
    ListNode slow = head, fast = head;
    while (fast != null && fast.next != null) {
        slow = slow.next;
        fast = fast.next.next;
        if (slow == fast) return true;          // fast lapped slow inside a cycle
    }
    return false;                               // fast fell off the end
}

ListNode cycleStart(ListNode head) {
    ListNode slow = head, fast = head;
    while (fast != null && fast.next != null) {
        slow = slow.next;
        fast = fast.next.next;
        if (slow == fast) {                     // meeting point found
            ListNode p = head;
            while (p != slow) { p = p.next; slow = slow.next; }
            return p;                           // both reach the cycle entry together
        }
    }
    return null;
}

ListNode middle(ListNode head) {                // second middle for even lengths
    ListNode slow = head, fast = head;
    while (fast != null && fast.next != null) { slow = slow.next; fast = fast.next.next; }
    return slow;
}
