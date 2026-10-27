package in.picturehouse.jaipur.repository;

import in.picturehouse.jaipur.domain.RazorpayOrder;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RazorpayOrderRepository extends JpaRepository<RazorpayOrder, Long> {
    Optional<RazorpayOrder> findByProviderOrderId(String providerOrderId);
}
