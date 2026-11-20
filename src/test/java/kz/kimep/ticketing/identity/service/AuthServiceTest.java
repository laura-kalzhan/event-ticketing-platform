package kz.kimep.ticketing.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;
import kz.kimep.ticketing.identity.api.LoginRequest;
import kz.kimep.ticketing.identity.api.RegisterRequest;
import kz.kimep.ticketing.identity.api.TokenResponse;
import kz.kimep.ticketing.identity.domain.Role;
import kz.kimep.ticketing.identity.domain.User;
import kz.kimep.ticketing.identity.repo.UserRepository;
import kz.kimep.ticketing.shared.error.ConflictException;
import kz.kimep.ticketing.shared.error.UnauthorizedException;
import kz.kimep.ticketing.testing.MutableClock;
import kz.kimep.ticketing.testing.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenService refreshTokenService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final MutableClock clock = new MutableClock(TestData.NOW);
    private AuthService authService;

    @BeforeEach
    void setUp() {
        JwtService jwtService = new JwtService(new JwtProperties("unit-test-secret-that-is-long-enough-1234",
                Duration.ofMinutes(15), Duration.ofDays(7)), clock);
        authService = new AuthService(userRepository, passwordEncoder, jwtService, refreshTokenService, clock);
    }

    @Test
    void registeringAnExistingEmailIsAConflict() {
        when(userRepository.existsByEmail("customer1@kimep.kz")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("Customer1@kimep.kz", "Password123!", "D")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void registeringStoresABcryptHashAndNeverThePassword() {
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        authService.register(new RegisterRequest("new@kimep.kz", "Password123!", "New Person"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getPasswordHash()).isNotEqualTo("Password123!").startsWith("$2a$");
        assertThat(passwordEncoder.matches("Password123!", saved.getValue().getPasswordHash())).isTrue();
        assertThat(saved.getValue().getRole()).isEqualTo(Role.CUSTOMER);
    }

    @Test
    void wrongPasswordIsUnauthorized() {
        when(userRepository.findByEmail("customer1@kimep.kz")).thenReturn(Optional.of(user()));

        assertThatThrownBy(() -> authService.login(new LoginRequest("customer1@kimep.kz", "wrong-password")))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void correctPasswordReturnsAccessAndRefreshTokens() {
        User user = user();
        when(userRepository.findByEmail("customer1@kimep.kz")).thenReturn(Optional.of(user));
        when(refreshTokenService.issue(user)).thenReturn("refresh-1");

        TokenResponse tokens = authService.login(new LoginRequest("customer1@kimep.kz", "Password123!"));

        assertThat(tokens.accessToken()).isNotBlank();
        assertThat(tokens.refreshToken()).isEqualTo("refresh-1");
        assertThat(tokens.expiresIn()).isEqualTo(900);
    }

    private User user() {
        User user = new User();
        user.setId(4L);
        user.setEmail("customer1@kimep.kz");
        user.setPasswordHash(passwordEncoder.encode("Password123!"));
        user.setRole(Role.CUSTOMER);
        user.setEnabled(true);
        return user;
    }
}
