package org.onosproject.ovsdb.rfc.error;

/**
 * This exception is thrown when the encoding does not meet UTF-8 in RFC7047.
 */
public class UnsupportedEncodingException extends RuntimeException {
    private static final long serialVersionUID = -4865311369828520666L;

    /**
     * Constructs a UnsupportedEncodingException object.
     * @param message error message
     */
    public UnsupportedEncodingException(String message) {
        super(message);
    }
}
