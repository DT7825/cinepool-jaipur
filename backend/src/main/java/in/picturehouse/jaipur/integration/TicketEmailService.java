package in.picturehouse.jaipur.integration;

import in.picturehouse.jaipur.domain.TicketBooking;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class TicketEmailService {
    private final JavaMailSender mailSender;
    private final String from;
    public TicketEmailService(JavaMailSender mailSender, @Value("${app.mail.from:}") String from) {
        this.mailSender=mailSender; this.from=from;
    }
    public void send(TicketBooking booking, String email) {
        if (from.isBlank()) throw new IllegalStateException("Ticket email is not configured. Set SMTP settings and TICKET_EMAIL_FROM in the Java backend environment.");
        try {
            ByteArrayOutputStream image = new ByteArrayOutputStream();
            var matrix = new QRCodeWriter().encode(booking.getTicketCode(), BarcodeFormat.QR_CODE, 320, 320);
            MatrixToImageWriter.writeToStream(matrix, "PNG", image);
            MimeMessage message=mailSender.createMimeMessage();
            MimeMessageHelper helper=new MimeMessageHelper(message,true, StandardCharsets.UTF_8.name());
            helper.setFrom(from); helper.setTo(email); helper.setSubject("Your Picturehouse Jaipur ticket · " + booking.getMovieTitle());
            helper.setText("<h2>Your movie ticket is confirmed</h2><p><b>"+escape(booking.getMovieTitle())+"</b></p><p>"+escape(booking.getTheatreName())+" · "+escape(booking.getShowDetails())+"</p><p>Seats: "+escape(booking.getSeats())+"</p><p>Booking reference: "+escape(booking.getBookingReference())+"</p><p>Show the attached QR code at the theatre entrance.</p>",true);
            helper.addAttachment("entry-qr.png", () -> new java.io.ByteArrayInputStream(image.toByteArray()), "image/png");
            mailSender.send(message);
        } catch (Exception e) {
            throw new IllegalStateException("The ticket email could not be sent. Check the SMTP settings and backend log.",e);
        }
    }
    private static String escape(String value) { return value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;"); }
}
