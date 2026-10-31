// A reusable field constraint
@Target({FIELD, PARAMETER}) @Retention(RUNTIME)
@Constraint(validatedBy = CurrencyCodeValidator.class)
public @interface CurrencyCode {
    String message() default "must be an ISO 4217 currency code";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

class CurrencyCodeValidator implements ConstraintValidator<CurrencyCode, String> {
    public boolean isValid(String value, ConstraintValidatorContext ctx) {
        // combine with @NotNull when required
        if (value == null) return true;
        try { Currency.getInstance(value); return true; } catch (IllegalArgumentException e) { return false; }
    }
}

// Cross-field rule on the record itself
record BookingRequest(@NotNull LocalDate checkIn, @NotNull LocalDate checkOut, @CurrencyCode String currency) {
    @AssertTrue(message = "checkOut must be after checkIn")
    boolean isRangeValid() { return checkIn == null || checkOut == null || checkOut.isAfter(checkIn); }
}

// Path and query parameters: annotate the controller with @Validated (or rely on Spring
// 6.1+ method validation)
@GetMapping("/v1/rates")
List<Rate> rates(@RequestParam @CurrencyCode String base, @RequestParam @Max(100) int limit) { … }
