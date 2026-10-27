package in.picturehouse.jaipur.repository;

import in.picturehouse.jaipur.domain.Showtime;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShowtimeRepository extends JpaRepository<Showtime, Long> {
    List<Showtime> findByOnSaleTrueAndStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(LocalDateTime start, LocalDateTime end);
    List<Showtime> findByScreen_Theatre_IdInAndOnSaleTrueOrderByStartsAtAsc(List<Long> theatreIds);
    List<Showtime> findByMovie_Id(Long movieId);
}
