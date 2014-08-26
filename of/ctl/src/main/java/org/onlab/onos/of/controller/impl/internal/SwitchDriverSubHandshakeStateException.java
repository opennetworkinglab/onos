package org.onlab.onos.of.controller.impl.internal;

/**
 * Thrown when a switch driver's sub-handshake state-machine receives an
 * unexpected OFMessage and/or is in an invald state.
 *
 */
public class SwitchDriverSubHandshakeStateException extends
    SwitchDriverSubHandshakeException {
    private static final long serialVersionUID = -8249926069195147051L;

    public SwitchDriverSubHandshakeStateException(String msg) {
        super(msg);
    }
}
