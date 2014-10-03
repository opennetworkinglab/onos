package org.onlab.onos.net.intent;

import com.google.common.base.MoreObjects;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.flow.TrafficSelector;
import org.onlab.onos.net.flow.TrafficTreatment;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Abstraction of point-to-point connectivity.
 */
public class PointToPointIntent extends ConnectivityIntent {

    private final ConnectPoint ingressPort;
    private final ConnectPoint egressPort;

    /**
     * Creates a new point-to-point intent with the supplied ingress/egress
     * ports.
     *
     * @param id          intent identifier
     * @param selector    traffic selector
     * @param treatment   treatment
     * @param ingressPort ingress port
     * @param egressPort  egress port
     * @throws NullPointerException if {@code ingressPort} or {@code egressPort} is null.
     */
    public PointToPointIntent(IntentId id, TrafficSelector selector,
                              TrafficTreatment treatment,
                              ConnectPoint ingressPort,
                              ConnectPoint egressPort) {
        super(id, selector, treatment);
        this.ingressPort = checkNotNull(ingressPort);
        this.egressPort = checkNotNull(egressPort);
    }

    /**
     * Constructor for serializer.
     */
    protected PointToPointIntent() {
        super();
        this.ingressPort = null;
        this.egressPort = null;
    }

    /**
     * Returns the port on which the ingress traffic should be connected to
     * the egress.
     *
     * @return ingress port
     */
    public ConnectPoint getIngressPort() {
        return ingressPort;
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

        PointToPointIntent that = (PointToPointIntent) o;
        return Objects.equals(this.ingressPort, that.ingressPort)
                && Objects.equals(this.egressPort, that.egressPort);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), ingressPort, egressPort);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("id", getId())
                .add("match", getTrafficSelector())
                .add("action", getTrafficTreatment())
                .add("ingressPort", ingressPort)
                .add("egressPort", egressPort)
                .toString();
    }

}
