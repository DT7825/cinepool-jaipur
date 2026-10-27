package in.picturehouse.jaipur.repository;

import in.picturehouse.jaipur.domain.TheatreOwnerMembership;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TheatreOwnerMembershipRepository extends JpaRepository<TheatreOwnerMembership, Long> {
    List<TheatreOwnerMembership> findByClerkUserIdOrderByRequestedAtDesc(String clerkUserId);
    List<TheatreOwnerMembership> findByApprovedFalseOrderByRequestedAtAsc();
    Optional<TheatreOwnerMembership> findByClerkUserIdAndTheatreId(String clerkUserId, Long theatreId);
}
