def kappa(judge, human):
    """Cohen's kappa between an LLM judge and human labels.

    1 is perfect agreement; 0 is what chance alone would give. Raw agreement looks good on
    imbalanced data; kappa does not.
    """
    n = len(human)
    observed = sum(j == h for j, h in zip(judge, human)) / n
    pj = sum(judge) / n
    ph = sum(human) / n
    chance = pj * ph + (1 - pj) * (1 - ph)
    return 1.0 if chance == 1 else (observed - chance) / (1 - chance)
