/** UNSAFE: the email becomes part of the SQL text, so it can change the query itself. */
String findUserUnsafe(Connection db, String email) throws SQLException {
    try (Statement st = db.createStatement();
         ResultSet rs = st.executeQuery("SELECT name FROM users WHERE email = '" + email + "'")) {
        return rs.next() ? rs.getString(1) : null;
    }
}

/** Safe: the SQL is fixed and the email is sent separately, as data only. */
String findUser(Connection db, String email) throws SQLException {
    try (PreparedStatement st = db.prepareStatement("SELECT name FROM users WHERE email = ?")) {
        st.setString(1, email);
        try (ResultSet rs = st.executeQuery()) {
            return rs.next() ? rs.getString(1) : null;
        }
    }
}

/** Column names can't be parameters, so map user choices to fixed, known names. */
static final Map<String, String> SORTABLE = Map.of("date", "created_at", "total", "amount");

List<Long> orderIds(Connection db, long userId, String sort, boolean descending)
        throws SQLException {
    String column = SORTABLE.get(sort);
    if (column == null) throw new IllegalArgumentException("cannot sort by " + sort);
    String sql = "SELECT id FROM orders WHERE user_id = ? ORDER BY " + column
            + (descending ? " DESC" : " ASC");
    try (PreparedStatement st = db.prepareStatement(sql)) {
        st.setLong(1, userId);
        List<Long> ids = new ArrayList<>();
        try (ResultSet rs = st.executeQuery()) {
            while (rs.next()) ids.add(rs.getLong(1));
        }
        return ids;
    }
}
