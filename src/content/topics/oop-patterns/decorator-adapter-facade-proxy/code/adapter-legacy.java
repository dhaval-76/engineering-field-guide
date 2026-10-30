// What our code expects:
public interface PaymentProcessor { PaymentResult pay(Payment p); }

// What we have (vendor SDK / legacy class we can't change):
class LegacyGateway {
    Map<String, String> initiatePayment(Map<String, String> params) { /* ... */ return Map.of(); }
}

// Adapter: implements OUR interface, translates to THEIR API.
final class LegacyGatewayAdapter implements PaymentProcessor {
    private final LegacyGateway gateway;
    LegacyGatewayAdapter(LegacyGateway gateway) { this.gateway = gateway; }

    public PaymentResult pay(Payment p) {
        Map<String, String> response = gateway.initiatePayment(Map.of(
            "txn_ref", p.id(),
            "amt", p.amount().movePointRight(2).toPlainString(),   // rupees → paise as text
            "cur", p.currency().getCurrencyCode()));
        return switch (response.getOrDefault("status", "E")) {
            case "S" -> PaymentResult.success(response.get("gateway_ref"));
            case "P" -> PaymentResult.pending(response.get("gateway_ref"));
            default  -> PaymentResult.failed(response.getOrDefault("err", "unknown"));
        };
    }
}
