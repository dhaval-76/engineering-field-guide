from collections import defaultdict

def accounts_merge(accounts):
    uf = UnionFind(len(accounts))
    owner = {}
    for i, (_, *emails) in enumerate(accounts):
        for e in emails:
            if e in owner:
                uf.union(i, owner[e])
            else:
                owner[e] = i
    groups = defaultdict(set)
    for e, i in owner.items():
        groups[uf.find(i)].add(e)
    return [[accounts[root][0], *sorted(emails)] for root, emails in groups.items()]
