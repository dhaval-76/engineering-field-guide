# Start work from an up-to-date main
git switch main && git pull --ff-only          # fast-forward only: never creates a surprise merge
git switch -c feat/order-tracking              # new branch pointer at the current commit

# Commit in small, meaningful steps
git add -p                                     # stage hunks interactively; review what goes in
git commit -m "Add order status endpoint"

# Bring in the latest main while you work (your branch, not shared → rebase is fine)
git fetch origin
git rebase origin/main                         # replays your commits on top of origin/main
# conflicts? edit files → git add <file> → git rebase --continue   (or --abort to go back)

# Publish; after a rebase, the remote branch has the old commits → overwrite safely
git push -u origin feat/order-tracking
git push --force-with-lease                    # refuses if someone else pushed in the meantime

# Look at the graph
git log --oneline --graph --decorate --all -20

# Park unfinished work to switch context
git stash push -m "half-done refactor"
git stash pop
