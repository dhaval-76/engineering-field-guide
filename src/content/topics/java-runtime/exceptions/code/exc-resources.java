// try-with-resources: close() is called automatically, in reverse order, even on exceptions.
static List<String> readLines(Path file) throws IOException {
    try (BufferedReader reader = Files.newBufferedReader(file)) {
        return reader.lines().toList();
    }
}

// Several resources; if the body AND close() both throw, close()'s exception is attached
// to the body's exception as "suppressed" — nothing is lost.
try (Connection conn = dataSource.getConnection();
     PreparedStatement ps = conn.prepareStatement("SELECT name FROM users WHERE id = ?")) {
    ps.setLong(1, id);
    try (ResultSet rs = ps.executeQuery()) { /* … */ }
} catch (SQLException e) {
    for (Throwable s : e.getSuppressed()) log.warn("also failed while closing", s);
    throw new DataAccessException("loading user " + id, e);     // keep the cause!
}
