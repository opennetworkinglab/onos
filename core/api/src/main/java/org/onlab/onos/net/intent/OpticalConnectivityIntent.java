package org.onlab.onos.net.intent;

import org.onlab.onos.net.ConnectPoint;

// TODO: consider if this intent should be sub-class of ConnectivityIntent
/**
 * An optical layer Intent for a connectivity from a transponder port to another
 * transponder port.
 * <p>
 * This class doesn't accepts lambda specifier. This class computes path between
 * ports and assign lambda automatically. The lambda can be specified using
 * OpticalPathFlow class.
 */
public class OpticalConnectivityIntent extends AbstractIntent {
    protected ConnectPoint srcConnectPoint;
    protected ConnectPoint dstConnectPoint;

    /**
     * Constructor.
     *
     * @param id ID for this new Intent object.
     * @param srcConnectPoint The source transponder port.
     * @param dstConnectPoint The destination transponder port.
     */
    public OpticalConnectivityIntent(IntentId id,
            ConnectPoint srcConnectPoint, ConnectPoint dstConnectPoint) {
        super(id);
        this.srcConnectPoint = srcConnectPoint;
        this.dstConnectPoint = dstConnectPoint;
    }

    /**
     * Constructor for serializer.
     */
    protected OpticalConnectivityIntent() {
        super();
        this.srcConnectPoint = null;
        this.dstConnectPoint = null;
    }

    /**
     * Gets source transponder port.
     *
     * @return The source transponder port.
     */
    public ConnectPoint getSrcConnectPoint() {
        return srcConnectPoint;
    }

    /**
     * Gets destination transponder port.
     *
     * @return The source transponder port.
     */
    public ConnectPoint getDstConnectPoint() {
        return dstConnectPoint;
    }
}
