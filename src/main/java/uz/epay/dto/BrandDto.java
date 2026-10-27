package uz.epay.dto;

import uz.epay.entity.Brand;

public record BrandDto(Long id, String name, String slug, String logo, String description,
                       String country, Integer founded, String website) {

    public static BrandDto from(Brand b) {
        return new BrandDto(b.getId(), b.getName(), b.getSlug(), b.getLogo(), b.getDescription(),
                b.getCountry(), b.getFounded(), b.getWebsite());
    }
}
