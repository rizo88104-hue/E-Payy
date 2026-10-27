package uz.epay.dto;

import uz.epay.entity.Category;

public record CategoryDto(Long id, String name, int sortOrder) {

    public static CategoryDto from(Category c) {
        return new CategoryDto(c.getId(), c.getName(), c.getSortOrder());
    }
}
