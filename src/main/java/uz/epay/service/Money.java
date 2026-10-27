package uz.epay.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Money {

    private Money() {
    }

    public static long toUzs(BigDecimal usd, BigDecimal rate) {
        if (usd == null || rate == null) {
            throw new IllegalArgumentException("Summa va kurs bo'sh bo'lmasligi kerak");
        }
        if (usd.signum() < 0 || rate.signum() <= 0) {
            throw new IllegalArgumentException("Summa manfiy, kurs esa musbat bo'lishi kerak");
        }
        return usd.multiply(rate).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    public static long toTiyin(long uzs) {
        return Math.multiplyExact(uzs, 100);
    }
}
