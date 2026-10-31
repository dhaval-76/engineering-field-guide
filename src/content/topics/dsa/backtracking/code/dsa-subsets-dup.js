// All distinct subsets when the input has duplicates: sort, then at each depth skip a
// value equal to the previous sibling — choosing either copy first leads to the same
// subsets.
function subsetsWithDup(nums) {
  const sorted = [...nums].sort((a, b) => a - b);
  const out = [];
  const path = [];
  const build = (start) => {
    out.push([...path]); // every node of the tree is a subset
    for (let i = start; i < sorted.length; i++) {
      if (i > start && sorted[i] === sorted[i - 1]) continue; // duplicate sibling: skip
      path.push(sorted[i]); // choose
      build(i + 1); // explore
      path.pop(); // unchoose
    }
  };
  build(0);
  return out;
}
// [1,2,2] → [], [1], [1,2], [1,2,2], [2], [2,2] — 6 subsets, not 8.
