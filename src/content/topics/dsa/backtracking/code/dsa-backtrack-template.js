function backtrack(path, choices, results) {
  if (isComplete(path)) {
    results.push([...path]);                       // copy
    return;
  }
  for (const c of choices(path)) {
    if (!valid(path, c)) continue;                 // prune
    path.push(c);                                  // choose
    backtrack(path, choices, results);             // explore
    path.pop();                                    // unchoose
  }
}
