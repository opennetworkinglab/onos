package org.onlab.onos.net.intent;

import com.google.common.base.MoreObjects;
import org.onlab.onos.net.HostId;
import org.onlab.onos.net.flow.TrafficSelector;
import org.onlab.onos.net.flow.TrafficTreatment;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Abstraction of end-station to end-station connectivity.
 */
public class HostToHostIntent extends ConnectivityIntent {

    private final HostId src;
    private final HostId dst;

    /**
     * Creates a new point-to-point intent with the supplied ingress/egress
     * ports.
     *
     * @param intentId  intent identifier
     * @param selector  action
     * @param treatment ingress port
     * @throws NullPointerException if {@code ingressPort} or {@code egressPort}
     *                              is null.
     */
    public HostToHostIntent(IntentId intentId, HostId src, HostId dst,
                            TrafficSelector selector, TrafficTreatment treatment) {
        super(intentId, selector, treatment);
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
