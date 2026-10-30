#!/usr/bin/env bash
# Full local verification; exits non-zero on the first failure.
set -euo pipefail
npm run --silent check:content
npm run --silent diagrams -- --check
npx astro build > .cache/build.log 2>&1 || { grep -v '^\s*at ' .cache/build.log | tail -25; exit 1; }
echo "build ok"
