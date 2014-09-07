package org.onlab.onos.net.link;

import org.onlab.onos.net.ConnectPoint;

/**
 * Describes an infrastructure link.
 */
public interface LinkDescription {

    /**
     * Returns the link source.
     *
     * @return links source
     */
    ConnectPoint src();

    /**
     * Returns the link destination.
     *
     * @return links destination
     */
    ConnectPoint dst();

    // Add further link attributes
}
