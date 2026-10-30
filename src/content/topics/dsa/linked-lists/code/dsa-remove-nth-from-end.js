function removeNthFromEnd(head, n) {
  const dummy = new ListNode(0, head);
  let lead = dummy, trail = dummy;
  for (let i = 0; i <= n; i++) lead = lead.next;
  while (lead) { lead = lead.next; trail = trail.next; }
  trail.next = trail.next.next;
  return dummy.next;
}
