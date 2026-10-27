package uz.epay.exception;

import java.time.Duration;

/** 429 — limitdan oshdi. retryAfter javobdagi Retry-After sarlavhasiga yoziladi. */
public class TooManyRequestsException extends RuntimeException {
    private final Duration retryAfter;

    public TooManyRequestsException(String message, Duration retryAfter) {
        super(message);
        this.retryAfter = retryAfter;
    }

    public Duration getRetryAfter() {
        return retryAfter;
    }
}
