/*
 * Copyright 2015 Open Networking Laboratory
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

package org.onosproject.incubator.net.domain;

import org.onosproject.core.ApplicationId;
import org.onosproject.incubator.net.tunnel.NetworkTunnelId;
import org.onosproject.net.ConnectPoint;

/**
 * A variant of intent resource specialized for use on the inter-domain level.  It contains a higher level path.
 */
public class NetworkIntentResource extends IntentResource {

    private final org.onlab.graph.Path<DomainVertex, DomainEdge> netPath;

    private NetworkTunnelId networkTunnelId;

    /**
     * Constructor for a network intent resource.
     *
     * @param primitive      the primitive associated with this resource
     * @param networkTunnelId the id of this tunnel (used as a sorting mechanism)
     * @param appId          the id of the application which created this tunnel
     * @param ingress        the fist connect point associated with this tunnel (order is irrelevant as long as it is
     *                       consistent with the path)
     * @param egress         the second connect point associated with this tunnel (order is irrelevant as long as it is
     *                       consistent with the path)
     * @param path           the path followed through the graph of domain vertices and domain edges
     */
    public NetworkIntentResource(IntentPrimitive primitive, NetworkTunnelId networkTunnelId, ApplicationId appId,
                                 ConnectPoint ingress, ConnectPoint egress,
                                 org.onlab.graph.Path<DomainVertex, DomainEdge> path) {
        super(primitive, appId, ingress, egress);

        this.networkTunnelId = networkTunnelId;
        this.netPath = path;
    }

    /**
     * Returns the network path associated with this resource at creation.
     *
     * @return this resource's network lever path or if this resource backs a domain level tunnel then null.
     */
    public org.onlab.graph.Path<DomainVertex, DomainEdge> path() {
        return netPath;
    }

    /**
     * Returns ths network ID associated with this network tunnel at creation.
     *
     * @return thsi resource's tunnel ID.
     */
    public NetworkTunnelId tunnelId() {
        return this.networkTunnelId;
    }
}
