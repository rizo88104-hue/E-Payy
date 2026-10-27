package uz.epay.dto;

import uz.epay.entity.PaymentMethod;

import java.math.BigDecimal;
import java.util.List;

public record StorefrontConfig(BigDecimal usdRate, List<Warranty> warranty, List<PaymentMethod> paymentMethods) {

    public record Warranty(int years, int percent) {
    }
}
