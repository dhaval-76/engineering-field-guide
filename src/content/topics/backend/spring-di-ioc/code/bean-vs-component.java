// @Component and its stereotypes: Spring finds the class by scanning and calls its
// constructor
@Service       class OrderService { … }          // business logic (no extra behaviour)
// data access; also translates SQL exceptions to DataAccessException
@Repository    class JpaOrderStore { … }
@RestController class OrderController { … }      // web; @Controller + @ResponseBody

// @Bean: YOU write the construction code in a @Configuration class — for classes you don't own,
// or when construction needs logic, or several differently configured instances of one type
@Configuration
class ClientsConfig {

    @Bean
    HttpClient partnerClient(PartnerProperties props) {
        return HttpClient.newBuilder()
            .connectTimeout(props.connectTimeout())
            .build();
    }

    // a second ObjectMapper, chosen with @Qualifier("reportsMapper")
    @Bean("reportsMapper")
    ObjectMapper reportsMapper() {
        return JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();
    }

    @Bean
    @ConditionalOnProperty(name = "features.fraud-check", havingValue = "true")
    FraudChecker fraudChecker() { return new RulesFraudChecker(); }
}

// Several beans of one type: @Primary marks the default; @Qualifier picks a specific one;
// List<PaymentGateway> injects all of them (useful for strategy/plugin patterns).
