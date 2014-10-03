package org.onlab.onos.net.intent;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Sets;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.flow.TrafficSelector;
import org.onlab.onos.net.flow.TrafficTreatment;

import java.util.Objects;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Abstraction of single source, multiple destination connectivity intent.
 */
public class SinglePointToMultiPointIntent extends ConnectivityIntent {

    private final ConnectPoint ingressPort;
    private final Set<ConnectPoint> egressPorts;

    /**
     * Creates a new single-to-multi point connectivity intent.
     *
     * @param id          intent identifier
     * @param selector    traffic selector
     * @param treatment   treatment
     * @param ingressPort port on which traffic will ingress
     * @param egressPorts set of ports on which traffic will egress
     * @throws NullPointerException     if {@code ingressPort} or
     *                                  {@code egressPorts} is null
     * @throws IllegalArgumentException if the size of {@code egressPorts} is
     *                                  not more than 1
     */
    public SinglePointToMultiPointIntent(IntentId id, TrafficSelector selector,
                                         TrafficTreatment treatment,
                                         ConnectPoint ingressPort,
                                         Set<ConnectPoint> egressPorts) {
        super(id, selector, treatment);

        checkNotNull(egressPorts);
        checkArgument(!egressPorts.isEmpty(),
                      "there should be at least one egress port");

        this.ingressPort = checkNotNull(ingressPort);
        this.egressPorts = Sets.newHashSet(egressPorts);
    }

    /**
     * Constructor for serializer.
     */
    protected SinglePointToMultiPointIntent() {
        super();
        this.ingressPort = null;
        this.egressPorts = null;
    }

    /**
     * Returns the port on which the ingress traffic should be connected to the egress.
     *
     * @return ingress port
     */
    public ConnectPoint getIngressPort() {
        return ingressPort;
    }

    /**
     * Returns the set of ports on which the traffic should egress.
     *
     * @return set of egress ports
     */
    public Set<ConnectPoint> getEgressPorts() {
        return egressPorts;
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

        SinglePointToMultiPointIntent that = (SinglePointToMultiPointIntent) o;
        return Objects.equals(this.ingressPort, that.ingressPort)
                && Objects.equals(this.egressPorts, that.egressPorts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), ingressPort, egressPorts);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("id", getId())
                .add("match", getTrafficSelector())
                .add("action", getTrafficTreatment())
                .add("ingressPort", ingressPort)
                .add("egressPort", egressPorts)
                .toString();
    }

}
