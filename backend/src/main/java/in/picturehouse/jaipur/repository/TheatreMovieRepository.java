package in.picturehouse.jaipur.repository;

import in.picturehouse.jaipur.domain.TheatreMovie;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TheatreMovieRepository extends JpaRepository<TheatreMovie,Long>{
    List<TheatreMovie> findByTheatre_IdIn(List<Long> theatreIds);
    Optional<TheatreMovie> findByTheatre_IdAndMovie_Id(Long theatreId,Long movieId);
    long countByMovie_Id(Long movieId);
}
