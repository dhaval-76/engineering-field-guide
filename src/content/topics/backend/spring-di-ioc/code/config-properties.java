// application.yml
//   partner:
//     base-url: https://api.partner.example
//     connect-timeout: 2s
//     retries: 3

@ConfigurationProperties(prefix = "partner")
@Validated
record PartnerProperties(
    @NotNull URI baseUrl,
    @NotNull Duration connectTimeout,     // "2s", "500ms" bind to Duration
    @Min(0) @Max(5) int retries) {}

@SpringBootApplication
@ConfigurationPropertiesScan              // registers all @ConfigurationProperties records
class ShopApplication { }

// Typed, validated at startup (bad config fails fast), documented in one place — prefer over scattered @Value.
// Precedence (highest first, simplified): command-line args → environment variables (PARTNER_RETRIES)
// → profile-specific files (application-prod.yml) → application.yml → defaults.
