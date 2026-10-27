package in.picturehouse.jaipur.repository;

import in.picturehouse.jaipur.domain.RidePost;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface RidePostRepository extends JpaRepository<RidePost, Long> {
    List<RidePost> findByRideDateAndActiveTrueOrderByDepartureAtAsc(LocalDate rideDate);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select r from RidePost r where r.id = :id")
    Optional<RidePost> findByIdForUpdate(@Param("id") Long id);
}
