import heapq

def merge_k_lists(lists):
    heap = [(head.val, i, head) for i, head in enumerate(lists) if head]   # i breaks ties
    heapq.heapify(heap)
    dummy = tail = ListNode()
    while heap:
        _, i, node = heapq.heappop(heap)
        tail.next = tail = node
        if node.next:
            heapq.heappush(heap, (node.next.val, i, node.next))
    return dummy.next
