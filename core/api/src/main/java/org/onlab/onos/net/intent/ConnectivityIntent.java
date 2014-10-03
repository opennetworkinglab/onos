package org.onlab.onos.net.intent;

import com.google.common.base.Objects;
import org.onlab.onos.net.flow.TrafficSelector;
import org.onlab.onos.net.flow.TrafficTreatment;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Abstraction of connectivity intent for traffic matching some criteria.
 */
public abstract class ConnectivityIntent extends AbstractIntent {

    // TODO: other forms of intents should be considered for this family:
    //   point-to-point with constraints (waypoints/obstacles)
    //   multi-to-single point with constraints (waypoints/obstacles)
    //   single-to-multi point with constraints (waypoints/obstacles)
    //   concrete path (with alternate)
    //   ...

    private final TrafficSelector selector;
    // TODO: should consider which is better for multiple actions,
    // defining compound action class or using list of actions.
    private final TrafficTreatment treatment;

    /**
     * Creates a connectivity intent that matches on the specified intent
     * and applies the specified treatement.
     *
     * @param intentId   intent identifier
     * @param selector   traffic selector
     * @param treatement treatement
     * @throws NullPointerException if the selector or treatement is null
     */
    protected ConnectivityIntent(IntentId intentId, TrafficSelector selector,
                                 TrafficTreatment treatement) {
        super(intentId);
        this.selector = checkNotNull(selector);
        this.treatment = checkNotNull(treatement);
    }

    /**
     * Constructor for serializer.
     */
    protected ConnectivityIntent() {
        super();
        this.selector = null;
        this.treatment = null;
    }

    /**
     * Returns the match specifying the type of traffic.
     *
     * @return traffic match
     */
    public TrafficSelector getTrafficSelector() {
        return selector;
    }

    /**
     * Returns the action applied to the traffic.
     *
     * @return applied action
     */
    public TrafficTreatment getTrafficTreatment() {
        return treatment;
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) {
            return false;
        }
        ConnectivityIntent that = (ConnectivityIntent) o;
        return Objects.equal(this.selector, that.selector)
                && Objects.equal(this.treatment, that.treatment);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), selector, treatment);
    }

}
