package uz.epay.dto;

import uz.epay.entity.ProductImage;

public record ImageDto(Long id, String url, int sortOrder) {

    public static ImageDto from(ProductImage i) {
        return new ImageDto(i.getId(), i.getUrl(), i.getSortOrder());
    }
}
