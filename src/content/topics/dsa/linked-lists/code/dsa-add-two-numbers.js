// Digits stored in reverse order (ones first): 342 + 465 = [2,4,3] + [5,6,4] → [7,0,8].
function addTwoNumbers(a, b) {
  const dummy = new ListNode(0);
  let tail = dummy;
  let carry = 0;
  while (a || b || carry) {
    // keep going for a final carry
    const sum = carry + (a ? a.val : 0) + (b ? b.val : 0);
    carry = Math.floor(sum / 10);
    tail.next = new ListNode(sum % 10);
    tail = tail.next;
    if (a) a = a.next;
    if (b) b = b.next;
  }
  return dummy.next;
}
// [9,9,9] + [1] → [0,0,0,1]. O(max(m, n)). Numbers of any length — no overflow.
