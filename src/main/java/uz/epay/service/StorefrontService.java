package uz.epay.service;

import org.springframework.stereotype.Service;
import uz.epay.config.EpayProperties;
import uz.epay.dto.StorefrontConfig;
import uz.epay.entity.PaymentMethod;

import java.util.List;

@Service
public class StorefrontService {
    private final EpayProperties props;

    public StorefrontService(EpayProperties props) {
        this.props = props;
    }

    public StorefrontConfig config() {
        List<StorefrontConfig.Warranty> warranty = props.order().warranty().entrySet().stream()
                .map(e -> new StorefrontConfig.Warranty(e.getKey(), e.getValue()))
                .toList();
        return new StorefrontConfig(props.payment().usdRate(), warranty, List.of(PaymentMethod.CASH));
    }
}
