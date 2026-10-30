// Spring Boot reads META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
// from every starter jar; each listed class is applied only if its conditions match.
@AutoConfiguration
@ConditionalOnClass(PartnerClient.class)                      // the library is on the classpath
@EnableConfigurationProperties(PartnerProperties.class)
public class PartnerClientAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean                                 // back off if the app defined its own
    PartnerClient partnerClient(PartnerProperties props) {
        return new PartnerClient(props.baseUrl(), props.connectTimeout());
    }
}
// Why did (or didn't) something get configured? Run with --debug (or /actuator/conditions)
// to print the conditions evaluation report.
