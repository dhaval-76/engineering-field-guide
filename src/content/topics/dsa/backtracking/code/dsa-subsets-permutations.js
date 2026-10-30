function subsets(nums) {
  const out = [], path = [];
  const go = (start) => {
    out.push([...path]);
    for (let i = start; i < nums.length; i++) {
      path.push(nums[i]);
      go(i + 1);
      path.pop();
    }
  };
  go(0);
  return out;
}

function permute(nums) {
  const out = [], path = [], used = new Array(nums.length).fill(false);
  const go = () => {
    if (path.length === nums.length) return void out.push([...path]);
    nums.forEach((x, i) => {
      if (used[i]) return;
      used[i] = true; path.push(x);
      go();
      used[i] = false; path.pop();
    });
  };
  go();
  return out;
}
