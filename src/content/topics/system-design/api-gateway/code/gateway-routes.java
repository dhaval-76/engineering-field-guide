@Configuration
class GatewaySecurity {
    @Bean
    SecurityWebFilterChain security(ServerHttpSecurity http) {
        return http
            .authorizeExchange(ex -> ex
                .pathMatchers("/api/v1/products/**").permitAll()       // public catalogue
                .anyExchange().authenticated())
            // local signature + exp/iss/aud checks
            .oauth2ResourceServer(o -> o.jwt(Customizer.withDefaults()))
            // token-based API
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .build();
    }

    /** Rate-limit key: API key for partners, user id for signed-in users, IP otherwise. */
    @Bean
    KeyResolver apiKeyResolver() {
        return exchange -> {
            String apiKey = exchange.getRequest().getHeaders().getFirst("X-Api-Key");
            if (apiKey != null) return Mono.just("key:" + apiKey);
            return exchange.getPrincipal()
                .map(p -> "user:" + p.getName())
                .switchIfEmpty(Mono.fromSupplier(() ->
                    "ip:" + exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()));
        };
    }
}
