# While working: record fixes as "fixup" commits aimed at the commit they amend
git commit --fixup=a1b2c3d            # creates "fixup! Add shopping cart"

# Before review: fold them in automatically
git rebase -i --autosquash origin/main

# Or edit the plan by hand. The editor shows your commits, oldest first:
#   pick   a1b2c3d Add shopping cart
#   fixup  d4e5f6a WIP                 ← melt into the commit above, drop its message
#   fixup  9f8e7d6 fix bug
#   pick   1c2b3a4 Add cart tests
#   fixup  5e6f7a8 fix lint
#   reword 0a9b8c7 discount stuff      ← keep, but edit the message
#   drop   7d6c5b4 debug logging       ← remove entirely
#
# Result: three clean commits:
#   Add shopping cart
#   Add cart tests
#   Apply percentage discounts at checkout

git push --force-with-lease           # only because this branch is yours
