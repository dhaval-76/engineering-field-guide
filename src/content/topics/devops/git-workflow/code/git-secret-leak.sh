# 1. Contain: revoke/rotate the credential NOW at its provider (AWS, Stripe, GitHub…).
#    Check its access logs for use since the push.

# 2. Remove it from the repository history (coordinate: everyone must re-clone afterwards)
pip install git-filter-repo
git clone --mirror git@github.com:org/shop.git && cd shop.git
printf 'sk_live_51Hxxxxxxxx==>REMOVED\n' > ../replacements.txt
git filter-repo --replace-text ../replacements.txt     # rewrite every commit containing it
git push --force --mirror
# (Hosting providers may keep cached views and PR refs — ask support to purge them.)

# 3. Prevent recurrence
#    - secret scanning / push protection on the host; gitleaks in pre-commit and CI
#    - secrets from a manager (Vault, AWS Secrets Manager) or CI secrets, never in the repo
#    - .env and key files in .gitignore
