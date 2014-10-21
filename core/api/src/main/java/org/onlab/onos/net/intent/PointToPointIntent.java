package org.onlab.onos.net.intent;

import com.google.common.base.MoreObjects;
import org.onlab.onos.ApplicationId;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.flow.TrafficSelector;
import org.onlab.onos.net.flow.TrafficTreatment;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Abstraction of point-to-point connectivity.
 */
public class PointToPointIntent extends ConnectivityIntent {

    private final ConnectPoint ingressPoint;
    private final ConnectPoint egressPoint;

    /**
     * Creates a new point-to-point intent with the supplied ingress/egress
     * ports.
     *
     * @param appId        application identifier
     * @param selector     traffic selector
     * @param treatment    treatment
     * @param ingressPoint ingress port
     * @param egressPoint  egress port
     * @throws NullPointerException if {@code ingressPoint} or {@code egressPoints} is null.
     */
    public PointToPointIntent(ApplicationId appId, TrafficSelector selector,
                              TrafficTreatment treatment,
                              ConnectPoint ingressPoint,
                              ConnectPoint egressPoint) {
        super(id(PointToPointIntent.class, selector, treatment, ingressPoint, egressPoint),
              appId, null, selector, treatment);
        this.ingressPoint = checkNotNull(ingressPoint);
        this.egressPoint = checkNotNull(egressPoint);
    }

    /**
     * Constructor for serializer.
     */
    protected PointToPointIntent() {
        super();
        this.ingressPoint = null;
        this.egressPoint = null;
    }

    /**
     * Returns the port on which the ingress traffic should be connected to
     * the egress.
     *
     * @return ingress port
     */
    public ConnectPoint ingressPoint() {
        return ingressPoint;
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
                .add("ingress", ingressPoint)
                .add("egress", egressPoint)
                .toString();
    }

}
