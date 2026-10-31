@SpringBootTest                           // whole app, mock servlet environment
// shared containers: started once, reused by every class
@Import(TestcontainersConfig.class)
@AutoConfigureMockMvc
class PlaceOrderFlowTest {

    @Autowired MockMvc mvc;
    @Autowired OrderRepository orders;
    @MockitoBean PaymentGateway payments; // the only thing we fake: an external, paid API

    @Test
    @WithMockUser(username = "C-1", roles = "CUSTOMER")
    void placingAnOrder_persistsItAndReturns201() throws Exception {
        when(payments.charge(any(), any())).thenReturn(new Charge("ch_1", Charge.Status.SUCCEEDED));

        mvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", "k-1")
                .content("""
                    {"customerId": "C-1", "items": [{"sku": "BOOK", "qty": 1}]}"""))
           .andExpect(status().isCreated());

        assertThat(orders.findByCustomerId("C-1"))
            .singleElement().extracting(Order::getStatus).isEqualTo(OrderStatus.PAID);
    }
}

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfig {
    @Bean @ServiceConnection
    PostgreSQLContainer postgres() { return new PostgreSQLContainer("postgres:17-alpine"); }

    @Bean @ServiceConnection(name = "redis")
    GenericContainer<?> redis() { return new GenericContainer<>("redis:7-alpine").withExposedPorts(6379); }
}
