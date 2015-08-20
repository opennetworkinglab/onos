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
package org.onosproject.net.newresource.impl;

import org.onosproject.net.Link;
import org.onosproject.net.LinkKey;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.link.LinkListener;
import org.onosproject.net.newresource.ResourceAdminService;
import org.onosproject.net.newresource.ResourcePath;

import java.util.concurrent.ExecutorService;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An implementation of LinkListener registering links as resources.
 */
final class ResourceLinkListener implements LinkListener {

    private final ResourceAdminService adminService;
    private final ExecutorService executor;

    /**
     * Creates an instance with the specified ResourceAdminService and ExecutorService.
     *
     * @param adminService instance invoked to register resources
     * @param executor executor used for processing resource registration
     */
    ResourceLinkListener(ResourceAdminService adminService, ExecutorService executor) {
        this.adminService = checkNotNull(adminService);
        this.executor = checkNotNull(executor);
    }

    @Override
    public void event(LinkEvent event) {
        Link link = event.subject();
        switch (event.type()) {
            case LINK_ADDED:
                registerLinkResource(link);
                break;
            case LINK_REMOVED:
                unregisterLinkResource(link);
                break;
            default:
                break;
        }
    }

    private void registerLinkResource(Link link) {
        LinkKey linkKey = LinkKey.linkKey(link);
        executor.submit(() -> adminService.registerResources(ResourcePath.ROOT, linkKey));
    }

    private void unregisterLinkResource(Link link) {
        LinkKey linkKey = LinkKey.linkKey(link);
        executor.submit(() -> adminService.unregisterResources(ResourcePath.ROOT, linkKey));
    }
}
