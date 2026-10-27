package uz.epay.dto;

public record DeleteResult(Outcome outcome) {

    public enum Outcome {
        DELETED,
        ARCHIVED
    }
}
