package kz.kimep.ticketing.identity.repo;

import java.util.Optional;
import kz.kimep.ticketing.identity.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
