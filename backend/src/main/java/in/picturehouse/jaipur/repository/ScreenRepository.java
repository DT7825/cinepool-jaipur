package in.picturehouse.jaipur.repository;

import in.picturehouse.jaipur.domain.Screen;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScreenRepository extends JpaRepository<Screen, Long> {
    java.util.List<Screen> findByTheatre_IdIn(java.util.List<Long> theatreIds);
}
