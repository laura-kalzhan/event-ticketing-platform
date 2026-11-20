package kz.kimep.ticketing.identity.repo;

import java.util.Optional;
import kz.kimep.ticketing.identity.domain.RefreshToken;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    @EntityGraph(attributePaths = "user")
    Optional<RefreshToken> findByToken(String token);
}
