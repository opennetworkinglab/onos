package org.onlab.onos.net.resource;

/**
 * Representation of a request for bandwidth resource.
 */
public class BandwidthResourceRequest implements ResourceRequest {
    private final Bandwidth bandwidth;

    /**
     * Creates a new {@link BandwidthResourceRequest} with {@link Bandwidth}
     * object.
     *
     * @param bandwidth {@link Bandwidth} object to be requested
     */
    public BandwidthResourceRequest(Bandwidth bandwidth) {
        this.bandwidth = bandwidth;
    }

    /**
     * Creates a new {@link BandwidthResourceRequest} with bandwidth value.
     *
     * @param bandwidth bandwidth value to be requested
     */
    public BandwidthResourceRequest(double bandwidth) {
        this.bandwidth = Bandwidth.valueOf(bandwidth);
    }

    /**
     * Returns the bandwidth resource.
     *
     * @return the bandwidth resource
     */
    public Bandwidth bandwidth() {
        return bandwidth;
    }
}
