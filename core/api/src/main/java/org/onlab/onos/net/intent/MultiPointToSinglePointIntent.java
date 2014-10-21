package org.onlab.onos.net.intent;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Sets;
import org.onlab.onos.ApplicationId;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.flow.TrafficSelector;
import org.onlab.onos.net.flow.TrafficTreatment;

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
     * traffic selector and treatment.
     *
     * @param appId         application identifier
     * @param selector      traffic selector
     * @param treatment     treatment
     * @param ingressPoints set of ports from which ingress traffic originates
     * @param egressPoint   port to which traffic will egress
     * @throws NullPointerException     if {@code ingressPoints} or
     *                                  {@code egressPoint} is null.
     * @throws IllegalArgumentException if the size of {@code ingressPoints} is
     *                                  not more than 1
     */
    public MultiPointToSinglePointIntent(ApplicationId appId,
                                         TrafficSelector selector,
                                         TrafficTreatment treatment,
                                         Set<ConnectPoint> ingressPoints,
                                         ConnectPoint egressPoint) {
        super(id(MultiPointToSinglePointIntent.class, selector, treatment,
                 ingressPoints, egressPoint), appId, null, selector, treatment);

        checkNotNull(ingressPoints);
        checkArgument(!ingressPoints.isEmpty(), "Ingress point set cannot be empty");

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
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("id", id())
                .add("appId", appId())
                .add("selector", selector())
                .add("treatment", treatment())
                .add("ingress", ingressPoints())
                .add("egress", egressPoint())
                .toString();
    }
}
