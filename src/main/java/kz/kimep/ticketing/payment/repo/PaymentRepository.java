package kz.kimep.ticketing.payment.repo;

import java.util.Optional;
import kz.kimep.ticketing.payment.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByBookingId(Long bookingId);
}
