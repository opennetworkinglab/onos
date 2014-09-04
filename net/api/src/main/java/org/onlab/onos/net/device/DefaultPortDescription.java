package org.onlab.onos.net.device;

import org.onlab.onos.net.PortNumber;

/**
 * Default implementation of immutable port description.
 */
public class DefaultPortDescription implements PortDescription {

    private final PortNumber number;
    private final boolean isEnabled;

    public DefaultPortDescription(PortNumber number, boolean isEnabled) {
        this.number = number;
        this.isEnabled = isEnabled;
    }

    @Override
    public PortNumber portNumber() {
        return number;
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

}
