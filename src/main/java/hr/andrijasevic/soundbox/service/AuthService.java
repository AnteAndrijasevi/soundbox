package hr.andrijasevic.soundbox.service;

import hr.andrijasevic.soundbox.domain.User;
import hr.andrijasevic.soundbox.dto.AuthResponse;
import hr.andrijasevic.soundbox.dto.LoginRequest;
import hr.andrijasevic.soundbox.dto.RegisterRequest;
import hr.andrijasevic.soundbox.exception.ConflictException;
import hr.andrijasevic.soundbox.exception.UnauthorizedException;
import hr.andrijasevic.soundbox.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, JwtService jwtService, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already registered");
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new ConflictException("Username taken");
        }

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .build();

        userRepository.save(user);

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, user.getDisplayUsername(), user.getEmail());
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, user.getDisplayUsername(), user.getEmail());
    }
}
