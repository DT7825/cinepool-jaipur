package in.picturehouse.jaipur.repository;

import in.picturehouse.jaipur.domain.Movie;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieRepository extends JpaRepository<Movie, Long> {
    List<Movie> findByActiveTrueOrderByTitleAsc();
    java.util.Optional<Movie> findByTitleIgnoreCase(String title);
}
