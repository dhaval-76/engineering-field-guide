/**
 * Cohen's kappa between an LLM judge and human labels. 1 is perfect agreement; 0 is what chance
 * alone would give. Raw agreement looks good on imbalanced data; kappa does not.
 */
double kappa(boolean[] judge, boolean[] human) {
    int n = human.length, agree = 0, judgeYes = 0, humanYes = 0;
    for (int i = 0; i < n; i++) {
        if (judge[i] == human[i]) agree++;
        if (judge[i]) judgeYes++;
        if (human[i]) humanYes++;
    }
    double observed = (double) agree / n;
    double pj = (double) judgeYes / n, ph = (double) humanYes / n;
    double chance = pj * ph + (1 - pj) * (1 - ph);
    return chance == 1 ? 1 : (observed - chance) / (1 - chance);
}
