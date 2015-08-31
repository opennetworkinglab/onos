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

import org.onlab.packet.MplsLabel;
import org.onlab.packet.VlanId;
import org.onlab.util.ItemNotFoundException;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Link;
import org.onosproject.net.LinkKey;
import org.onosproject.net.behaviour.MplsQuery;
import org.onosproject.net.behaviour.VlanQuery;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.link.LinkListener;
import org.onosproject.net.newresource.ResourceAdminService;
import org.onosproject.net.newresource.ResourcePath;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An implementation of LinkListener registering links as resources.
 */
final class ResourceLinkListener implements LinkListener {

    private static final int TOTAL_VLANS = 1024;
    private static final List<VlanId> ENTIRE_VLAN_IDS = getEntireVlans();

    private static final int TOTAL_MPLS_LABELS = 1048576;
    private static final List<MplsLabel> ENTIRE_MPLS_LABELS = getEntireMplsLabels();

    private final ResourceAdminService adminService;
    private final DriverService driverService;
    private final ExecutorService executor;

    /**
     * Creates an instance with the specified ResourceAdminService and ExecutorService.
     *
     * @param adminService instance invoked to register resources
     * @param driverService driver service instance
     * @param executor executor used for processing resource registration
     */
    ResourceLinkListener(ResourceAdminService adminService, DriverService driverService, ExecutorService executor) {
        this.adminService = checkNotNull(adminService);
        this.driverService = checkNotNull(driverService);
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
        executor.submit(() -> {
            // register the link
            LinkKey linkKey = LinkKey.linkKey(link);
            adminService.registerResources(ResourcePath.ROOT, linkKey);

            ResourcePath linkPath = new ResourcePath(linkKey);
            // register VLAN IDs against the link
            if (isEnabled(link, this::isVlanEnabled)) {
                adminService.registerResources(linkPath, ENTIRE_VLAN_IDS);
            }

            // register MPLS labels against the link
            if (isEnabled(link, this::isMplsEnabled)) {
                adminService.registerResources(linkPath, ENTIRE_MPLS_LABELS);
            }
        });
    }

    private void unregisterLinkResource(Link link) {
        LinkKey linkKey = LinkKey.linkKey(link);
        executor.submit(() -> adminService.unregisterResources(ResourcePath.ROOT, linkKey));
    }

    private boolean isEnabled(Link link, Predicate<ConnectPoint> predicate) {
        return predicate.test(link.src()) && predicate.test(link.dst());
    }

    private boolean isVlanEnabled(ConnectPoint cp) {
        try {
            DriverHandler handler = driverService.createHandler(cp.deviceId());
            if (handler == null) {
                return false;
            }

            VlanQuery query = handler.behaviour(VlanQuery.class);
            return query != null && query.isEnabled(cp.port());
        } catch (ItemNotFoundException e) {
            return false;
        }
    }

    private boolean isMplsEnabled(ConnectPoint cp) {
        try {
            DriverHandler handler = driverService.createHandler(cp.deviceId());
            if (handler == null) {
                return false;
            }

            MplsQuery query = handler.behaviour(MplsQuery.class);
            return query != null && query.isEnabled(cp.port());
        } catch (ItemNotFoundException e) {
            return false;
        }
    }

    private static List<VlanId> getEntireVlans() {
        return IntStream.range(0, TOTAL_VLANS)
                .mapToObj(x -> VlanId.vlanId((short) x))
                .collect(Collectors.toList());
    }

    private static List<MplsLabel> getEntireMplsLabels() {
        // potentially many objects are created
        return IntStream.range(0, TOTAL_MPLS_LABELS)
                .mapToObj(MplsLabel::mplsLabel)
                .collect(Collectors.toList());
    }
}
