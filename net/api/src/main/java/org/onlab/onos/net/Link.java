package org.onlab.onos.net;

import org.onlab.onos.net.provider.Provided;

/**
 * Abstraction of a network infrastructure link.
 */
public interface Link extends Provided { // TODO: Also should extend graph Edge

    /**
     * Returns the link source connection point.
     *
     * @return link source connection point
     */
    ConnectPoint src();

    /**
     * Returns the link destination connection point.
     *
     * @return link destination connection point
     */
    ConnectPoint dst();

}
