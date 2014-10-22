package org.onlab.onos.net.intent;

import com.google.common.base.MoreObjects;
import org.onlab.onos.ApplicationId;
import org.onlab.onos.net.HostId;
import org.onlab.onos.net.flow.TrafficSelector;
import org.onlab.onos.net.flow.TrafficTreatment;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Abstraction of end-station to end-station bidirectional connectivity.
 */
public final class HostToHostIntent extends ConnectivityIntent {

    private final HostId one;
    private final HostId two;

    /**
     * Creates a new host-to-host intent with the supplied host pair.
     *
     * @param appId     application identifier
     * @param one       first host
     * @param two       second host
     * @param selector  action
     * @param treatment ingress port
     * @throws NullPointerException if {@code one} or {@code two} is null.
     */
    public HostToHostIntent(ApplicationId appId, HostId one, HostId two,
                            TrafficSelector selector,
                            TrafficTreatment treatment) {
        super(id(HostToHostIntent.class, min(one, two), max(one, two),
                 selector, treatment),
              appId, null, selector, treatment);
        this.one = checkNotNull(one);
        this.two = checkNotNull(two);
    }

    private static HostId min(HostId one, HostId two) {
        return one.hashCode() < two.hashCode() ? one : two;
    }

    private static HostId max(HostId one, HostId two) {
        return one.hashCode() >= two.hashCode() ? one : two;
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
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("id", id())
                .add("appId", appId())
                .add("selector", selector())
                .add("treatment", treatment())
                .add("one", one)
                .add("two", two)
                .toString();
    }

}
