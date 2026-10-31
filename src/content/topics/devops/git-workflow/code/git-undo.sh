git restore src/App.java              # throw away unstaged edits to a file
git restore --staged src/App.java     # unstage, keep the edits

# change the last commit's message or content (before pushing)
git commit --amend

git reset --soft HEAD~1               # undo last commit; changes stay staged
# …changes stay in the working tree, unstaged (default)
git reset --mixed HEAD~1
# …changes discarded (uncommitted work is unrecoverable)
git reset --hard HEAD~1

git revert 3f2e1d0                    # shared branch: add a commit that undoes 3f2e1d0
git revert -m 1 7a6b5c4               # revert a merge commit, keeping parent 1's side

git reflog                            # every position HEAD has been at, ~90 days
# e.g. you deleted a branch or reset too far:
#   5c4b3a2 HEAD@{3}: commit: Add invoice export
git switch -c rescue 5c4b3a2          # bring it back as a branch

# copy one commit onto the current branch (e.g. a hotfix)
git cherry-pick 3f2e1d0
