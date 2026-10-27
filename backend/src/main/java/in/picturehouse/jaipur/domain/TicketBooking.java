package in.picturehouse.jaipur.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

/** A paid, confirmed ticket created only after Razorpay confirms a captured payment. */
@Entity
@Table(name = "ticket_bookings", uniqueConstraints = {
        @UniqueConstraint(columnNames = "provider_order_id"),
        @UniqueConstraint(columnNames = "ticket_code")
})
public class TicketBooking {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_reference", nullable = false, unique = true, length = 36)
    private String bookingReference = UUID.randomUUID().toString();

    @Column(name = "ticket_code", nullable = false, unique = true, length = 36)
    private String ticketCode = UUID.randomUUID().toString();

    @Column(name = "provider_order_id", nullable = false, unique = true)
    private String providerOrderId;

    @Column(nullable = false)
    private String paymentId;

    @Column(name = "clerk_user_id", nullable = false)
    private String clerkUserId;

    @Column(name = "showtime_id", nullable = false)
    private Long showtimeId;

    @Column(nullable = false)
    private String movieTitle;

    @Column(nullable = false)
    private String theatreName;

    @Column(nullable = false)
    private String showDetails;

    @Column(nullable = false, length = 40)
    private String seats;

    @Column(nullable = false)
    private int amountPaise;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "email_sent_at")
    private Instant emailSentAt;

    protected TicketBooking() {}

    public TicketBooking(RazorpayOrder order, String paymentId) {
        this.providerOrderId = order.getProviderOrderId();
        this.paymentId = paymentId;
        this.clerkUserId = order.getClerkUserId();
        this.showtimeId = order.getShowtimeId();
        this.movieTitle = order.getMovieTitle();
        this.theatreName = order.getTheatreName();
        this.showDetails = order.getShowDetails();
        this.seats = order.getSeats();
        this.amountPaise = order.getAmountPaise();
    }

    public String getBookingReference() { return bookingReference; }
    public String getTicketCode() { return ticketCode; }
    public String getProviderOrderId() { return providerOrderId; }
    public String getPaymentId() { return paymentId; }
    public String getClerkUserId() { return clerkUserId; }
    public Long getShowtimeId() { return showtimeId; }
    public String getMovieTitle() { return movieTitle; }
    public String getTheatreName() { return theatreName; }
    public String getShowDetails() { return showDetails; }
    public String getSeats() { return seats; }
    public int getAmountPaise() { return amountPaise; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getEmailSentAt() { return emailSentAt; }
    public void markEmailSent() { this.emailSentAt = Instant.now(); }
}
