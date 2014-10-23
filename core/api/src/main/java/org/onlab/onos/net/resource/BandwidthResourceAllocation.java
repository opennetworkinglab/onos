package org.onlab.onos.net.resource;

/**
 * Representation of allocated bandwidth resource.
 */
public class BandwidthResourceAllocation extends BandwidthResourceRequest {
    /**
     * Creates a new {@link BandwidthResourceAllocation} with {@link Bandwidth}
     * object.
     *
     * @param bandwidth allocated bandwidth
     */
    public BandwidthResourceAllocation(Bandwidth bandwidth) {
        super(bandwidth);
    }
}
