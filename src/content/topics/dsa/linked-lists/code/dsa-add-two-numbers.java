// Digits stored in reverse order (ones first): 342 + 465 = [2,4,3] + [5,6,4] → [7,0,8].
ListNode addTwoNumbers(ListNode a, ListNode b) {
    ListNode dummy = new ListNode(0), tail = dummy;
    int carry = 0;
    while (a != null || b != null || carry != 0) {     // keep going for a final carry
        int sum = carry + (a != null ? a.val : 0) + (b != null ? b.val : 0);
        carry = sum / 10;
        tail.next = new ListNode(sum % 10);
        tail = tail.next;
        if (a != null) a = a.next;
        if (b != null) b = b.next;
    }
    return dummy.next;
}
// [9,9,9] + [1] → [0,0,0,1]. O(max(m, n)). Numbers of any length — no overflow.
