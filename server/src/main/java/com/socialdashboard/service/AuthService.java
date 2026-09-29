package com.socialdashboard.service;

import com.socialdashboard.dto.Dtos;
import com.socialdashboard.entity.User;
import com.socialdashboard.exception.ApiException;
import com.socialdashboard.repository.UserRepository;
import com.socialdashboard.security.AppUserDetailsService;
import com.socialdashboard.security.JwtService;
import com.socialdashboard.security.UserPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AppUserDetailsService userDetailsService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       AppUserDetailsService userDetailsService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Transactional
    public Dtos.AuthResponse register(Dtos.RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ApiException(org.springframework.http.HttpStatus.CONFLICT,
                    "An account with that email already exists", "email");
        }
        User user = new User(email, passwordEncoder.encode(request.password()), request.name().trim());
        userRepository.save(user);
        return issueToken(user);
    }

    @Transactional(readOnly = true)
    public Dtos.AuthResponse login(Dtos.LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password()));
        } catch (DisabledException ex) {
            throw ApiException.badRequest("This account has been disabled");
        } catch (BadCredentialsException ex) {
            throw new ApiException(org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "Incorrect email or password");
        }
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ApiException(org.springframework.http.HttpStatus.UNAUTHORIZED,
                        "Incorrect email or password"));
        return issueToken(user);
    }

    @Transactional(readOnly = true)
    public Dtos.UserResponse profile(Long userId) {
        return Dtos.UserResponse.from(requireUser(userId));
    }

    @Transactional
    public Dtos.UserResponse updateProfile(Long userId, Dtos.UpdateProfileRequest request) {
        User user = requireUser(userId);

        if (request.newPassword() != null && !request.newPassword().isBlank()) {
            if (request.currentPassword() == null || request.currentPassword().isBlank()) {
                throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST,
                        "Current password is required to set a new password", "currentPassword");
            }
            if (request.newPassword().length() < 8) {
                throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST,
                        "New password must be at least 8 characters", "newPassword");
            }
            if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
                throw new ApiException(org.springframework.http.HttpStatus.UNAUTHORIZED,
                        "Current password is incorrect", "currentPassword");
            }
            user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        }

        if (request.name() != null && !request.name().isBlank()) {
            user.setName(request.name().trim());
        }
        if (request.bio() != null) {
            user.setBio(request.bio().isBlank() ? null : request.bio().trim());
        }
        userRepository.save(user);
        return Dtos.UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("User"));
    }

    private Dtos.AuthResponse issueToken(User user) {
        UserPrincipal principal = userDetailsService.loadPrincipal(user.getEmail());
        String token = jwtService.generateToken(principal);
        return new Dtos.AuthResponse(token, jwtService.getExpirationMs(), Dtos.UserResponse.from(user));
    }
}
