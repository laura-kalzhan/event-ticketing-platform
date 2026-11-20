package kz.kimep.ticketing.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final AuthenticatedUser USER = new AuthenticatedUser(4L, "customer1@kimep.kz", "CUSTOMER");

    private final JwtService jwtService = service("first-test-secret-that-is-long-enough-123", Clock.systemUTC());

    @Test
    void tokenKeepsTheUserIdEmailAndRole() {
        AuthenticatedUser parsed = jwtService.parseAccessToken(jwtService.createAccessToken(USER));

        assertThat(parsed).isEqualTo(USER);
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        JwtService attacker = service("attacker-secret-that-is-also-long-enough-99", Clock.systemUTC());
        String forged = attacker.createAccessToken(USER);

        assertThatThrownBy(() -> jwtService.parseAccessToken(forged)).isInstanceOf(JwtException.class);
    }

    @Test
    void expiredTokenIsRejected() {
        Clock yesterday = Clock.fixed(Instant.now().minus(Duration.ofDays(1)), ZoneOffset.UTC);
        String old = service("first-test-secret-that-is-long-enough-123", yesterday).createAccessToken(USER);

        assertThatThrownBy(() -> jwtService.parseAccessToken(old)).isInstanceOf(ExpiredJwtException.class);
    }

    private static JwtService service(String secret, Clock clock) {
        return new JwtService(new JwtProperties(secret, Duration.ofMinutes(15), Duration.ofDays(7)), clock);
    }
}
