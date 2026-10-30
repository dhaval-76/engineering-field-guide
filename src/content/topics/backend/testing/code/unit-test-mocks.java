@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock OrderRepository orders;
    @Mock PaymentGateway payments;                  // external system: mock it
    @Mock ApplicationEventPublisher events;
    // A real fixed clock; @Spy only so that @InjectMocks passes it to the constructor.
    @Spy  Clock clock = Clock.fixed(Instant.parse("2026-03-01T10:00:00Z"), ZoneOffset.UTC);
    @InjectMocks OrderService service;              // real class, built with the mocks above

    @Captor ArgumentCaptor<Order> savedOrder;
    @Captor ArgumentCaptor<Object> event;           // Object: matches publishEvent(Object) for POJO events

    private final OrderRequest request = new OrderRequest("C-1", List.of(new Item("BOOK", 2)));

    @Test
    void placeOrder_chargesSavesAndPublishes() {
        when(payments.charge(eq("C-1"), any())).thenReturn(new Charge("ch_1", Charge.Status.SUCCEEDED));
        when(orders.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));   // return what was saved

        Order result = service.placeOrder(request);

        assertThat(result.status()).isEqualTo(OrderStatus.PAID);

        verify(orders).save(savedOrder.capture());   // inspect the object the service built
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
