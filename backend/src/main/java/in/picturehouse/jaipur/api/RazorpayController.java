package in.picturehouse.jaipur.api;

import in.picturehouse.jaipur.domain.RazorpayOrder;
import in.picturehouse.jaipur.domain.Showtime;
import in.picturehouse.jaipur.domain.ShowtimeSeat;
import in.picturehouse.jaipur.domain.TicketBooking;
import in.picturehouse.jaipur.integration.RazorpayClient;
import in.picturehouse.jaipur.repository.RazorpayOrderRepository;
import in.picturehouse.jaipur.repository.ShowtimeRepository;
import in.picturehouse.jaipur.repository.ShowtimeSeatRepository;
import in.picturehouse.jaipur.repository.TicketBookingRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.time.format.DateTimeFormatter;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/payments/razorpay")
public class RazorpayController {
    private final RazorpayClient razorpay;
    private final RazorpayOrderRepository orders;
    private final ShowtimeRepository showtimes;
    private final ShowtimeSeatRepository seats;
    private final TicketBookingRepository bookings;
    private static final DateTimeFormatter SHOW_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd - h:mm a");

    public RazorpayController(RazorpayClient razorpay, RazorpayOrderRepository orders,
            ShowtimeRepository showtimes, ShowtimeSeatRepository seats, TicketBookingRepository bookings) {
        this.razorpay = razorpay;
        this.orders = orders;
        this.showtimes = showtimes;
        this.seats = seats;
        this.bookings = bookings;
    }

    @PostMapping("/orders")
    @Transactional
    public CreateOrderResponse createOrder(@Valid @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        requireTestCredentials();
        if (request.seats().size() > 8 || new HashSet<>(request.seats()).size() != request.seats().size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose between 1 and 8 different seats.");
        }
        Showtime showtime = showtimes.findById(request.showtimeId())
                .filter(Showtime::isOnSale)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Showtime was not found or is not on sale."));
        List<ShowtimeSeat> requestedSeats = seats.findByShowtimeIdOrderBySeatCodeAsc(showtime.getId()).stream()
                .filter(seat -> request.seats().contains(seat.getSeatCode())).toList();
        if (requestedSeats.size() != request.seats().size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more selected seats do not belong to this showtime.");
        }
        if (requestedSeats.stream().anyMatch(ShowtimeSeat::isBooked)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "One of those seats has already been booked. Refresh the seat map and choose again.");
        }
        int amountPaise = request.seats().stream().mapToInt(RazorpayController::priceForSeatPaise).sum();
        String movieTitle = showtime.getMovie().getTitle();
        String theatreName = showtime.getScreen().getTheatre().getName();
        String showDetails = showtime.getStartsAt().format(SHOW_FORMAT);
        String receipt = "ph_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        String seatList = String.join(",", request.seats());
        Map<String, String> notes = Map.of(
                "clerk_user_id", jwt.getSubject(),
                "movie", movieTitle,
                "theatre", theatreName,
                "show", showDetails,
                "seats", seatList);

        Map<?, ?> providerOrder;
        try {
            providerOrder = razorpay.createOrder(amountPaise, receipt, notes);
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Razorpay could not create a test order. Check your test keys and backend logs.");
        }
        Object providerOrderId = providerOrder.get("id");
        if (!(providerOrderId instanceof String orderId) || orderId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Razorpay returned no order ID.");
        }
        orders.save(new RazorpayOrder(orderId, jwt.getSubject(), amountPaise,
                movieTitle, theatreName, showDetails, seatList, showtime.getId()));
        return new CreateOrderResponse(razorpay.keyId(), orderId, amountPaise, "INR");
    }

    @PostMapping("/verify")
    @Transactional
    public VerifyPaymentResponse verifyPayment(@Valid @RequestBody VerifyPaymentRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        requireTestCredentials();
        RazorpayOrder order = orders.findByProviderOrderId(request.razorpayOrderId())
                .filter(saved -> saved.getClerkUserId().equals(jwt.getSubject()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment order not found for this account."));
        var existingBooking = bookings.findByProviderOrderIdAndClerkUserId(order.getProviderOrderId(), jwt.getSubject());
        if (existingBooking.isPresent()) return response(order, existingBooking.get());
        if (order.getStatus() == RazorpayOrder.Status.REFUNDED) {
            return new VerifyPaymentResponse(false, "REFUNDED", order.getAmountPaise(), order.getMovieTitle(),
                    order.getTheatreName(), order.getShowDetails(), order.getSeats(), null, null);
        }
        if (order.getStatus() == RazorpayOrder.Status.CAPTURED
                && !request.razorpayPaymentId().equals(order.getPaymentId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This order already has a different captured payment.");
        }
        if (!razorpay.verifyCheckoutSignature(order.getProviderOrderId(), request.razorpayPaymentId(), request.razorpaySignature())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Razorpay signature did not match.");
        }

        Map<?, ?> payment;
        try {
            payment = razorpay.fetchPayment(request.razorpayPaymentId());
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not verify payment status with Razorpay.");
        }
        boolean matchesOrder = order.getProviderOrderId().equals(payment.get("order_id"));
        boolean matchesAmount = payment.get("amount") instanceof Number amount && amount.intValue() == order.getAmountPaise();
        boolean matchesCurrency = "INR".equals(payment.get("currency"));
        boolean captured = "captured".equals(payment.get("status"));
        if (!matchesOrder || !matchesAmount || !matchesCurrency || !captured) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Razorpay has not confirmed a captured payment for this order yet.");
        }
        // Orders created before the booking fields were added cannot safely reserve inventory.
        if (order.getShowtimeId() == null) return refundWithoutTicket(order, request.razorpayPaymentId());
        List<String> seatCodes = List.of(order.getSeats().split(","));
        List<ShowtimeSeat> lockedSeats = seats.lockSeats(order.getShowtimeId(), seatCodes);
        if (lockedSeats.size() != seatCodes.size() || lockedSeats.stream().anyMatch(ShowtimeSeat::isBooked)) {
            return refundWithoutTicket(order, request.razorpayPaymentId());
        }

        lockedSeats.forEach(ShowtimeSeat::markBooked);
        order.markCaptured(request.razorpayPaymentId());
        TicketBooking booking = bookings.save(new TicketBooking(order, request.razorpayPaymentId()));
        return response(order, booking);
    }

    private VerifyPaymentResponse response(RazorpayOrder order, TicketBooking booking) {
        return new VerifyPaymentResponse(true, "BOOKED", order.getAmountPaise(), order.getMovieTitle(),
                order.getTheatreName(), order.getShowDetails(), order.getSeats(),
                booking.getBookingReference(), booking.getTicketCode());
    }

    private VerifyPaymentResponse refundWithoutTicket(RazorpayOrder order, String paymentId) {
        try {
            razorpay.refundPayment(paymentId, order.getAmountPaise());
            order.markRefunded(paymentId);
            return new VerifyPaymentResponse(false, "REFUNDED", order.getAmountPaise(), order.getMovieTitle(),
                    order.getTheatreName(), order.getShowDetails(), order.getSeats(), null, null);
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Payment was captured but the seats could not be booked. Contact support with payment ID " + paymentId + " for a refund.");
        }
    }

    private void requireTestCredentials() {
        if (!razorpay.isConfiguredForTestMode()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Configure a Razorpay test Key ID and Key Secret on the Java backend first.");
        }
    }

    private static int priceForSeatPaise(String seat) {
        if (!seat.matches("[A-F][1-8]")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seat selection contains an invalid seat.");
        }
        int rupees = seat.startsWith("F") ? 500 : (seat.startsWith("D") || seat.startsWith("E")) ? 300 : 250;
        return rupees * 100;
    }

    public record CreateOrderRequest(
            @NotNull Long showtimeId,
            @NotEmpty List<@NotNull @Pattern(regexp = "[A-F][1-8]") String> seats) {}

    public record VerifyPaymentRequest(
            @NotBlank String razorpayOrderId,
            @NotBlank String razorpayPaymentId,
            @NotBlank @Pattern(regexp = "[0-9a-fA-F]{64}") String razorpaySignature) {}

    public record CreateOrderResponse(String keyId, String orderId, int amount, String currency) {}
    public record VerifyPaymentResponse(boolean verified, String status, int amountPaise,
            String movieTitle, String theatreName, String showDetails, String seats,
            String bookingReference, String ticketCode) {}
}
