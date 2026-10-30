function mergeKLists(lists) {
  const heap = new Heap((a, b) => a.val < b.val);
  for (const head of lists) if (head) heap.push(head);
  const dummy = new ListNode(0);
  let tail = dummy;
  while (heap.size) {
    const node = heap.pop();
    tail.next = node;
    tail = node;
    if (node.next) heap.push(node.next);
  }
  return dummy.next;
}
