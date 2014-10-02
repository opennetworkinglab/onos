package org.onlab.onos.net.intent;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.flow.TrafficSelector;

// TODO: consider if this intent should be sub-class of Connectivity intent
/**
 * A packet layer Intent for a connectivity from a set of ports to a set of
 * ports.
 * <p>
 * TODO: Design methods to support the ReactiveForwarding and the SDN-IP. <br>
 * NOTE: Should this class support modifier methods? Should this object a
 * read-only object?
 */
public class PacketConnectivityIntent extends AbstractIntent {
    protected Set<ConnectPoint> srcConnectPoints;
    protected TrafficSelector selector;
    protected Set<ConnectPoint> dstConnectPoints;
    protected boolean canSetupOpticalFlow;
    protected int idleTimeoutValue;
    protected int hardTimeoutValue;

    /**
     * Creates a connectivity intent for the packet layer.
     * <p>
     * When the "canSetupOpticalFlow" option is true, this intent will compute
     * the packet/optical converged path, decompose it to the OpticalPathFlow
     * and the PacketPathFlow objects, and execute the operations to add them
     * considering the dependency between the packet and optical layers.
     *
     * @param id ID for this new Intent object.
     * @param srcConnectPoints The set of source switch ports.
     * @param match Traffic specifier for this object.
     * @param dstConnectPoints The set of destination switch ports.
     * @param canSetupOpticalFlow The flag whether this intent can create
     *        optical flows if needed.
     */
    public PacketConnectivityIntent(IntentId id,
            Collection<ConnectPoint> srcConnectPoints, TrafficSelector match,
            Collection<ConnectPoint> dstConnectPoints, boolean canSetupOpticalFlow) {
        super(id);
        this.srcConnectPoints = new HashSet<ConnectPoint>(srcConnectPoints);
        this.selector = match;
        this.dstConnectPoints = new HashSet<ConnectPoint>(dstConnectPoints);
        this.canSetupOpticalFlow = canSetupOpticalFlow;
        this.idleTimeoutValue = 0;
        this.hardTimeoutValue = 0;

        // TODO: check consistency between these parameters.
    }

    /**
     * Constructor for serializer.
     */
    protected PacketConnectivityIntent() {
        super();
        this.srcConnectPoints = null;
        this.selector = null;
        this.dstConnectPoints = null;
        this.canSetupOpticalFlow = false;
        this.idleTimeoutValue = 0;
        this.hardTimeoutValue = 0;
    }

    /**
     * Gets the set of source switch ports.
     *
     * @return the set of source switch ports.
     */
    public Collection<ConnectPoint> getSrcConnectPoints() {
        return Collections.unmodifiableCollection(srcConnectPoints);
    }

    /**
     * Gets the traffic specifier.
     *
     * @return The traffic specifier.
     */
    public TrafficSelector getMatch() {
        return selector;
    }

    /**
     * Gets the set of destination switch ports.
     *
     * @return the set of destination switch ports.
     */
    public Collection<ConnectPoint> getDstConnectPoints() {
        return Collections.unmodifiableCollection(dstConnectPoints);
    }

    /**
     * Adds the specified port to the set of source ports.
     *
     * @param port ConnectPoint object to be added
     */
    public void addSrcConnectPoint(ConnectPoint port) {
        // TODO implement it.
    }

    /**
     * Adds the specified port to the set of destination ports.
     *
     * @param port ConnectPoint object to be added
     */
    public void addDstConnectPoint(ConnectPoint port) {
        // TODO implement it.
    }

    /**
     * Removes the specified port from the set of source ports.
     *
     * @param port ConnectPoint object to be removed
     */
    public void removeSrcConnectPoint(ConnectPoint port) {
        // TODO implement it.
    }

    /**
     * Removes the specified port from the set of destination ports.
     *
     * @param port ConnectPoint object to be removed
     */
    public void removeDstConnectPoint(ConnectPoint port) {
        // TODO implement it.
    }

    /**
     * Sets idle-timeout value.
     *
     * @param timeout Idle-timeout value (seconds)
     */
    public void setIdleTimeout(int timeout) {
        idleTimeoutValue = timeout;
    }

    /**
     * Sets hard-timeout value.
     *
     * @param timeout Hard-timeout value (seconds)
     */
    public void setHardTimeout(int timeout) {
        hardTimeoutValue = timeout;
    }

    /**
     * Gets idle-timeout value.
     *
     * @return Idle-timeout value (seconds)
     */
    public int getIdleTimeout() {
        return idleTimeoutValue;
    }

    /**
     * Gets hard-timeout value.
     *
     * @return Hard-timeout value (seconds)
     */
    public int getHardTimeout() {
        return hardTimeoutValue;
    }

    /**
     * Returns whether this intent can create optical flows if needed.
     *
     * @return whether this intent can create optical flows.
     */
    public boolean canSetupOpticalFlow() {
        return canSetupOpticalFlow;
    }
}
