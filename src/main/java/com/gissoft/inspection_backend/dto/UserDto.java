package com.gissoft.inspection_backend.dto;

import jakarta.validation.constraints.NotBlank;

public class UserDto {

    public record CreateUserRequest(
            @NotBlank String username,
            @NotBlank String password,
            @NotBlank String role,       // INSPECTOR | SUPERVISOR | MANAGER | ADMIN
            String fullName,
            String phone,
            String dgAccess,             // ALL | HEALTH | TECHNICAL
            Long   fineLimit,
            Long   supervisorFineLimit,
            Long   managerFineLimit,
            boolean canCloseCases
    ) {}

    public record UpdateUserRequest(
            String role,
            String fullName,
            String phone,
            String dgAccess,
            Long   fineLimit,
            Long   supervisorFineLimit,
            Long   managerFineLimit,
            boolean canCloseCases,
            String newPassword           // optional — only set to change password
    ) {}
}
