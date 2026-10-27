package uz.epay.exception;

/** 400 — so'rov noto'g'ri (format, biznes qoidasi). */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
