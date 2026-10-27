package uz.epay.service;

public interface ProductUsageChecker {

    boolean isUsedInOrders(Long productId);
}
