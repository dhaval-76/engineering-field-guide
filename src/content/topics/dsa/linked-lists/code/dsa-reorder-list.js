// L0 → Ln → L1 → Ln−1 → L2 → … in place: find the middle, reverse the second half, weave.
function reorderList(head) {
  if (!head || !head.next) return;
  let slow = head; // 1. middle (first middle for even lengths)
  let fast = head;
  while (fast.next && fast.next.next) {
    slow = slow.next;
    fast = fast.next.next;
  }
  let second = reverse(slow.next); // 2. reverse the second half
  slow.next = null; //    and cut the list in two
  let first = head; // 3. weave the halves together
  while (second) {
    const nextFirst = first.next;
    const nextSecond = second.next;
    first.next = second;
    second.next = nextFirst;
    first = nextFirst;
    second = nextSecond;
  }
}

function reverse(head) {
  let prev = null;
  while (head) {
    const next = head.next;
    head.next = prev;
    prev = head;
    head = next;
  }
  return prev;
}
// [1,2,3,4,5] → [1,5,2,4,3]. O(n) time, O(1) space.
