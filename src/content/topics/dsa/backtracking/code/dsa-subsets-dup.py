# All distinct subsets when the input has duplicates: sort, then at each depth skip a
# value equal to the previous sibling — choosing either copy first leads to the same
# subsets.
def subsets_with_dup(nums: list[int]) -> list[list[int]]:
    nums = sorted(nums)
    out: list[list[int]] = []
    path: list[int] = []

    def build(start: int) -> None:
        out.append(path[:])                  # every node of the tree is a subset
        for i in range(start, len(nums)):
            if i > start and nums[i] == nums[i - 1]:
                continue                     # duplicate sibling: skip
            path.append(nums[i])             # choose
            build(i + 1)                     # explore
            path.pop()                       # unchoose

    build(0)
    return out
# [1,2,2] → [], [1], [1,2], [1,2,2], [2], [2,2] — 6 subsets, not 8.
