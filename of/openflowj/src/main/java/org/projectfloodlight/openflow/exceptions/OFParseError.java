package org.projectfloodlight.openflow.exceptions;

public class OFParseError extends Exception {
    private static final long serialVersionUID = 1L;

    public OFParseError() {
        super();
    }

    public OFParseError(final String message, final Throwable cause) {
        super(message, cause);
    }

    public OFParseError(final String message) {
        super(message);
    }

    public OFParseError(final Throwable cause) {
        super(cause);
    }

}
