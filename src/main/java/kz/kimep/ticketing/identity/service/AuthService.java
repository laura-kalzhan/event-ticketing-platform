package kz.kimep.ticketing.identity.service;

import java.time.Clock;
import kz.kimep.ticketing.identity.api.LoginRequest;
import kz.kimep.ticketing.identity.api.RegisterRequest;
import kz.kimep.ticketing.identity.api.TokenResponse;
import kz.kimep.ticketing.identity.api.UserResponse;
import kz.kimep.ticketing.identity.domain.Role;
import kz.kimep.ticketing.identity.domain.User;
import kz.kimep.ticketing.identity.repo.UserRepository;
import kz.kimep.ticketing.shared.error.ConflictException;
import kz.kimep.ticketing.shared.error.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final Clock clock;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("An account with this email already exists. Log in instead.");
        }
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName());
        user.setRole(Role.CUSTOMER);
        user.setCreatedAt(clock.instant());
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email().trim().toLowerCase())
                .filter(User::isEnabled)
                .filter(found -> passwordEncoder.matches(request.password(), found.getPasswordHash()))
                .orElseThrow(() -> new UnauthorizedException("Email or password is wrong."));
        return tokensFor(user);
    }

    @Transactional
    public TokenResponse refresh(String refreshToken) {
        return tokensFor(refreshTokenService.consume(refreshToken));
    }

    @Transactional
    public void logout(String refreshToken, AuthenticatedUser user) {
        refreshTokenService.revoke(refreshToken, user.id());
    }

    private TokenResponse tokensFor(User user) {
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getEmail(), user.getRole().name());
        String accessToken = jwtService.createAccessToken(principal);
        String refreshToken = refreshTokenService.issue(user);
        return new TokenResponse(accessToken, refreshToken, "Bearer", jwtService.accessTokenSeconds());
    }
}
