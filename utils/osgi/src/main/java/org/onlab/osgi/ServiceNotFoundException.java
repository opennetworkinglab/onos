package org.onlab.osgi;

/**
 * Represents condition where some service is not found or not available.
 */
public class ServiceNotFoundException extends RuntimeException {

    /**
     * Creates a new exception with no message.
     */
    public ServiceNotFoundException() {
    }

    /**
     * Creates a new exception with the supplied message.
     * @param message error message
     */
    public ServiceNotFoundException(String message) {
        super(message);
    }

    /**
     * Creates a new exception with the supplied message and cause.
     * @param message error message
     * @param cause cause of the error
     */
    public ServiceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

}
