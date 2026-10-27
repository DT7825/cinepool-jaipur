package in.picturehouse.jaipur.repository;

import in.picturehouse.jaipur.domain.RideChatMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RideChatMessageRepository extends JpaRepository<RideChatMessage,Long> {
    List<RideChatMessage> findByRequest_IdOrderByCreatedAtAsc(Long requestId);
}
