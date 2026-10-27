package uz.epay.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.epay.dto.StorefrontConfig;
import uz.epay.service.StorefrontService;

@RestController
@RequestMapping("/api/storefront")
public class StorefrontController {
    private final StorefrontService storefront;

    public StorefrontController(StorefrontService storefront) {
        this.storefront = storefront;
    }

    @GetMapping("/config")
    public StorefrontConfig config() {
        return storefront.config();
    }
}
