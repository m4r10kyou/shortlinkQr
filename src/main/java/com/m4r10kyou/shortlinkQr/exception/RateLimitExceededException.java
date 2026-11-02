package com.m4r10kyou.shortlinkQr.exception;

public class RateLimitExceededException extends RuntimeException {

    /*
     * The full window, not the time actually remaining: the window start is held
     * inside Caffeine's entry and is not observable from here. An upper bound is
     * always true - the quota is free by then - at the cost of a client sometimes
     * waiting longer than it needs to.
     */
    private  final long retryAfterSeconds;

    public RateLimitExceededException(String message, long retryAfterSeconds) {

        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}