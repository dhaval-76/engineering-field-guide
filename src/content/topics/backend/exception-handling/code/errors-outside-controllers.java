// 1. Filters run before the DispatcherServlet: @RestControllerAdvice never sees their exceptions.
//    Spring Security has its own hooks for 401/403 — return problem+json from them too.
@Bean
SecurityFilterChain api(HttpSecurity http) throws Exception {
    return http
        .exceptionHandling(e -> e
            .authenticationEntryPoint((req, res, ex) -> writeProblem(res, 401, "unauthenticated"))
            .accessDeniedHandler((req, res, ex) -> writeProblem(res, 403, "forbidden")))
        // …
        .build();
}

// 2. Your own filter: catch and write the response yourself (or let it reach /error)
@Override
protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
        throws IOException, ServletException {
    try {
        chain.doFilter(req, res);
    } catch (TenantMissingException e) {
        writeProblem(res, 400, "tenant_missing");
    }
}

// 3. @Async void methods: exceptions go to this handler, never to the HTTP caller
@Configuration @EnableAsync
class AsyncConfig implements AsyncConfigurer {
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> log.error("Async failure in {}", method.getName(), ex);
    }
}
