class ShoppingCartTest {

    private ShoppingCart cart;                  // a fresh instance per test method

    @BeforeEach
    void setUp() {
        cart = new ShoppingCart(new PriceCatalog(Map.of("BOOK", money("12.50"), "PEN", money("1.20"))));
    }

    @Test
    void total_sumsLinePrices() {
        cart.add("BOOK", 2);                    // arrange / act
        cart.add("PEN", 3);

        assertThat(cart.total()).isEqualTo(money("28.60"));   // assert
    }

    @Test
    void add_rejectsUnknownSku() {
        var ex = assertThrows(UnknownSkuException.class, () -> cart.add("LAPTOP", 1));
        assertThat(ex.sku()).isEqualTo("LAPTOP");
        assertThat(cart.isEmpty()).isTrue();    // and nothing was half-added
    }

    @ParameterizedTest(name = "{0} items → {1}% off")
    @CsvSource({ "1, 0", "4, 0", "5, 10", "20, 10", "21, 15" })
    void bulkDiscount_byQuantity(int qty, int expectedPercent) {
        assertThat(Discounts.bulkPercent(qty)).isEqualTo(expectedPercent);
    }

    @Nested
    @DisplayName("when the cart is empty")
    class WhenEmpty {
        @Test void totalIsZero()        { assertThat(cart.total()).isEqualTo(Money.ZERO); }
        @Test void checkoutIsRejected() { assertThrows(EmptyCartException.class, cart::checkout); }
    }
}
