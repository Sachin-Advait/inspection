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
    private final AuditService auditService;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;

    public LoginResponse login(LoginRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(), request.getPassword())
        );

        UserDetails userDetails =
                userDetailsService.loadUserByUsername(request.getUsername());

        String role = userDetails.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("INSPECTOR");

        String token =
                jwtService.generateToken(Map.of("role", role), userDetails);

        // ✅ CLEAN AUDIT (no role/IP here)
        auditService.log(
                userDetails.getUsername(),
                "LOGIN",
                "AUTH",
                null,
                Map.of("login", "success")
        );

        return new LoginResponse(
                token,
                userDetails.getUsername(),
                role
        );
    }
}