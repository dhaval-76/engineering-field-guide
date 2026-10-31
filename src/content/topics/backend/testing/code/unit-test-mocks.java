@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock OrderRepository orders;
    @Mock PaymentGateway payments;                  // external system: mock it
    @Mock ApplicationEventPublisher events;
    // A real fixed clock; @Spy only so that @InjectMocks passes it to the constructor.
    @Spy  Clock clock = Clock.fixed(Instant.parse("2026-03-01T10:00:00Z"), ZoneOffset.UTC);
    // real class, built with the mocks above
    @InjectMocks OrderService service;

    @Captor ArgumentCaptor<Order> savedOrder;
    // Object: matches publishEvent(Object) for POJO events
    @Captor ArgumentCaptor<Object> event;

    private final OrderRequest request = new OrderRequest("C-1", List.of(new Item("BOOK", 2)));

    @Test
    void placeOrder_chargesSavesAndPublishes() {
        when(payments.charge(eq("C-1"), any())).thenReturn(new Charge("ch_1", Charge.Status.SUCCEEDED));
        // return what was saved
        when(orders.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = service.placeOrder(request);

        assertThat(result.status()).isEqualTo(OrderStatus.PAID);

        // inspect the object the service built
        verify(orders).save(savedOrder.capture());
        assertThat(savedOrder.getValue().customerId()).isEqualTo("C-1");
        assertThat(savedOrder.getValue().placedAt()).isEqualTo(Instant.parse("2026-03-01T10:00:00Z"));

        verify(events).publishEvent(event.capture());
        assertThat(event.getValue()).isInstanceOfSatisfying(OrderPlaced.class,
                e -> assertThat(e.orderId()).isEqualTo(result.id()));
    }

    @Test
    void placeOrder_whenPaymentDeclined_savesNothingAndPublishesNothing() {
        when(payments.charge(any(), any())).thenThrow(new PaymentDeclinedException("card_declined"));

        assertThrows(PaymentDeclinedException.class, () -> service.placeOrder(request));

        verify(orders, never()).save(any());
        verifyNoInteractions(events);
    }
}
