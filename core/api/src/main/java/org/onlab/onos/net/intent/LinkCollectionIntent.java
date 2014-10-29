/*
 * Copyright 2014 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onlab.onos.net.intent;

import com.google.common.base.MoreObjects;
import org.onlab.onos.core.ApplicationId;
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
     * Creates a new actionable intent capable of funneling the selected
     * traffic along the specified convergent tree and out the given egress point.
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
