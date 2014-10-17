package org.onlab.onos.net.intent;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

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

    /**
     * Creates a new point-to-point intent with the supplied ingress/egress
     * ports and using the specified explicit path.
     *
     * @param id          intent identifier
     * @param selector    traffic match
     * @param treatment   action
     * @param links       traversed links
     * @throws NullPointerException {@code path} is null
     */
    public LinkCollectionIntent(IntentId id,
                                TrafficSelector selector,
                                TrafficTreatment treatment,
                                Set<Link> links) {
        super(id, selector, treatment);
        this.links = links;
    }

    protected LinkCollectionIntent() {
        super();
        this.links = null;
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

        return Objects.equals(this.links, that.links);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), links);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("id", id())
                .add("match", selector())
                .add("action", treatment())
                .add("links", links())
                .toString();
    }
}
