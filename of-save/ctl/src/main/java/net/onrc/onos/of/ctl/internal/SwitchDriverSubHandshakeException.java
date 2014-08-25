package net.onrc.onos.of.ctl.internal;

/**
 * Base class for exception thrown by switch driver sub-handshake processing.
 *
 */
public class SwitchDriverSubHandshakeException extends RuntimeException {
    private static final long serialVersionUID = -6257836781419604438L;

    protected SwitchDriverSubHandshakeException() {
        super();
    }

    protected SwitchDriverSubHandshakeException(String arg0, Throwable arg1) {
        super(arg0, arg1);
    }

    protected SwitchDriverSubHandshakeException(String arg0) {
        super(arg0);
    }

    protected SwitchDriverSubHandshakeException(Throwable arg0) {
        super(arg0);
    }

}
