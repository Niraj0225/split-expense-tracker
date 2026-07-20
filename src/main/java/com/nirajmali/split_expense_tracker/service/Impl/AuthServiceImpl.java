package com.nirajmali.split_expense_tracker.service.Impl;

import com.nirajmali.split_expense_tracker.dto.AuthDTO;
import com.nirajmali.split_expense_tracker.entity.User;
import com.nirajmali.split_expense_tracker.repository.UserRepository;
import com.nirajmali.split_expense_tracker.security.CustomUserDetailsService;
import com.nirajmali.split_expense_tracker.security.JwtAuthFilter;
import com.nirajmali.split_expense_tracker.security.JwtUtil;
import com.nirajmali.split_expense_tracker.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;

    @Override
    public AuthDTO.AuthResponse register(AuthDTO.RegisterRequest request) {
        Optional<User> user=userRepository.findByEmail(request.getEmail());
        if (user.isPresent()){
            throw new RuntimeException("Email already registered: "+ request.getEmail());
        }

        User createNewUser=User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        User savedUser=userRepository.save(createNewUser);

        UserDetails userDetails=userDetailsService.loadUserByUsername(savedUser.getEmail());
        String token=jwtUtil.generateToken(userDetails);

        return AuthDTO.AuthResponse.builder()
                .userId(savedUser.getId())
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .token(token)
                .build();

    }

    @Override
    public AuthDTO.AuthResponse login(AuthDTO.LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        Optional<User> user=userRepository.findByEmail(request.getEmail());
        if (user.isEmpty()){
            throw new BadCredentialsException("User not found with email: "+ request.getEmail());
        }

        UserDetails userDetails=userDetailsService.loadUserByUsername(user.get().getEmail());
        String token=jwtUtil.generateToken(userDetails);


        return AuthDTO.AuthResponse.builder()
                .userId(user.get().getId())
                .name(user.get().getName())
                .email(user.get().getEmail())
                .token(token)
                .build();
    }
}
