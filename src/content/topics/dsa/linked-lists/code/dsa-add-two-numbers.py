# Digits stored in reverse order (ones first): 342 + 465 = [2,4,3] + [5,6,4] → [7,0,8].
def add_two_numbers(a: "ListNode | None", b: "ListNode | None") -> "ListNode | None":
    dummy = tail = ListNode(0)
    carry = 0
    while a or b or carry:              # keep going for a final carry
        total = carry + (a.val if a else 0) + (b.val if b else 0)
        carry, digit = divmod(total, 10)
        tail.next = ListNode(digit)
        tail = tail.next
        a = a.next if a else None
        b = b.next if b else None
    return dummy.next
# [9,9,9] + [1] → [0,0,0,1]. O(max(m, n)). Numbers of any length — no overflow.
