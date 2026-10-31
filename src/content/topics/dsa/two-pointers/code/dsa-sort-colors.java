// Dutch national flag: sort an array of 0s, 1s and 2s in one pass, in place.
// Invariant: [0, low) are 0s · [low, mid) are 1s · [mid, high] unknown · (high, end] are 2s
void sortColors(int[] nums) {
    int low = 0, mid = 0, high = nums.length - 1;
    while (mid <= high) {
        if (nums[mid] == 0) swap(nums, low++, mid++);
        else if (nums[mid] == 1) mid++;
        else swap(nums, mid, high--);   // don't advance mid: the swapped-in value is unchecked
    }
}

void swap(int[] a, int i, int j) { int t = a[i]; a[i] = a[j]; a[j] = t; }
// [2,0,2,1,1,0] → [0,0,1,1,2,2]. O(n), one pass, O(1) space.
