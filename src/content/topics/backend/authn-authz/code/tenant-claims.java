// Access token claims:
// {"sub":"u_1","org_id":"org_42","roles":["ADMIN"],"aud":"projects-api", …}
@Bean
JwtAuthenticationConverter jwtAuthConverter() {
    JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
    roles.setAuthoritiesClaimName("roles");
    roles.setAuthorityPrefix("ROLE_");
    JwtAuthenticationConverter c = new JwtAuthenticationConverter();
    c.setJwtGrantedAuthoritiesConverter(roles);
    return c;
}

@Component("tenant")
class TenantAccess {
    public String orgId(Authentication auth) {
        return ((JwtAuthenticationToken) auth).getToken().getClaimAsString("org_id");
    }
}

@Service
class ProjectService {
    // Every query is scoped to the caller's org; the org id never comes from the request
    // body or URL.
    public List<Project> list(Authentication auth) {
        return projects.findByOrgId(tenant.orgId(auth));
    }

    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public Project create(Authentication auth, NewProject req) {
        return projects.save(Project.create(tenant.orgId(auth), req));
    }
}
// Defence in depth: PostgreSQL row-level security (POLICY … USING (org_id =
// current_setting('app.org_id'))) so a forgotten WHERE clause still can't read another
// org's rows.
