package org.onosproject.net.intent;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.onlab.packet.MplsLabel;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Link;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.intent.constraint.LinkTypeConstraint;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;


/**
 * Abstraction of MPLS label-switched connectivity.
 */
public final class MplsIntent extends ConnectivityIntent {

    private final ConnectPoint ingressPoint;
    private final Optional<MplsLabel> ingressLabel;
    private final ConnectPoint egressPoint;
    private final Optional<MplsLabel> egressLabel;

    /**
     * Creates a new MPLS intent with the supplied ingress/egress
     * ports and labels and with built-in link type constraint to avoid optical links.
     *
     * @param appId        application identifier
     * @param selector     traffic selector
     * @param treatment    treatment
     * @param ingressPoint ingress port
     * @param ingressLabel ingress MPLS label
     * @param egressPoint  egress port
     * @param egressLabel  egress MPLS label
     * @throws NullPointerException if {@code ingressPoint} or {@code egressPoints} is null.
     */
    public MplsIntent(ApplicationId appId, TrafficSelector selector,
                              TrafficTreatment treatment,
                              ConnectPoint ingressPoint,
                              Optional<MplsLabel> ingressLabel,
                              ConnectPoint egressPoint,
                              Optional<MplsLabel> egressLabel) {
        this(appId, selector, treatment, ingressPoint, ingressLabel, egressPoint, egressLabel,
             ImmutableList.of(new LinkTypeConstraint(false, Link.Type.OPTICAL)));
    }

    /**
     * Creates a new point-to-point intent with the supplied ingress/egress
     * ports, labels and constraints.
     *
     * @param appId        application identifier
     * @param selector     traffic selector
     * @param treatment    treatment
     * @param ingressPoint ingress port
     * @param ingressLabel ingress MPLS label
     * @param egressPoint  egress port
     * @param egressLabel  egress MPLS label
     * @param constraints  optional list of constraints
     * @throws NullPointerException if {@code ingressPoint} or {@code egressPoints} is null.
     */
    public MplsIntent(ApplicationId appId, TrafficSelector selector,
                              TrafficTreatment treatment,
                              ConnectPoint ingressPoint,
                              Optional<MplsLabel> ingressLabel,
                              ConnectPoint egressPoint,
                              Optional<MplsLabel> egressLabel,
                              List<Constraint> constraints) {

        super(appId, Collections.emptyList(), selector, treatment, constraints,
                DEFAULT_INTENT_PRIORITY);

        checkNotNull(ingressPoint);
        checkNotNull(egressPoint);
        checkArgument(!ingressPoint.equals(egressPoint),
                "ingress and egress should be different (ingress: %s, egress: %s)", ingressPoint, egressPoint);
        checkNotNull(ingressLabel);
        checkNotNull(egressLabel);
        this.ingressPoint = ingressPoint;
        this.ingressLabel = ingressLabel;
        this.egressPoint = egressPoint;
        this.egressLabel = egressLabel;

    }

    /**
     * Constructor for serializer.
     */
    protected MplsIntent() {
        super();
        this.ingressPoint = null;
        this.ingressLabel = null;
        this.egressPoint = null;
        this.egressLabel = null;

    }

    /**
     * Returns the port on which the ingress traffic should be connected to
     * the egress.
     *
     * @return ingress switch port
     */
    public ConnectPoint ingressPoint() {
        return ingressPoint;
    }

    /**
     * Returns the port on which the traffic should egress.
     *
     * @return egress switch port
     */
    public ConnectPoint egressPoint() {
        return egressPoint;
    }


    /**
     * Returns the MPLS label which the ingress traffic should tagged.
     *
     * @return ingress MPLS label
     */
    public Optional<MplsLabel> ingressLabel() {
        return ingressLabel;
    }

    /**
     * Returns the MPLS label which the egress traffic should tagged.
     *
     * @return egress MPLS label
     */
    public Optional<MplsLabel> egressLabel() {
        return egressLabel;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("id", id())
                .add("appId", appId())
                .add("priority", priority())
                .add("selector", selector())
                .add("treatment", treatment())
                .add("ingressPoint", ingressPoint)
                .add("ingressLabel", ingressLabel)
                .add("egressPoint", egressPoint)
                .add("egressLabel", egressLabel)
                .add("constraints", constraints())
                .toString();
    }



}
