package org.onlab.onos.net.link;

import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.Link;

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

    /**
     * Returns the link type.
     *
     * @return link type
     */
    Link.Type type();


    // Add further link attributes
}
