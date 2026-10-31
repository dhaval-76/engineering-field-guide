// Plain JDBC: try-with-resources returns the connection to the pool even when an
// exception is thrown
try (Connection c = dataSource.getConnection();
     PreparedStatement ps = c.prepareStatement("SELECT id, total FROM orders WHERE customer_id = ?")) {
    ps.setString(1, customerId);
    try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) result.add(map(rs));
    }
}   // close() on a pooled connection = "give it back", not "disconnect"

// ✗ Leak: an exception between getConnection() and close() keeps the connection forever
Connection c = dataSource.getConnection();
List<Order> orders = query(c, customerId);   // throws → close() never runs
c.close();
