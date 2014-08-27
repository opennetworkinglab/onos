package org.onlab.onos.net;

import org.onlab.onos.net.provider.Provided;

/**
 * Base abstraction of a network element, i.e. an infrastructure device or an end-station host.
 */
public interface Element extends Provided {

    /**
     * Returns the network element identifier.
     *
     * @return element identifier
     */
    ElementId id();

}
