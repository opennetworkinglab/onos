package org.onlab.onos.net.intent;

import com.google.common.base.MoreObjects;
import org.onlab.onos.net.HostId;
import org.onlab.onos.net.flow.TrafficSelector;
import org.onlab.onos.net.flow.TrafficTreatment;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Abstraction of end-station to end-station bidirectional connectivity.
 */
public final class HostToHostIntent extends ConnectivityIntent {

    private final HostId one;
    private final HostId two;

    /**
     * Creates a new point-to-point intent with the supplied ingress/egress
     * ports.
     *
     * @param intentId  intent identifier
     * @param one       first host
     * @param two       second host
     * @param selector  action
     * @param treatment ingress port
     * @throws NullPointerException if {@code ingressPort} or {@code egressPort}
     *                              is null.
     */
    public HostToHostIntent(IntentId intentId, HostId one, HostId two,
                            TrafficSelector selector,
                            TrafficTreatment treatment) {
        super(intentId, selector, treatment);
        this.one = checkNotNull(one);
        this.two = checkNotNull(two);
    }

    /**
     * Returns identifier of the first host.
     *
     * @return first host identifier
     */
    public HostId one() {
        return one;
    }

    /**
     * Returns identifier of the second host.
     *
     * @return second host identifier
     */
    public HostId two() {
        return two;
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

        HostToHostIntent that = (HostToHostIntent) o;
        return Objects.equals(this.one, that.one)
                && Objects.equals(this.two, that.two);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), one, two);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("id", id())
                .add("selector", selector())
                .add("treatment", treatment())
                .add("one", one)
                .add("two", two)
                .toString();
    }

}
