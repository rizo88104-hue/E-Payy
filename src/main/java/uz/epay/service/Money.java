package uz.epay.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Pul hisoblari (CLAUDE.md "Pul qoidalari"): narx dollarda BigDecimal, so'm — butun son (long), double yo'q.
 * Toshib ketish jim o'tib ketmaydi — ArithmeticException.
 */
public final class Money {

    private Money() {
    }

    /** Dollar × kurs → butun so'm, HALF_UP bilan yaxlitlangan (0.5 so'm yuqoriga). */
    public static long toUzs(BigDecimal usd, BigDecimal rate) {
        if (usd == null || rate == null) {
            throw new IllegalArgumentException("Summa va kurs bo'sh bo'lmasligi kerak");
        }
        if (usd.signum() < 0 || rate.signum() <= 0) {
            throw new IllegalArgumentException("Summa manfiy, kurs esa musbat bo'lishi kerak");
        }
        return usd.multiply(rate).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    /** So'm → tiyin (Payme summani tiyinda qabul qiladi). */
    public static long toTiyin(long uzs) {
        return Math.multiplyExact(uzs, 100);
    }
}
