/*
 * Copyright 2015 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onosproject.incubator.net.domain;

import org.onosproject.core.ApplicationId;
import org.onosproject.incubator.net.tunnel.DomainTunnelId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Path;

/**
 * A variant of intent resource specialized for use on the intra-domain level.  It contains a lower level path.
 */
public class DomainIntentResource extends IntentResource {

    private final Path domainPath;

    private final DomainTunnelId domainTunnelId;

    private final IntentDomainId intentDomainId;

    /**
     * Constructor for a domain intent resource.
     *
     * @param primitive      the primitive associated with this resource
     * @param domainTunnelId the id of this tunnel (used as a sorting mechanism)
     * @param domainId       the ID of the intent domain containing this tunnel
     * @param appId          the id of the application which created this tunnel
     * @param ingress        the fist connect point associated with this tunnel (order is irrelevant as long as it is
     *                       consistent with the path)
     * @param egress         the second connect point associated with this tunnel (order is irrelevant as long as it is
     *                       consistent with the path)
     * @param path           the path followed through the domain
     */
    public DomainIntentResource(IntentPrimitive primitive, DomainTunnelId domainTunnelId, IntentDomainId domainId,
                                ApplicationId appId, ConnectPoint ingress, ConnectPoint egress, Path path) {
        super(primitive, appId, ingress, egress);

        this.domainPath = path;
        this.domainTunnelId = domainTunnelId;
        this.intentDomainId = domainId;
    }

    /**
     * Returns the domain path associated with this resource at creation.
     *
     * @return this resource's domain level path or if this resource backs a network tunnel then null.
     */
    public Path path() {
        return domainPath;
    }

    /**
     * Returns the tunnel ID associated with this domain at creation.
     *
     * @return this resource's tunnel ID.
     */
    public DomainTunnelId tunnelId() {
        return domainTunnelId;
    }

    /**
     * Returns the domain ID associated with this resource at creation.
     *
     * @return this resource's domain ID.
     */
    public IntentDomainId domainId() {
        return intentDomainId;
    }

}
