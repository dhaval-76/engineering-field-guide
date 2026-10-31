// L0 → Ln → L1 → Ln−1 → L2 → … in place: find the middle, reverse the second half, weave.
void reorderList(ListNode head) {
    if (head == null || head.next == null) return;
    // 1. middle (first middle for even)
    ListNode slow = head, fast = head;
    while (fast.next != null && fast.next.next != null) {
        slow = slow.next;
        fast = fast.next.next;
    }
    ListNode second = reverse(slow.next);              // 2. reverse the second half
    slow.next = null;                                  //    and cut the list in two
    ListNode first = head;                             // 3. weave the halves together
    while (second != null) {
        ListNode nextFirst = first.next, nextSecond = second.next;
        first.next = second;
        second.next = nextFirst;
        first = nextFirst;
        second = nextSecond;
    }
}

ListNode reverse(ListNode head) {
    ListNode prev = null;
    while (head != null) {
        ListNode next = head.next;
        head.next = prev;
        prev = head;
        head = next;
    }
    return prev;
}
// [1,2,3,4,5] → [1,5,2,4,3]. O(n) time, O(1) space.
