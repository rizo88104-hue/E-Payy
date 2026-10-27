package uz.epay.dto;

import uz.epay.entity.Region;

public record RegionDto(Region code, String uz, String ru, String en) {

    public static RegionDto from(Region r) {
        return new RegionDto(r, r.uz(), r.ru(), r.en());
    }
}
