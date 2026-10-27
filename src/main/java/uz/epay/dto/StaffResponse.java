package uz.epay.dto;

import uz.epay.entity.Staff;
import uz.epay.entity.StaffRole;

import java.time.Instant;

public record StaffResponse(Long id, String username, String fullName, String phone, StaffRole role,
                            boolean active, Instant createdAt, Instant lastLoginAt) {

    public static StaffResponse from(Staff s) {
        return new StaffResponse(s.getId(), s.getUsername(), s.getFullName(), s.getPhone(), s.getRole(),
                s.isActive(), s.getCreatedAt(), s.getLastLoginAt());
    }
}
