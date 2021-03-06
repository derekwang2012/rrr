package org.lol.reddit.common;

public class UnexpectedInternalStateException extends RuntimeException {

    public UnexpectedInternalStateException() {
        super("The application's internal state is invalid");
    }

    public UnexpectedInternalStateException(String message) {
        super(message);
    }
}
