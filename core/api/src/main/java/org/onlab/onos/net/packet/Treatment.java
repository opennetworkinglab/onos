package org.onlab.onos.net.packet;

import org.onlab.onos.net.PortNumber;

/**
 * Abstraction of different kinds of treatment that can be applied to an
 * outbound packet.
 */
public interface Treatment {

    // TODO: implement these later: modifications, group
    // TODO: elsewhere provide factory methods for some default treatments

    /**
     * Returns the port number where the packet should be emitted.
     *
     * @return output port number
     */
    PortNumber output();

}
