package org.onlab.onos.net.intent;

import com.google.common.base.MoreObjects;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.Path;
import org.onlab.onos.net.flow.TrafficSelector;
import org.onlab.onos.net.flow.TrafficTreatment;

import java.util.Collection;
import java.util.Objects;

/**
 * Abstraction of explicitly path specified connectivity intent.
 */
public class PathIntent extends PointToPointIntent implements InstallableIntent {

    private final Path path;

    /**
     * Creates a new point-to-point intent with the supplied ingress/egress
     * ports and using the specified explicit path.
     *
     * @param id          intent identifier
     * @param match       traffic match
     * @param action      action
     * @param ingressPort ingress port
     * @param egressPort  egress port
     * @param path        traversed links
     * @throws NullPointerException {@code path} is null
     */
    public PathIntent(IntentId id, TrafficSelector match, TrafficTreatment action,
                      ConnectPoint ingressPort, ConnectPoint egressPort,
                      Path path) {
        super(id, match, action, ingressPort, egressPort);
        this.path = path;
    }

    protected PathIntent() {
        super();
        this.path = null;
    }

    /**
     * Returns the links which the traffic goes along.
     *
     * @return traversed links
     */
    public Path getPath() {
        return path;
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

        PathIntent that = (PathIntent) o;

        if (!path.equals(that.path)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), path);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("id", getId())
                .add("match", getTrafficSelector())
                .add("action", getTrafficTreatment())
                .add("ingressPort", getIngressPort())
                .add("egressPort", getEgressPort())
                .add("path", path)
                .toString();
    }

    @Override
    public Collection<Link> requiredLinks() {
        return path.links();
    }

}
