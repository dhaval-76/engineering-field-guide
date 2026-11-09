/**
 * Cohen's kappa between an LLM judge and human labels. 1 is perfect agreement; 0 is what chance
 * alone would give. Raw agreement looks good on imbalanced data; kappa does not.
 */
function kappa(judge, human) {
  const n = human.length;
  let agree = 0;
  let judgeYes = 0;
  let humanYes = 0;
  for (let i = 0; i < n; i++) {
    if (judge[i] === human[i]) agree++;
    if (judge[i]) judgeYes++;
    if (human[i]) humanYes++;
  }
  const observed = agree / n;
  const pj = judgeYes / n;
  const ph = humanYes / n;
  const chance = pj * ph + (1 - pj) * (1 - ph);
  return chance === 1 ? 1 : (observed - chance) / (1 - chance);
}
