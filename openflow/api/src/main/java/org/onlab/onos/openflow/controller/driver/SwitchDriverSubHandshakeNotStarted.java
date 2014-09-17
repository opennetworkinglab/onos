package org.onlab.onos.openflow.controller.driver;

/**
 * Thrown when a switch driver's sub-handshake has not been started but an
 * operation requiring the sub-handshake has been attempted.
 *
 */
public class SwitchDriverSubHandshakeNotStarted extends
    SwitchDriverSubHandshakeException {
    private static final long serialVersionUID = -5491845708752443501L;

    public SwitchDriverSubHandshakeNotStarted() {
        super();
    }
}
