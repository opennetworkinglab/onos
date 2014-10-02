package org.onlab.onos.net.intent;

import static com.google.common.base.Preconditions.checkNotNull;

import org.onlab.onos.net.flow.TrafficSelector;
import org.onlab.onos.net.flow.TrafficTreatment;

import com.google.common.base.Objects;

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
     * and applies the specified action.
     *
     * @param id    intent identifier
     * @param match traffic match
     * @param action action
     * @throws NullPointerException if the match or action is null
     */
    protected ConnectivityIntent(IntentId id, TrafficSelector match, TrafficTreatment action) {
        super(id);
        this.selector = checkNotNull(match);
        this.treatment = checkNotNull(action);
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
