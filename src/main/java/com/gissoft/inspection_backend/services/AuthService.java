package com.gissoft.inspection_backend.services;

import com.gissoft.inspection_backend.dto.LoginRequest;
import com.gissoft.inspection_backend.dto.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;


    public LoginResponse login(LoginRequest request) {

        // Throws BadCredentialsException if wrong — propagates to 401
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(), request.getPassword())
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());

        // Embed role as an extra claim so the frontend can read it without decoding authorities
        String role = userDetails.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("INSPECTOR");

        String token = jwtService.generateToken(Map.of("role", role), userDetails);

        return new LoginResponse(token, userDetails.getUsername(), role);
    }
}
