package uz.epay.exception;

/** 404 — so'ralgan yozuv topilmadi. */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
