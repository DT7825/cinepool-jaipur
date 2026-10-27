package in.picturehouse.jaipur.repository;

import in.picturehouse.jaipur.domain.TicketBooking;
import java.util.List;
import java.util.Optional;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketBookingRepository extends JpaRepository<TicketBooking, Long> {
    Optional<TicketBooking> findByProviderOrderIdAndClerkUserId(String providerOrderId, String clerkUserId);
    List<TicketBooking> findByClerkUserIdOrderByCreatedAtDesc(String clerkUserId);
    List<TicketBooking> findByShowtimeIdIn(Collection<Long> showtimeIds);
}
