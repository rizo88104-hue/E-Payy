package uz.epay.exception;

/** 409 — amal joriy holat bilan to'qnashadi (takror, noto'g'ri holat o'tishi). */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
