package org.onlab.onos.net.intent;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;
import java.util.Set;

import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.flow.TrafficSelector;
import org.onlab.onos.net.flow.TrafficTreatment;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Sets;

/**
 * Abstraction of multiple source to single destination connectivity intent.
 */
public class MultiPointToSinglePointIntent extends ConnectivityIntent {

    private final Set<ConnectPoint> ingressPorts;
    private final ConnectPoint egressPort;

    /**
     * Creates a new multi-to-single point connectivity intent for the specified
     * traffic match and action.
     *
     * @param id           intent identifier
     * @param match        traffic match
     * @param action       action
     * @param ingressPorts set of ports from which ingress traffic originates
     * @param egressPort   port to which traffic will egress
     * @throws NullPointerException if {@code ingressPorts} or
     * {@code egressPort} is null.
     * @throws IllegalArgumentException if the size of {@code ingressPorts} is
     * not more than 1
     */
    public MultiPointToSinglePointIntent(IntentId id, TrafficSelector match, TrafficTreatment action,
            Set<ConnectPoint> ingressPorts, ConnectPoint egressPort) {
        super(id, match, action);

        checkNotNull(ingressPorts);
        checkArgument(!ingressPorts.isEmpty(),
                "there should be at least one ingress port");

        this.ingressPorts = Sets.newHashSet(ingressPorts);
        this.egressPort = checkNotNull(egressPort);
    }

    /**
     * Constructor for serializer.
     */
    protected MultiPointToSinglePointIntent() {
        super();
        this.ingressPorts = null;
        this.egressPort = null;
    }

    /**
     * Returns the set of ports on which ingress traffic should be connected to
     * the egress port.
     *
     * @return set of ingress ports
     */
    public Set<ConnectPoint> getIngressPorts() {
        return ingressPorts;
    }

    /**
     * Returns the port on which the traffic should egress.
     *
     * @return egress port
     */
    public ConnectPoint getEgressPort() {
        return egressPort;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        MultiPointToSinglePointIntent that = (MultiPointToSinglePointIntent) o;
        return Objects.equals(this.ingressPorts, that.ingressPorts)
                && Objects.equals(this.egressPort, that.egressPort);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), ingressPorts, egressPort);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("id", getId())
                .add("match", getTrafficSelector())
                .add("action", getTrafficTreatment())
                .add("ingressPorts", getIngressPorts())
                .add("egressPort", getEgressPort())
                .toString();
    }
}
