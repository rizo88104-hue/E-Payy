package uz.epay.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uz.epay.dto.PasswordRequest;
import uz.epay.dto.StaffActiveRequest;
import uz.epay.dto.StaffCreateRequest;
import uz.epay.dto.StaffResponse;
import uz.epay.service.StaffService;

import java.util.List;

@RestController
@RequestMapping("/api/admin/staff")
public class StaffController {
    private final StaffService staff;

    public StaffController(StaffService staff) {
        this.staff = staff;
    }

    @GetMapping
    public List<StaffResponse> list() {
        return staff.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StaffResponse create(@Valid @RequestBody StaffCreateRequest req) {
        return staff.create(req);
    }

    @PatchMapping("/{id}/active")
    public StaffResponse setActive(@PathVariable Long id, @Valid @RequestBody StaffActiveRequest req) {
        return staff.setActive(id, req.active());
    }

    @PutMapping("/{id}/password")
    public StaffResponse resetPassword(@PathVariable Long id, @Valid @RequestBody PasswordRequest req) {
        return staff.resetPassword(id, req.password());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        staff.delete(id);
    }
}
