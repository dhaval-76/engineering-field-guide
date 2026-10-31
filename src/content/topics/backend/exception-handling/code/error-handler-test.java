@WebMvcTest(OrderController.class)
@Import(ApiExceptionHandler.class)
class OrderErrorsTest {
    @Autowired MockMvc mvc;
    // Spring Boot 3.4+ (@MockBean before)
    @MockitoBean OrderService orders;

    @Test
    void invalidBody_returns400WithFieldErrors() throws Exception {
        mvc.perform(post("/v1/orders").contentType(APPLICATION_JSON)
                .content("""
                    {"customerId": "", "items": []}"""))
           .andExpect(status().isBadRequest())
           .andExpect(content().contentType("application/problem+json"))
           .andExpect(jsonPath("$.code").value("validation_failed"))
           .andExpect(jsonPath("$.errors[?(@.field == 'customerId')]").exists())
           .andExpect(jsonPath("$.errors[?(@.field == 'items')]").exists());
    }

    @Test
    void unknownOrder_returns404() throws Exception {
        when(orders.find("ord_x")).thenThrow(new NotFoundException("order", "ord_x"));
        mvc.perform(get("/v1/orders/ord_x"))
           .andExpect(status().isNotFound())
           .andExpect(jsonPath("$.code").value("order_not_found"));
    }

    @Test
    void unexpectedFailure_hidesInternals() throws Exception {
        when(orders.find("ord_1")).thenThrow(new IllegalStateException("SQL syntax error near 'orders'"));
        mvc.perform(get("/v1/orders/ord_1"))
           .andExpect(status().isInternalServerError())
           .andExpect(jsonPath("$.detail", not(containsString("SQL"))));
    }
}
