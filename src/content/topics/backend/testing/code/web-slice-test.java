@WebMvcTest(OrderController.class)        // controller + MVC + advice + security; no services, no DB
class OrderControllerTest {

    @Autowired MockMvc mvc;
    @MockitoBean OrderService orders;      // replaces the bean in the test context

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void create_returns201WithLocation() throws Exception {
        when(orders.placeOrder(any())).thenReturn(new OrderView("O-1", "PAID"));

        mvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"customerId": "C-1", "items": [{"sku": "BOOK", "qty": 2}]}"""))
           .andExpect(status().isCreated())
           .andExpect(header().string("Location", endsWith("/api/orders/O-1")))
           .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void create_invalidBody_returnsProblemDetailsWithFieldErrors() throws Exception {
        mvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"customerId": "", "items": []}"""))
           .andExpect(status().isBadRequest())
           .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
           .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("customerId", "items")));

        verifyNoInteractions(orders);      // validation stopped it before the service
    }

    @Test
    void create_withoutLogin_returns401() throws Exception {
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("{}"))
           .andExpect(status().isUnauthorized());
    }
}
