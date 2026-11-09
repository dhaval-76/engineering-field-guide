import hashlib


class TrustedDocuments:
    """Only operations registered at build time can run. Clients send a hash, not a query."""

    def __init__(self):
        self._by_hash = {}

    def register(self, document):
        """Called from the client's build, for each operation it contains."""
        digest = hashlib.sha256(document.encode()).hexdigest()
        self._by_hash[digest] = document
        return digest

    def lookup(self, digest):
        """None means reject: this query did not come from a build of our own app."""
        return self._by_hash.get(digest)
