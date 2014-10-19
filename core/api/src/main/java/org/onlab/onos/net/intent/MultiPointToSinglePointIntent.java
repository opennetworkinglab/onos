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
 * Abstraction of multiple source to single destination connectivity intent.
 */
public final class MultiPointToSinglePointIntent extends ConnectivityIntent {

    private final Set<ConnectPoint> ingressPoints;
    private final ConnectPoint egressPoint;

    /**
     * Creates a new multi-to-single point connectivity intent for the specified
     * traffic match and action.
     *
     * @param id           intent identifier
     * @param match        traffic match
     * @param action       action
     * @param ingressPoints set of ports from which ingress traffic originates
     * @param egressPoint   port to which traffic will egress
     * @throws NullPointerException     if {@code ingressPoints} or
     *                                  {@code egressPoint} is null.
     * @throws IllegalArgumentException if the size of {@code ingressPoints} is
     *                                  not more than 1
     */
    public MultiPointToSinglePointIntent(IntentId id, TrafficSelector match,
                                         TrafficTreatment action,
                                         Set<ConnectPoint> ingressPoints,
                                         ConnectPoint egressPoint) {
        super(id, match, action);

        checkNotNull(ingressPoints);
        checkArgument(!ingressPoints.isEmpty(),
                      "there should be at least one ingress port");

        this.ingressPoints = Sets.newHashSet(ingressPoints);
        this.egressPoint = checkNotNull(egressPoint);
    }

    /**
     * Constructor for serializer.
     */
    protected MultiPointToSinglePointIntent() {
        super();
        this.ingressPoints = null;
        this.egressPoint = null;
    }

    /**
     * Returns the set of ports on which ingress traffic should be connected to
     * the egress port.
     *
     * @return set of ingress ports
     */
    public Set<ConnectPoint> ingressPoints() {
        return ingressPoints;
    }

    /**
     * Returns the port on which the traffic should egress.
     *
     * @return egress port
     */
    public ConnectPoint egressPoint() {
        return egressPoint;
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
        return Objects.equals(this.ingressPoints, that.ingressPoints)
                && Objects.equals(this.egressPoint, that.egressPoint);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), ingressPoints, egressPoint);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("id", id())
                .add("match", selector())
                .add("action", treatment())
                .add("ingressPoints", ingressPoints())
                .add("egressPoint", egressPoint())
                .toString();
    }
}
