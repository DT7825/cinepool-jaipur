package in.picturehouse.jaipur.api;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import in.picturehouse.jaipur.domain.TicketBooking;
import in.picturehouse.jaipur.repository.TicketBookingRepository;
import in.picturehouse.jaipur.integration.TicketEmailService;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/bookings")
public class TicketController {
    private final TicketBookingRepository bookings;
    private final TicketEmailService ticketEmail;

    public TicketController(TicketBookingRepository bookings, TicketEmailService ticketEmail) {
        this.bookings = bookings;
        this.ticketEmail = ticketEmail;
    }

    @GetMapping("/mine")
    @Transactional(readOnly = true)
    public List<TicketResponse> myTickets(@AuthenticationPrincipal Jwt jwt) {
        return bookings.findByClerkUserIdOrderByCreatedAtDesc(jwt.getSubject()).stream()
                .map(booking -> new TicketResponse(booking.getBookingReference(), booking.getMovieTitle(),
                        booking.getTheatreName(), booking.getShowDetails(), booking.getSeats(), booking.getAmountPaise(),
                        booking.getCreatedAt().toString(), qrDataUrl(booking), booking.getEmailSentAt() != null))
                .toList();
    }

    @PostMapping("/{reference}/email")
    @Transactional
    public EmailResponse emailTicket(@PathVariable String reference, @AuthenticationPrincipal Jwt jwt) {
        TicketBooking booking=bookings.findByClerkUserIdOrderByCreatedAtDesc(jwt.getSubject()).stream()
                .filter(item -> item.getBookingReference().equals(reference)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Ticket not found for this account."));
        String email=jwt.getClaimAsString("email");
        if (email == null || email.isBlank())
            throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED,"Email claim is missing. Add the primary email claim to the Clerk Session Token, then sign out and in again.");
        try { ticketEmail.send(booking,email); booking.markEmailSent(); }
        catch (IllegalStateException e) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,e.getMessage()); }
        return new EmailResponse(true,"Ticket email sent.");
    }

    private static String qrDataUrl(TicketBooking booking) {
        try {
            var matrix = new QRCodeWriter().encode(booking.getTicketCode(), BarcodeFormat.QR_CODE, 320, 320);
            ByteArrayOutputStream png = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", png);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(png.toByteArray());
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not create this ticket's QR code.");
        }
    }

    public record TicketResponse(String bookingReference, String movieTitle, String theatreName,
            String showDetails, String seats, int amountPaise, String bookedAt, String qrCodeDataUrl, boolean emailSent) {}
    public record EmailResponse(boolean sent,String message) {}
}
