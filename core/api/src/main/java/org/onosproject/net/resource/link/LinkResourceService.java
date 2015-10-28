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
package org.onosproject.net.resource.link;

import org.onosproject.event.ListenerService;
import org.onosproject.net.Link;
import org.onosproject.net.intent.IntentId;
import org.onosproject.net.resource.ResourceRequest;

/**
 * Service for providing link resource allocation.
 *
 * @deprecated in Emu Release
 */
@Deprecated
public interface LinkResourceService
    extends ListenerService<LinkResourceEvent, LinkResourceListener> {

    /**
     * Requests resources.
     *
     * @param req resources to be allocated
     * @return allocated resources
     * @deprecated in Emu Release
     */
    @Deprecated
    LinkResourceAllocations requestResources(LinkResourceRequest req);

    /**
     * Releases resources.
     *
     * @param allocations resources to be released
     * @deprecated in Emu Release
     */
    @Deprecated
    void releaseResources(LinkResourceAllocations allocations);

    /**
     * Updates previously made allocations with a new resource request.
     *
     * @param req            updated resource request
     * @param oldAllocations old resource allocations
     * @return new resource allocations
     * @deprecated in Emu Release
     */
    @Deprecated
    LinkResourceAllocations updateResources(LinkResourceRequest req,
                                            LinkResourceAllocations oldAllocations);

    /**
     * Returns all allocated resources.
     *
     * @return allocated resources
     * @deprecated in Emu Release
     */
    @Deprecated
    Iterable<LinkResourceAllocations> getAllocations();

    /**
     * Returns all allocated resources to given link.
     *
     * @param link a target link
     * @return allocated resources
     * @deprecated in Emu Release
     */
    @Deprecated
    Iterable<LinkResourceAllocations> getAllocations(Link link);

    /**
     * Returns the resources allocated for an Intent.
     *
     * @param intentId the target Intent's id
     * @return allocated resources for Intent
     * @deprecated in Emu Release
     */
    @Deprecated
    LinkResourceAllocations getAllocations(IntentId intentId);

    /**
     * Returns available resources for given link.
     *
     * @param link a target link
     * @return available resources for the target link
     * @deprecated in Emu Release
     */
    @Deprecated
    Iterable<ResourceRequest> getAvailableResources(Link link);

    /**
     * Returns available resources for given link.
     *
     * @param link        a target link
     * @param allocations allocations to be included as available
     * @return available resources for the target link
     * @deprecated in Emu Release
     */
    @Deprecated
    Iterable<ResourceRequest> getAvailableResources(Link link,
                                                    LinkResourceAllocations allocations);

}
