package uz.epay.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @ParameterizedTest(name = "{0} $ × {1} = {2} so''m")
    @CsvSource({
            // yaxlitlash chegarasi: 0.5 so'm yuqoriga, undan kami pastga
            "0.01, 12650.49, 127",       // 126.5049 -> 127
            "1.00, 12650.49, 12650",     // .49 -> pastga
            "1.00, 12650.50, 12651",     // .50 -> yuqoriga (HALF_UP, banker's emas)
            "1.00, 12650.51, 12651",     // .51 -> yuqoriga
            "0.50, 1.00, 1",             // 0.5 so'm -> 1
            "0.49, 1.00, 0",             // 0.49 so'm -> 0
            "2.50, 1.00, 3",             // HALF_EVEN bo'lganda 2 chiqardi
            "0.00, 12650.00, 0",
            "1299.99, 12650.00, 16444874", // 16444873.5 -> 16444874
            // katta summa: 1 mlrd dollar
            "1000000000.00, 12650.00, 12650000000000"
    })
    void toUzs_roundsHalfUp(String usd, String rate, long expected) {
        assertThat(Money.toUzs(new BigDecimal(usd), new BigDecimal(rate))).isEqualTo(expected);
    }

    @Test
    void toUzs_overflow() {
        BigDecimal huge = new BigDecimal(Long.MAX_VALUE);
        assertThatThrownBy(() -> Money.toUzs(huge, new BigDecimal("12650.00")))
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    void toUzs_rejectsBadInput() {
        assertThatThrownBy(() -> Money.toUzs(null, BigDecimal.ONE)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Money.toUzs(BigDecimal.ONE, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Money.toUzs(new BigDecimal("-1"), BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Money.toUzs(BigDecimal.ONE, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void toTiyin() {
        assertThat(Money.toTiyin(0)).isZero();
        assertThat(Money.toTiyin(16_444_874)).isEqualTo(1_644_487_400L);
        assertThat(Money.toTiyin(Long.MAX_VALUE / 100)).isEqualTo(Long.MAX_VALUE / 100 * 100);
    }

    @Test
    void toTiyin_overflow() {
        assertThatThrownBy(() -> Money.toTiyin(Long.MAX_VALUE / 100 + 1)).isInstanceOf(ArithmeticException.class);
    }
}
