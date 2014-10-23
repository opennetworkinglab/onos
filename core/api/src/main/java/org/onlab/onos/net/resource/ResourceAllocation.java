package org.onlab.onos.net.resource;

/**
 * Abstraction of allocated resource.
 */
public interface ResourceAllocation extends ResourceRequest {

    /**
     * Returns the type of the allocated resource.
     *
     * @return the type of the allocated resource
     */
    ResourceType type();
}
