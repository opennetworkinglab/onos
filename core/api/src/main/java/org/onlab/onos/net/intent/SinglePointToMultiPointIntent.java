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

    private final ConnectPoint ingressPoint;
    private final Set<ConnectPoint> egressPoints;

    /**
     * Creates a new single-to-multi point connectivity intent.
     *
     * @param id           intent identifier
     * @param selector     traffic selector
     * @param treatment    treatment
     * @param ingressPoint port on which traffic will ingress
     * @param egressPoints set of ports on which traffic will egress
     * @throws NullPointerException     if {@code ingressPoint} or
     *                                  {@code egressPoints} is null
     * @throws IllegalArgumentException if the size of {@code egressPoints} is
     *                                  not more than 1
     */
    public SinglePointToMultiPointIntent(IntentId id, TrafficSelector selector,
                                         TrafficTreatment treatment,
                                         ConnectPoint ingressPoint,
                                         Set<ConnectPoint> egressPoints) {
        super(id, selector, treatment);

        checkNotNull(egressPoints);
        checkArgument(!egressPoints.isEmpty(),
                      "there should be at least one egress port");

        this.ingressPoint = checkNotNull(ingressPoint);
        this.egressPoints = Sets.newHashSet(egressPoints);
    }

    /**
     * Constructor for serializer.
     */
    protected SinglePointToMultiPointIntent() {
        super();
        this.ingressPoint = null;
        this.egressPoints = null;
    }

    /**
     * Returns the port on which the ingress traffic should be connected to the egress.
     *
     * @return ingress port
     */
    public ConnectPoint ingressPoint() {
        return ingressPoint;
    }

    /**
     * Returns the set of ports on which the traffic should egress.
     *
     * @return set of egress ports
     */
    public Set<ConnectPoint> egressPoints() {
        return egressPoints;
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
        return Objects.equals(this.ingressPoint, that.ingressPoint)
                && Objects.equals(this.egressPoints, that.egressPoints);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), ingressPoint, egressPoints);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("id", id())
                .add("match", selector())
                .add("action", treatment())
                .add("ingressPoint", ingressPoint)
                .add("egressPort", egressPoints)
                .toString();
    }

}
