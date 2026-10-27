package in.picturehouse.jaipur.integration;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Small server-only client for Razorpay Orders and Payments APIs. */
@Component
public class RazorpayClient {
    private static final String API_ROOT = "https://api.razorpay.com/v1";
    private final RestClient http = RestClient.builder().baseUrl(API_ROOT).build();
    private final String keyId;
    private final String keySecret;

    public RazorpayClient(@Value("${app.razorpay.key-id:}") String keyId,
            @Value("${app.razorpay.key-secret:}") String keySecret) {
        this.keyId = keyId;
        this.keySecret = keySecret;
    }

    public String keyId() { return keyId; }

    public boolean isConfiguredForTestMode() {
        return keyId.startsWith("rzp_test_") && !keySecret.isBlank();
    }

    public Map<?, ?> createOrder(int amountPaise, String receipt, Map<String, String> notes) {
        return http.post().uri("/orders")
                .headers(headers -> headers.setBasicAuth(keyId, keySecret))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("amount", amountPaise, "currency", "INR", "receipt", receipt, "notes", notes))
                .retrieve().body(Map.class);
    }

    public Map<?, ?> fetchPayment(String paymentId) {
        return http.get().uri("/payments/{id}", paymentId)
                .headers(headers -> headers.setBasicAuth(keyId, keySecret))
                .retrieve().body(Map.class);
    }

    public Map<?, ?> refundPayment(String paymentId, int amountPaise) {
        return http.post().uri("/payments/{id}/refund", paymentId)
                .headers(headers -> headers.setBasicAuth(keyId, keySecret))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("amount", amountPaise))
                .retrieve().body(Map.class);
    }

    public boolean verifyCheckoutSignature(String orderId, String paymentId, String signature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(keySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expected = HexFormat.of().formatHex(mac.doFinal((orderId + "|" + paymentId).getBytes(StandardCharsets.UTF_8)));
            return java.security.MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.US_ASCII), signature.getBytes(StandardCharsets.US_ASCII));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not verify the Razorpay payment signature.", exception);
        }
    }
}
