package in.picturehouse.jaipur.repository;

import in.picturehouse.jaipur.domain.Theatre;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TheatreRepository extends JpaRepository<Theatre, Long> {
    List<Theatre> findByActiveTrueAndCityIgnoreCaseOrderByNameAsc(String city);
}
