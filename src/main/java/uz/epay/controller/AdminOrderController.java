package uz.epay.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.epay.dto.OrderAssignRequest;
import uz.epay.dto.OrderDetails;
import uz.epay.dto.OrderNoteRequest;
import uz.epay.dto.OrderStatusRequest;
import uz.epay.dto.OrderSummary;
import uz.epay.dto.PageResponse;
import uz.epay.entity.OrderStatus;
import uz.epay.entity.PaymentStatus;
import uz.epay.service.OrderService;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {
    private final OrderService orders;

    public AdminOrderController(OrderService orders) {
        this.orders = orders;
    }

    @GetMapping
    public PageResponse<OrderSummary> list(@RequestParam(required = false) OrderStatus status,
                                           @RequestParam(required = false) PaymentStatus payStatus,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                           @RequestParam(required = false) String q,
                                           @RequestParam(defaultValue = "0") @Min(0) int page,
                                           @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return orders.list(status, payStatus, from, to, q, page, size);
    }

    @GetMapping("/{id}")
    public OrderDetails get(@PathVariable Long id) {
        return orders.details(id);
    }

    @PostMapping("/{id}/take")
    public OrderDetails take(@PathVariable Long id) {
        return orders.take(id);
    }

    @PutMapping("/{id}/operator")
    public OrderDetails assign(@PathVariable Long id, @Valid @RequestBody OrderAssignRequest req) {
        return orders.assign(id, req.operatorId());
    }

    @PatchMapping("/{id}/status")
    public OrderDetails changeStatus(@PathVariable Long id, @Valid @RequestBody OrderStatusRequest req) {
        return orders.changeStatus(id, req.status());
    }

    @PatchMapping("/{id}/note")
    public OrderDetails note(@PathVariable Long id, @Valid @RequestBody OrderNoteRequest req) {
        return orders.note(id, req.note());
    }

    @PatchMapping("/{id}/refunded")
    public OrderDetails refunded(@PathVariable Long id) {
        return orders.markRefunded(id);
    }
}
