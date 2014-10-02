package org.onlab.onos.net;

/**
 * Base abstraction of a network element, i.e. an infrastructure device or an end-station host.
 */
public interface Element extends Annotated, Provided {

    /**
     * Returns the network element identifier.
     *
     * @return element identifier
     */
    ElementId id();

}
