// Spring Boot 3: let Spring Security validate JWTs — don't hand-write a JWT filter.
// application.yml
// spring.security.oauth2.resourceserver.jwt.issuer-uri:
// https://auth.example.com/realms/shop
//   (Spring fetches the issuer's JWKS public keys and checks signature, iss and exp)

@Configuration
@EnableMethodSecurity                                   // enables @PreAuthorize
class SecurityConfig {
    @Bean
    SecurityFilterChain api(HttpSecurity http) throws Exception {
        return http
            // OK only because auth is a bearer header, not a cookie
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers(HttpMethod.GET, "/v1/orders/**").hasAuthority("SCOPE_orders:read")
                .requestMatchers("/v1/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            // missing/invalid token → 401
            .oauth2ResourceServer(o -> o.jwt(Customizer.withDefaults()))
            .build();
    }

    @Bean
    JwtDecoder jwtDecoder(@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer) {
        NimbusJwtDecoder decoder = JwtDecoders.fromIssuerLocation(issuer);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefaultWithIssuer(issuer),
            new JwtClaimValidator<List<String>>("aud", aud -> aud != null && aud.contains("orders-api"))));
        return decoder;
    }
}
