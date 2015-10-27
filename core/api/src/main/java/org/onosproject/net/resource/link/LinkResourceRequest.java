/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.net.resource.link;

import org.onosproject.net.Link;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.intent.IntentId;
import org.onosproject.net.resource.ResourceRequest;

import java.util.Collection;
import java.util.Set;

/**
 * Representation of a request for link resource.
 */
public interface LinkResourceRequest extends ResourceRequest {

    /**
     * Returns the {@link IntentId} associated with the request.
     *
     * @return the {@link IntentId} associated with the request
     */
    IntentId intentId();

    /**
     * Returns the set of target links.
     *
     * @return the set of target links
     */
    Collection<Link> links();

    /**
     * Returns the set of resource requests.
     *
     * @return the set of resource requests
     */
    Set<ResourceRequest> resources();

    /**
     * Builder of link resource request.
     */
    interface Builder {
        /**
         * Adds lambda request.
         *
         * @return self
         */
        Builder addLambdaRequest();

        /**
         * Adds MPLS request.
         *
         * @return self
         */
        Builder addMplsRequest();

        /**
         * Adds bandwidth request with bandwidth value.
         *
         * @param bandwidth bandwidth value to be requested
         * @return self
         */
        Builder addBandwidthRequest(double bandwidth);

        /**
         * Adds the resources required for a constraint.
         *
         * @param constraint the constraint
         * @return self
         */
        Builder addConstraint(Constraint constraint);

        /**
         * Returns link resource request.
         *
         * @return link resource request
         */
        LinkResourceRequest build();
    }
}
