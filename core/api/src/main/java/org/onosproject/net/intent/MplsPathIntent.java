package org.onosproject.net.intent;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collections;
import java.util.List;
import java.util.Optional;


import org.onlab.packet.MplsLabel;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.Path;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;


/**
 * Abstraction of explicit MPLS label-switched path.
 */

public final class MplsPathIntent extends PathIntent {

    private final Optional<MplsLabel> ingressLabel;
    private final Optional<MplsLabel> egressLabel;

    /**
     * Creates a new point-to-point intent with the supplied ingress/egress
     * ports and using the specified explicit path.
     *
     * @param appId application identifier
     * @param selector traffic selector
     * @param treatment treatment
     * @param path traversed links
     * @param ingressLabel MPLS egress label
     * @param egressLabel MPLS ingress label
     * @throws NullPointerException {@code path} is null
     */
    public MplsPathIntent(ApplicationId appId, TrafficSelector selector,
            TrafficTreatment treatment, Path path, Optional<MplsLabel> ingressLabel,
            Optional<MplsLabel> egressLabel) {
        this(appId, selector, treatment, path, ingressLabel, egressLabel,
             Collections.emptyList());

    }

    /**
     * Creates a new point-to-point intent with the supplied ingress/egress
     * ports and using the specified explicit path.
     *
     * @param appId application identifier
     * @param selector traffic selector
     * @param treatment treatment
     * @param path traversed links
     * @param ingressLabel MPLS egress label
     * @param egressLabel MPLS ingress label
     * @param constraints optional list of constraints
     * @throws NullPointerException {@code path} is null
     */
    public MplsPathIntent(ApplicationId appId, TrafficSelector selector,
            TrafficTreatment treatment, Path path, Optional<MplsLabel> ingressLabel,
            Optional<MplsLabel> egressLabel, List<Constraint> constraints) {
        super(appId, selector, treatment, path, constraints,
                DEFAULT_INTENT_PRIORITY);

        checkNotNull(ingressLabel);
        checkNotNull(egressLabel);
        this.ingressLabel = ingressLabel;
        this.egressLabel = egressLabel;
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

}
