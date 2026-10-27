package in.picturehouse.jaipur.integration;

import java.util.Map;

/** Provider boundary: implementations should verify signed webhooks before confirming a booking. */
public interface PaymentGateway {
    PaymentSession createCheckout(String bookingReference, int amountPaise, String currency, String provider);
    record PaymentSession(String providerReference, String checkoutUrl, Map<String,String> metadata) {}
}
