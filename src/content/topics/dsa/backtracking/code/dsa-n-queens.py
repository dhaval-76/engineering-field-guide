def solve_n_queens(n):
    cols, diag, anti, queens, out = set(), set(), set(), [], []
    def place(row):
        if row == n:
            out.append(["." * c + "Q" + "." * (n - c - 1) for c in queens])
            return
        for c in range(n):
            if c in cols or row - c in diag or row + c in anti:
                continue
            cols.add(c); diag.add(row - c); anti.add(row + c); queens.append(c)
            place(row + 1)
            cols.remove(c); diag.remove(row - c); anti.remove(row + c); queens.pop()
    place(0)
    return out
