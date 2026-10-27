package uz.epay.exception;

/** 403 — bu amalga huquq yo'q. */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
