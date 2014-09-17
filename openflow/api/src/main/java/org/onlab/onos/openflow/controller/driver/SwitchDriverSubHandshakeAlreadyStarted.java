package org.onlab.onos.openflow.controller.driver;

/**
 * Thrown when IOFSwitch.startDriverHandshake() is called more than once.
 *
 */
public class SwitchDriverSubHandshakeAlreadyStarted extends
    SwitchDriverSubHandshakeException {
    private static final long serialVersionUID = -5491845708752443501L;

    public SwitchDriverSubHandshakeAlreadyStarted() {
        super();
    }
}
