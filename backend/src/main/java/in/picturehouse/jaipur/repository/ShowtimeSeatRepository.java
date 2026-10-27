package in.picturehouse.jaipur.repository;

import in.picturehouse.jaipur.domain.ShowtimeSeat;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface ShowtimeSeatRepository extends JpaRepository<ShowtimeSeat, Long> {
    List<ShowtimeSeat> findByShowtimeIdOrderBySeatCodeAsc(Long showtimeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select seat from ShowtimeSeat seat where seat.showtime.id = :showtimeId and seat.seatCode in :seatCodes order by seat.seatCode")
    List<ShowtimeSeat> lockSeats(@Param("showtimeId") Long showtimeId, @Param("seatCodes") List<String> seatCodes);
}
