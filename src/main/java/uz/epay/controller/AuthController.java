package uz.epay.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uz.epay.dto.ChangeOwnPasswordRequest;
import uz.epay.dto.StaffResponse;
import uz.epay.service.StaffService;

/** Kirgan xodim haqida (har qanday rol). Login/logout — Spring Security: POST /login, POST /logout. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final StaffService staff;

    public AuthController(StaffService staff) {
        this.staff = staff;
    }

    @GetMapping("/me")
    public StaffResponse me(Authentication auth) {
        return staff.me(auth.getName());
    }

    /** O'z parolini almashtirish: boshqa qurilmalardagi sessiyalar yopiladi, joriysi qoladi. */
    @PostMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangeOwnPasswordRequest req, Authentication auth) {
        staff.changeOwnPassword(auth.getName(), req.currentPassword(), req.newPassword());
    }
}
