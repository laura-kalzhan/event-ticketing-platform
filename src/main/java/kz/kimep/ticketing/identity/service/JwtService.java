package kz.kimep.ticketing.identity.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final String ROLE_CLAIM = "role";
    private static final String EMAIL_CLAIM = "email";

    private final SecretKey key;
    private final Duration accessTokenExpiration;
    private final Clock clock;

    public JwtService(JwtProperties properties, Clock clock) {
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = properties.accessTokenExpiration();
        this.clock = clock;
    }

    public String createAccessToken(AuthenticatedUser user) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(String.valueOf(user.id()))
                .claim(EMAIL_CLAIM, user.email())
                .claim(ROLE_CLAIM, user.role())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenExpiration)))
                .signWith(key)
                .compact();
    }

    public AuthenticatedUser parseAccessToken(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        return new AuthenticatedUser(Long.valueOf(claims.getSubject()),
                claims.get(EMAIL_CLAIM, String.class),
                claims.get(ROLE_CLAIM, String.class));
    }

    public long accessTokenSeconds() {
        return accessTokenExpiration.toSeconds();
    }
}
