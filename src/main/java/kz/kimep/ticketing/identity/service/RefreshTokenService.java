package kz.kimep.ticketing.identity.service;

import java.time.Clock;
import java.util.UUID;
import kz.kimep.ticketing.identity.domain.RefreshToken;
import kz.kimep.ticketing.identity.domain.User;
import kz.kimep.ticketing.identity.repo.RefreshTokenRepository;
import kz.kimep.ticketing.shared.error.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;
    private final Clock clock;

    @Transactional
    public String issue(User user) {
        RefreshToken token = new RefreshToken();
        token.setToken(UUID.randomUUID().toString());
        token.setUser(user);
        token.setExpiresAt(clock.instant().plus(jwtProperties.refreshTokenExpiration()));
        refreshTokenRepository.save(token);
        return token.getToken();
    }

    @Transactional
    public User consume(String value) {
        RefreshToken token = refreshTokenRepository.findByToken(value)
                .filter(found -> found.isUsable(clock.instant()))
                .orElseThrow(() -> new UnauthorizedException("Refresh token is invalid or expired. Log in again."));
        token.setRevoked(true);
        return token.getUser();
    }

    @Transactional
    public void revoke(String value, Long userId) {
        refreshTokenRepository.findByToken(value)
                .filter(token -> token.getUser().getId().equals(userId))
                .ifPresent(token -> token.setRevoked(true));
    }
}
