package uz.epay.dto;

import uz.epay.entity.Region;

/** Hudud kodi va nomlari 3 tilda (frontend tanlov ro'yxati uchun). */
public record RegionDto(Region code, String uz, String ru, String en) {

    public static RegionDto from(Region r) {
        return new RegionDto(r, r.uz(), r.ru(), r.en());
    }
}
