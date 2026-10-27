package in.picturehouse.jaipur.integration;

/** Send only after payment confirmation; QR content must be opaque, signed, single-use and time-bound. */
public interface TicketDeliveryGateway {
    void sendEmail(String to, String bookingReference, byte[] ticketPdf, byte[] qrPng);
    void sendWhatsApp(String e164Phone, String bookingReference, String templateId);
}
