package org.onlab.onos.net.intent;

import com.google.common.base.MoreObjects;
import org.onlab.onos.ApplicationId;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.flow.TrafficSelector;
import org.onlab.onos.net.flow.TrafficTreatment;

import java.util.Set;

/**
 * Abstraction of a connectivity intent that is implemented by a set of path
 * segments.
 */
public final class LinkCollectionIntent extends ConnectivityIntent {

    private final Set<Link> links;

    private final ConnectPoint egressPoint;

    /**
     * Creates a new point-to-point intent with the supplied ingress/egress
     * ports and using the specified explicit path.
     *
     * @param appId       application identifier
     * @param selector    traffic match
     * @param treatment   action
     * @param links       traversed links
     * @param egressPoint egress point
     * @throws NullPointerException {@code path} is null
     */
    public LinkCollectionIntent(ApplicationId appId,
                                TrafficSelector selector,
                                TrafficTreatment treatment,
                                Set<Link> links,
                                ConnectPoint egressPoint) {
        super(id(LinkCollectionIntent.class, selector, treatment, links, egressPoint),
              appId, resources(links), selector, treatment);
        this.links = links;
        this.egressPoint = egressPoint;
    }

    /**
     * Constructor for serializer.
     */
    protected LinkCollectionIntent() {
        super();
        this.links = null;
        this.egressPoint = null;
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
    public boolean isInstallable() {
        return true;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("id", id())
                .add("appId", appId())
                .add("selector", selector())
                .add("treatment", treatment())
                .add("links", links())
                .add("egress", egressPoint())
                .toString();
    }
}
