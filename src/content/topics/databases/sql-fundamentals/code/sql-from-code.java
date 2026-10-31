// Plain JDBC; JdbcTemplate and JPA use the same placeholders underneath.
public long placeOrder(DataSource ds, long customerId, long productId, int qty) throws SQLException {
    try (Connection con = ds.getConnection()) {
        con.setAutoCommit(false);                                  // start a transaction
        try {
            int reserved;
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE products SET stock = stock - ? WHERE id = ? AND stock >= ?")) {
                // bound, never concatenated
                ps.setInt(1, qty); ps.setLong(2, productId); ps.setInt(3, qty);
                reserved = ps.executeUpdate();
            }
            if (reserved == 0) throw new OutOfStockException(productId);

            long orderId;
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO orders (customer_id, total) " +
                    "SELECT ?, price * ? FROM products WHERE id = ? RETURNING id")) {
                ps.setLong(1, customerId); ps.setInt(2, qty); ps.setLong(3, productId);
                try (ResultSet rs = ps.executeQuery()) { rs.next(); orderId = rs.getLong(1); }
            }
            // both changes, or neither
            con.commit();
            return orderId;
        } catch (SQLException | RuntimeException e) {
            con.rollback();
            throw e;
        }
    }
}
