# L0 → Ln → L1 → Ln−1 → L2 → … in place: find the middle, reverse the second half, weave.
def reorder_list(head: "ListNode | None") -> None:
    if head is None or head.next is None:
        return
    slow = fast = head                  # 1. middle (first middle for even lengths)
    while fast.next and fast.next.next:
        slow, fast = slow.next, fast.next.next
    second = reverse(slow.next)         # 2. reverse the second half
    slow.next = None                    #    and cut the list in two
    first = head                        # 3. weave the halves together
    while second:
        next_first, next_second = first.next, second.next
        first.next, second.next = second, next_first
        first, second = next_first, next_second


def reverse(head: "ListNode | None") -> "ListNode | None":
    prev = None
    while head:
        head.next, prev, head = prev, head, head.next
    return prev
# [1,2,3,4,5] → [1,5,2,4,3]. O(n) time, O(1) space.
