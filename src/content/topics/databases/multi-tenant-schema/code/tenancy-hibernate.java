// ── Option 1: shared tables with a discriminator column (Hibernate 6+) ──────────────
@Entity
public class Project {
    @Id @GeneratedValue Long id;

    // Hibernate adds "tenant_id = ?" to every query on this entity
    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    Long tenantId; // and sets it on insert from the current tenant

    String name;
}

@Component
class TenantResolver
        implements CurrentTenantIdentifierResolver<Long>, HibernatePropertiesCustomizer {
    @Override
    public Long resolveCurrentTenantIdentifier() {
        // set from the JWT by a request filter
        return TenantContext.required();
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }

    @Override
    public void customize(Map<String, Object> props) {
        props.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, this);
    }
}

// Belt and braces: also set the RLS variable for the transaction (see the SQL tab).
@Aspect
@Component
class RlsTenantSetter {
    private final JdbcTemplate jdbc;

    RlsTenantSetter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Before(
            "@within(org.springframework.transaction.annotation.Transactional) ||"
                + " @annotation(org.springframework.transaction.annotation.Transactional)")
    void setTenant() {
        jdbc.queryForObject(
                "select set_config('app.tenant_id', ?, true)",
                String.class,
                String.valueOf(TenantContext.required()));
    }
}

// ── Option 2: schema per tenant — switch the schema on each borrowed connection ─────
class SchemaPerTenantConnectionProvider implements MultiTenantConnectionProvider<String> {
    private final DataSource dataSource;

    SchemaPerTenantConnectionProvider(DataSource ds) {
        this.dataSource = ds;
    }

    @Override
    public Connection getConnection(String tenant) throws SQLException {
        Connection c = dataSource.getConnection();
        // never concatenate unchecked input
        c.setSchema("tenant_" + requireSafeIdentifier(tenant));
        return c;
    }

    @Override
    public void releaseConnection(String tenant, Connection c) throws SQLException {
        // reset before returning to the pool
        c.setSchema("public");
        c.close();
    }

    @Override
    public Connection getAnyConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void releaseAnyConnection(Connection c) throws SQLException {
        c.close();
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    @Override
    public boolean isUnwrappableAs(Class<?> t) {
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> t) {
        throw new UnsupportedOperationException();
    }
}
