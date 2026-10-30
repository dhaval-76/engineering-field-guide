# config.py — a Python module is imported once and cached in sys.modules,
# so module-level objects are effectively singletons.
import os
from functools import cache

DB_URL = os.environ.get("DB_URL")


@cache  # computed on first call, then reused: a lazy singleton
def get_pool():
    return create_pool(DB_URL)
