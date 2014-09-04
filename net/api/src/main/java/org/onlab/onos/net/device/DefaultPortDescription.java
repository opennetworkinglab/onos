package org.onlab.onos.net.device;

import com.google.common.collect.ImmutableSet;
import org.onlab.onos.net.Port;
import org.onlab.onos.net.PortNumber;

import java.util.Set;

/**
 * Default implementation of immutable port description.
 */
public class DefaultPortDescription implements PortDescription {

    private final PortNumber number;
    private final Set<Port.State> state;

    public DefaultPortDescription(PortNumber number, Set<Port.State> state) {
        this.number = number;
        this.state = ImmutableSet.copyOf(state);
    }

    @Override
    public PortNumber portNumber() {
        return number;
    }

    @Override
    public Set<Port.State> portState() {
        return state;
    }

}
