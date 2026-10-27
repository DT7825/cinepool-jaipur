package in.picturehouse.jaipur.repository;

import in.picturehouse.jaipur.domain.RideJoinRequest;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface RideJoinRequestRepository extends JpaRepository<RideJoinRequest,Long> {
    List<RideJoinRequest> findByRequesterClerkUserIdOrderByCreatedAtDesc(String userId);
    List<RideJoinRequest> findByRide_ClerkUserIdOrderByCreatedAtDesc(String userId);
    java.util.Optional<RideJoinRequest> findByRide_IdAndRequesterClerkUserId(Long rideId,String userId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select r from RideJoinRequest r where r.id = :id")
    Optional<RideJoinRequest> findByIdForUpdate(@Param("id") Long id);
}
