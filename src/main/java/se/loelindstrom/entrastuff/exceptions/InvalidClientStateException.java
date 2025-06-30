package se.loelindstrom.entrastuff.exceptions;

public class InvalidClientStateException extends RuntimeException {
    public InvalidClientStateException() {
    }

    public InvalidClientStateException(String message) {
        super(message);
    }

    public InvalidClientStateException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidClientStateException(Throwable cause) {
        super(cause);
    }

    public InvalidClientStateException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
