package in.picturehouse.jaipur.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/** A short-lived Razorpay test order owned by the Clerk user that created it. */
@Entity
@Table(name = "razorpay_orders", uniqueConstraints = @UniqueConstraint(columnNames = "provider_order_id"))
public class RazorpayOrder {
    public enum Status { CREATED, CAPTURED, REFUNDED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider_order_id", nullable = false, unique = true)
    private String providerOrderId;

    @Column(nullable = false)
    private String clerkUserId;

    @Column(nullable = false)
    private int amountPaise;

    @Column(nullable = false)
    private String movieTitle;

    @Column(nullable = false)
    private String theatreName;

    @Column(nullable = false)
    private String showDetails;

    @Column(nullable = false)
    private String seats;

    @Column
    private Long showtimeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.CREATED;

    private String paymentId;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected RazorpayOrder() {}

    public RazorpayOrder(String providerOrderId, String clerkUserId, int amountPaise,
            String movieTitle, String theatreName, String showDetails, String seats, Long showtimeId) {
        this.providerOrderId = providerOrderId;
        this.clerkUserId = clerkUserId;
        this.amountPaise = amountPaise;
        this.movieTitle = movieTitle;
        this.theatreName = theatreName;
        this.showDetails = showDetails;
        this.seats = seats;
        this.showtimeId = showtimeId;
    }

    public String getProviderOrderId() { return providerOrderId; }
    public String getClerkUserId() { return clerkUserId; }
    public int getAmountPaise() { return amountPaise; }
    public String getMovieTitle() { return movieTitle; }
    public String getTheatreName() { return theatreName; }
    public String getShowDetails() { return showDetails; }
    public String getSeats() { return seats; }
    public Long getShowtimeId() { return showtimeId; }
    public Status getStatus() { return status; }
    public String getPaymentId() { return paymentId; }
    public Instant getCreatedAt() { return createdAt; }

    public void markCaptured(String paymentId) {
        this.paymentId = paymentId;
        this.status = Status.CAPTURED;
    }

    public void markRefunded(String paymentId) {
        this.paymentId = paymentId;
        this.status = Status.REFUNDED;
    }
}
