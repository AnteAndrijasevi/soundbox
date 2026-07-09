package hr.andrijasevic.soundbox.service;

import hr.andrijasevic.soundbox.domain.User;
import hr.andrijasevic.soundbox.dto.AuthResponse;
import hr.andrijasevic.soundbox.dto.LoginRequest;
import hr.andrijasevic.soundbox.dto.RegisterRequest;
import hr.andrijasevic.soundbox.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtService jwtService;

    private AuthService authService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, jwtService, passwordEncoder);
    }

    @Test
    void register_savesUserWithEncodedPasswordAndReturnsToken() {
        RegisterRequest request = new RegisterRequest("ante", "ante@example.com", "password123");
        when(userRepository.existsByEmail("ante@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("ante")).thenReturn(false);
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");

        AuthResponse response = authService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getDisplayUsername()).isEqualTo("ante");
        assertThat(saved.getEmail()).isEqualTo("ante@example.com");
        assertThat(saved.getPasswordHash()).isNotEqualTo("password123");
        assertThat(passwordEncoder.matches("password123", saved.getPasswordHash())).isTrue();

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.username()).isEqualTo("ante");
        assertThat(response.email()).isEqualTo("ante@example.com");
    }

    @Test
    void register_rejectsDuplicateEmail() {
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() ->
                authService.register(new RegisterRequest("ante", "taken@example.com", "password123")))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Email already registered");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_rejectsDuplicateUsername() {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("taken")).thenReturn(true);

        assertThatThrownBy(() ->
                authService.register(new RegisterRequest("taken", "new@example.com", "password123")))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Username taken");

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_returnsTokenForValidCredentials() {
        User user = User.builder()
                .username("ante")
                .email("ante@example.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .build();
        when(userRepository.findByEmail("ante@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        AuthResponse response = authService.login(new LoginRequest("ante@example.com", "password123"));

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.username()).isEqualTo("ante");
    }

    @Test
    void login_rejectsWrongPassword() {
        User user = User.builder()
                .username("ante")
                .email("ante@example.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .build();
        when(userRepository.findByEmail("ante@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("ante@example.com", "wrong-password")))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    void login_rejectsUnknownEmail() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("ghost@example.com", "password123")))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid credentials");
    }
}
