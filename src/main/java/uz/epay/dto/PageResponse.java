package uz.epay.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/** Sahifalangan javob. Spring'ning PageImpl ini to'g'ridan-to'g'ri JSON qilish barqaror emas — o'z shaklimiz. */
public record PageResponse<T>(List<T> items, int page, int size, long totalElements, int totalPages) {

    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(page.getContent().stream().map(mapper).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
