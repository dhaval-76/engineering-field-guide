// Immutability makes check-then-use safe: nobody can change the path after validation.
void openUserFile(String path) throws IOException {
    if (!path.startsWith("/srv/uploads/") || path.contains("..")) throw new SecurityException(path);
    // If String were mutable, another thread holding the same reference could rewrite it
    // to "/etc/passwd" between the check above and the open below.
    Files.readAllBytes(Path.of(path));
}

// The flip side: secrets in Strings can't be wiped. APIs that handle passwords use char[]
// so the caller can overwrite the array after use.
char[] password = console.readPassword();
try { authenticate(password); } finally { Arrays.fill(password, '\0'); }
