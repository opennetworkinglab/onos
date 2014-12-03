package org.onosproject.core;

/**
 * Represents that there is no available IDs.
 */
public class UnavailableIdException extends RuntimeException {

    private static final long serialVersionUID = -2287403908433720122L;

    /**
     * Constructs an exception with no message and no underlying cause.
     */
    public UnavailableIdException() {
    }

    /**
     * Constructs an exception with the specified message.
     *
     * @param message the message describing the specific nature of the error
     */
    public UnavailableIdException(String message) {
        super(message);
    }

    /**
     * Constructs an exception with the specified message and the underlying cause.
     *
     * @param message the message describing the specific nature of the error
     * @param cause the underlying cause of this error
     */
    public UnavailableIdException(String message, Throwable cause) {
        super(message, cause);
    }
}
