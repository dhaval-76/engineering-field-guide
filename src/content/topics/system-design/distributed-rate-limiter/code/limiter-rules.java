@Component
// after authentication has identified the caller
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
class RateLimitFilter extends OncePerRequestFilter {
    private final RuleSet rules;
    private final SlidingWindowLimiter limiter;

    @Override
    protected void doFilterInternal(
            HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        // API key / user / tenant, plan; IP if anonymous
        Caller caller = Caller.from(req);
        Rule rule = rules.match(caller, req.getMethod() + " " + req.getRequestURI());
        var d = limiter.check(rule.keyFor(caller), rule.limit(), rule.window());

        res.setHeader("X-RateLimit-Limit", String.valueOf(rule.limit()));
        if (d.remaining() >= 0)
            res.setHeader("X-RateLimit-Remaining", String.valueOf(d.remaining()));
        if (!d.allowed()) {
            res.setHeader("Retry-After", String.valueOf(d.retryAfterSeconds()));
            res.setStatus(429);
            res.setContentType("application/problem+json");
            res.getWriter()
                    .write(
                            "{\"type\":\"about:blank\",\"title\":\"Too Many"
                                + " Requests\",\"status\":429}");
            return;
        }
        chain.doFilter(req, res);
    }
}
