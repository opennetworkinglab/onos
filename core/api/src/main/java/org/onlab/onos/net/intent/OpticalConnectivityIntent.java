package org.onlab.onos.net.intent;

import org.onlab.onos.net.ConnectPoint;

/**
 * An optical layer Intent for a connectivity from a Transponder port to another
 * Transponder port. No trafficSelector as well as trafficTreament are needed.
 *
 */
public class OpticalConnectivityIntent extends AbstractIntent {
    protected ConnectPoint src;
    protected ConnectPoint dst;

    /**
     * Constructor.
     *
     * @param id  ID for this new Intent object.
     * @param src The source transponder port.
     * @param dst The destination transponder port.
     */
    public OpticalConnectivityIntent(IntentId id, ConnectPoint src, ConnectPoint dst) {
        super(id);
        this.src = src;
        this.dst = dst;
    }

    /**
     * Constructor for serializer.
     */
    protected OpticalConnectivityIntent() {
        super();
        this.src = null;
        this.dst = null;
    }

    /**
     * Gets source transponder port.
     *
     * @return The source transponder port.
     */
    public ConnectPoint getSrcConnectPoint() {
        return src;
    }

    /**
     * Gets destination transponder port.
     *
     * @return The source transponder port.
     */
    public ConnectPoint getDst() {
        return dst;
    }
}
