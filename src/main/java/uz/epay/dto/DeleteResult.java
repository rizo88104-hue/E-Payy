package uz.epay.dto;

/** Mahsulotni o'chirish natijasi: DELETED — butunlay o'chirildi; ARCHIVED — buyurtmalarda bor, arxivlandi. */
public record DeleteResult(Outcome outcome) {

    public enum Outcome {
        DELETED,
        ARCHIVED
    }
}
