// Constructor injection → a plain, fast unit test: no Spring context at all
@ExtendWith(MockitoExtension.class)
class CheckoutServiceTest {
    @Mock PaymentGateway gateway;
    @Mock OrderRepository orders;

    @Test
    void chargesAndSavesTheOrder() {
        var service = new CheckoutService(gateway, orders);
        when(gateway.charge(any())).thenReturn(new Charge("ch_1", SUCCEEDED));

        Order placed = service.place(cartWith(sku("A1", 2)));

        assertThat(placed.status()).isEqualTo(PAID);
        verify(orders).save(placed);
    }
}
// Runs in milliseconds. Reserve @SpringBootTest / slices (@WebMvcTest, @DataJpaTest) for wiring and integration.
