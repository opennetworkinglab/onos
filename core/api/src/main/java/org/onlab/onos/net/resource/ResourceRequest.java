package org.onlab.onos.net.resource;

/**
 * Abstraction of resource request.
 */
public interface ResourceRequest {
    /**
     * Returns the resource type.
     *
     * @return the resource type
     */
    ResourceType type();

}
