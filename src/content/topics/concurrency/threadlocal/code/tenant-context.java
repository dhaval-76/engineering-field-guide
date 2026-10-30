public final class TenantContext {
    private static final ThreadLocal<String> TENANT = new ThreadLocal<>();
    public static void set(String tenantId) { TENANT.set(tenantId); }
    public static String get() { return TENANT.get(); }
    public static void clear() { TENANT.remove(); }
}

public class TenantFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String tenant = req.getHeader("X-Tenant-Id");
        if (tenant == null) { res.sendError(400, "missing tenant"); return; }
        TenantContext.set(tenant);
        MDC.put("tenant", tenant);                  // every log line gets [tenant] via %X{tenant}
        try {
            chain.doFilter(req, res);
        } finally {
            MDC.remove("tenant");
            TenantContext.clear();
        }
    }
}
// Services and repositories call TenantContext.get(); log pattern: "%d [%X{tenant}] %msg%n"
