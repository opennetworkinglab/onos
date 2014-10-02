package org.onlab.onos.net.intent;

/**
 * Represents an intent related error.
 */
public class IntentException extends RuntimeException {

    private static final long serialVersionUID = 1907263634145241319L;

    /**
     * Constructs an exception with no message and no underlying cause.
     */
    public IntentException() {
    }

    /**
     * Constructs an exception with the specified message.
     *
     * @param message the message describing the specific nature of the error
     */
    public IntentException(String message) {
        super(message);
    }

    /**
     * Constructs an exception with the specified message and the underlying cause.
     *
     * @param message the message describing the specific nature of the error
     * @param cause the underlying cause of this error
     */
    public IntentException(String message, Throwable cause) {
        super(message, cause);
    }

}
