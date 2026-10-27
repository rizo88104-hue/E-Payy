package uz.epay.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import uz.epay.exception.BadRequestException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhoneNumbersTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "+998901234567", "998901234567", "901234567",
            "+998 90 123 45 67", "+998 (90) 123-45-67", "(90) 123-45-67", " 90 123 45 67 "
    })
    void normalize_validFormats(String input) {
        assertThat(PhoneNumbers.normalize(input)).isEqualTo("+998901234567");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "   ", "90123456", "9012345678", "99890123456", "9989012345678",
            "997901234567", "+7 901 234 56 78", "abc", "telefon yo'q"
    })
    void normalize_invalidFormats(String input) {
        assertThatThrownBy(() -> PhoneNumbers.normalize(input))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(PhoneNumbers.INVALID);
    }

    @ParameterizedTest
    @ValueSource(strings = {"+998901234567", "998 90 123 45 67", "901234567"})
    void key_isLastNineDigits(String input) {
        assertThat(PhoneNumbers.key(input)).isEqualTo("901234567");
    }

    @Test
    void key_shortOrNull() {
        assertThat(PhoneNumbers.key("12-34")).isEqualTo("1234");
        assertThat(PhoneNumbers.key(null)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"+998901234567", "901234567", "+998 90 123 45 67"})
    void mask_hidesMiddleDigits(String input) {
        assertThat(PhoneNumbers.mask(input)).isEqualTo("+998 90 *** ** 67");
    }

    @Test
    void mask_invalidNumber() {
        assertThatThrownBy(() -> PhoneNumbers.mask("123")).isInstanceOf(BadRequestException.class);
    }
}
