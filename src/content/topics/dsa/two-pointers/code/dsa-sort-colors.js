// Dutch national flag: sort an array of 0s, 1s and 2s in one pass, in place. Invariant:
// [0, low) are 0s · [low, mid) are 1s · [mid, high] unknown · (high, end] are 2s
function sortColors(nums) {
  let low = 0;
  let mid = 0;
  let high = nums.length - 1;
  while (mid <= high) {
    if (nums[mid] === 0) {
      [nums[low], nums[mid]] = [nums[mid], nums[low]];
      low++;
      mid++;
    } else if (nums[mid] === 1) {
      mid++;
    } else {
      // don't advance mid: the swapped-in value is unchecked
      [nums[mid], nums[high]] = [nums[high], nums[mid]];
      high--;
    }
  }
}
// [2,0,2,1,1,0] → [0,0,1,1,2,2]. O(n), one pass, O(1) space.
