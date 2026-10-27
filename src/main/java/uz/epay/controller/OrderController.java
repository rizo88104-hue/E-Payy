package uz.epay.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uz.epay.dto.OrderPlacedResponse;
import uz.epay.dto.OrderRequest;
import uz.epay.dto.OrderTrackRequest;
import uz.epay.dto.OrderTrackResponse;
import uz.epay.dto.RegionDto;
import uz.epay.entity.Region;
import uz.epay.service.OrderService;

import java.util.Arrays;
import java.util.List;

/** Xaridor (login yo'q): buyurtma berish, kuzatish va hududlar ro'yxati. IP — RateLimiter uchun. */
@RestController
@RequestMapping("/api")
public class OrderController {
    private final OrderService orders;

    public OrderController(OrderService orders) {
        this.orders = orders;
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderPlacedResponse create(@Valid @RequestBody OrderRequest req, HttpServletRequest http) {
        return orders.create(req, http.getRemoteAddr());
    }

    @PostMapping("/orders/track")
    public OrderTrackResponse track(@Valid @RequestBody OrderTrackRequest req, HttpServletRequest http) {
        return orders.track(req.number(), req.phone(), http.getRemoteAddr());
    }

    @GetMapping("/regions")
    public List<RegionDto> regions() {
        return Arrays.stream(Region.values()).map(RegionDto::from).toList();
    }
}
