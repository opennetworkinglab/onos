package org.onlab.onos.net.intent;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

import org.onlab.onos.net.HostId;
import org.onlab.onos.net.flow.TrafficSelector;
import org.onlab.onos.net.flow.TrafficTreatment;

import com.google.common.base.MoreObjects;

/**
 * Abstraction of point-to-point connectivity.
 */
public class HostToHostIntent extends ConnectivityIntent {

    private final HostId src;
    private final HostId dst;

    /**
     * Creates a new point-to-point intent with the supplied ingress/egress
     * ports.
     *
     * @param id intent identifier
     * @param match traffic match
     * @param action action
     * @param ingressPort ingress port
     * @param egressPort egress port
     * @throws NullPointerException if {@code ingressPort} or {@code egressPort}
     *         is null.
     */
    public HostToHostIntent(IntentId id, HostId src, HostId dst,
            TrafficSelector selector, TrafficTreatment treatment) {
        super(id, selector, treatment);
        this.src = checkNotNull(src);
        this.dst = checkNotNull(dst);
    }

    /**
     * Returns the port on which the ingress traffic should be connected to the
     * egress.
     *
     * @return ingress port
     */
    public HostId getSrc() {
        return src;
    }

    /**
     * Returns the port on which the traffic should egress.
     *
     * @return egress port
     */
    public HostId getDst() {
        return dst;
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
        return Objects.equals(this.src, that.src)
                && Objects.equals(this.dst, that.dst);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), src, dst);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("id", getId())
                .add("selector", getTrafficSelector())
                .add("treatmetn", getTrafficTreatment())
                .add("src", src)
                .add("dst", dst)
                .toString();
    }

}
