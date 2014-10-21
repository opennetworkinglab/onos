package org.onlab.onos.net.resource;

/**
 * Representation of a request for bandwidth resource.
 */
public interface BandwidthResourceRequest {
    /**
     * Returns the bandwidth resource.
     *
     * @return the bandwidth resource
     */
    Bandwidth bandwidth();
}
