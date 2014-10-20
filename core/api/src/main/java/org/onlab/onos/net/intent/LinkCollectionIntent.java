package org.onlab.onos.net.intent;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.flow.TrafficSelector;
import org.onlab.onos.net.flow.TrafficTreatment;

import com.google.common.base.MoreObjects;

/**
 * Abstraction of a connectivity intent that is implemented by a set of path
 * segments.
 */
public final class LinkCollectionIntent extends ConnectivityIntent implements InstallableIntent {

    private final Set<Link> links;

    private final ConnectPoint egressPoint;

    /**
     * Creates a new point-to-point intent with the supplied ingress/egress
     * ports and using the specified explicit path.
     *
     * @param id          intent identifier
     * @param selector    traffic match
     * @param treatment   action
     * @param links       traversed links
     * @param egressPoint egress point
     * @throws NullPointerException {@code path} is null
     */
    public LinkCollectionIntent(IntentId id,
                                TrafficSelector selector,
                                TrafficTreatment treatment,
                                Set<Link> links,
                                ConnectPoint egressPoint) {
        super(id, selector, treatment);
        this.links = links;
        this.egressPoint = egressPoint;
    }

    protected LinkCollectionIntent() {
        super();
        this.links = null;
        this.egressPoint = null;
    }

    @Override
    public Collection<Link> requiredLinks() {
        return links;
    }

    /**
     * Returns the set of links that represent the network connections needed
     * by this intent.
     *
     * @return Set of links for the network hops needed by this intent
     */
    public Set<Link> links() {
        return links;
    }

    /**
     * Returns the egress point of the intent.
     *
     * @return the egress point
     */
    public ConnectPoint egressPoint() {
        return egressPoint;
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

        LinkCollectionIntent that = (LinkCollectionIntent) o;

        return Objects.equals(this.links, that.links) &&
                Objects.equals(this.egressPoint, that.egressPoint);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), links, egressPoint);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("id", id())
                .add("match", selector())
                .add("action", treatment())
                .add("links", links())
                .add("egress", egressPoint())
                .toString();
    }
}
