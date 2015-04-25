package org.onosproject.ovsdb.lib.error;

public class InvalidEncodingException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private final String actual;

    public InvalidEncodingException(String actual, String message) {
        super(message);
        this.actual = actual;
    }

    public String getActual() {
        return actual;
    }
}
