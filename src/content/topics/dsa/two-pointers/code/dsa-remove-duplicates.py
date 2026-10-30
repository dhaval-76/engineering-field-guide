def remove_duplicates(sorted_nums: list[int]) -> int:
    if not sorted_nums:
        return 0
    write = 1
    for read in range(1, len(sorted_nums)):
        if sorted_nums[read] != sorted_nums[write - 1]:
            sorted_nums[write] = sorted_nums[read]
            write += 1
    return write
