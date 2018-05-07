/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.layout;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.utils.Comparators;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Arranges access network according to roles assigned to devices and hosts.
 */
public class AccessNetworkLayout extends LayoutAlgorithm {

    private double computeY = -350.0;
    private double serviceY = -200.0;
    private double spineY = 0.0;
    private double aggregationY = +200.0;
    private double accessY = +400.0;
    private double hostsY = +550.0;

    private double gatewayX = 1500.0;
    private double rowGap = 70;
    private double computeRowGap = -120;
    private double colGap = 54;
    private double computeOffset = 800.0;
    private double gatewayGap = 200.0;
    private double gatewayOffset = -200.0;
    private double serviceGap = 800;
    private int computePerRow = 25;
    private double spinesGap = 800;
    private double aggregationGap = 400;
    private double accessGap = 400;
    private int hostsPerRow = 6;

    private int spine, aggregation, accessLeaf, serviceLeaf, gateway;

    /**
     * Creates the network layout using default layout options.
     */
    public AccessNetworkLayout() {
    }

    /**
     * Creates the network layout using the specified layout property overrides.
     *
     * @param custom overrides of the default layout properties
     */
    public AccessNetworkLayout(Map<String, Object> custom) {
        computeY = (double) custom.getOrDefault("computeY", computeY);
        serviceY = (double) custom.getOrDefault("serviceY", serviceY);
        spineY = (double) custom.getOrDefault("spineY", spineY);
        aggregationY = (double) custom.getOrDefault("aggregationY", aggregationY);
        accessY = (double) custom.getOrDefault("accessY", accessY);
        hostsY = (double) custom.getOrDefault("hostsY", hostsY);
        gatewayX = (double) custom.getOrDefault("gatewayX", gatewayX);
        rowGap = (double) custom.getOrDefault("rowGap", rowGap);
        computeRowGap = (double) custom.getOrDefault("computeRowGap", computeRowGap);
        colGap = (double) custom.getOrDefault("colGap", colGap);
        computeOffset = (double) custom.getOrDefault("computeOffset", computeOffset);
        gatewayGap = (double) custom.getOrDefault("gatewayGap", gatewayGap);
        gatewayOffset = (double) custom.getOrDefault("gatewayOffset", gatewayOffset);
        serviceGap = (double) custom.getOrDefault("serviceGap", serviceGap);
        computePerRow = (int) custom.getOrDefault("computePerRow", computePerRow);
        spinesGap = (double) custom.getOrDefault("spinesGap", spinesGap);
        aggregationGap = (double) custom.getOrDefault("aggregationGap", aggregationGap);
        accessGap = (double) custom.getOrDefault("accessGap", accessGap);
        hostsPerRow = (int) custom.getOrDefault("hostsPerRow", hostsPerRow);
    }

    @Override
    protected boolean classify(Device device) {
        if (!super.classify(device)) {
            String role;

            // Does the device have any hosts attached? If not, it's a spine
            if (hostService.getConnectedHosts(device.id()).isEmpty()) {
                // Does the device have any aggregate links to other devices?
                Multiset<DeviceId> destinations = HashMultiset.create();
                linkService.getDeviceEgressLinks(device.id()).stream()
                        .map(l -> l.dst().deviceId()).forEach(destinations::add);

                // If yes, it's the main spine; otherwise it's an aggregate spine
                role = destinations.entrySet().stream().anyMatch(e -> e.getCount() > 1) ?
                        SPINE : AGGREGATION;
            } else {
                // Does the device have any multi-home hosts attached?
                // If yes, it's a service leaf; otherwise it's an access leaf
                role = hostService.getConnectedHosts(device.id()).stream()
                        .map(Host::locations).anyMatch(s -> s.size() > 1) ?
                        LEAF : ACCESS;
            }
            deviceCategories.put(role, device.id());
        }
        return true;
    }

    @Override
    protected boolean classify(Host host) {
        if (!super.classify(host)) {
            // Is the host attached to an access leaf?
            // If so, it's an access host; otherwise it's a service host or gateway
            String role = host.locations().stream().map(ConnectPoint::deviceId)
                    .anyMatch(d -> deviceCategories.get(ACCESS)
                            .contains(deviceService.getDevice(d).id())) ?
                    ACCESS : COMPUTE;
            hostCategories.put(role, host.id());
        }
        return true;
    }

    @Override
    public void apply() {
        placeSpines();
        placeServiceLeavesAndHosts();
        placeAccessLeavesAndHosts();
    }

    private void placeSpines() {
        spine = 1;
        List<DeviceId> spines = deviceCategories.get(SPINE);
        spines.stream().sorted(Comparators.ELEMENT_ID_COMPARATOR)
                .forEach(d -> place(d, c(spine++, spines.size(), spinesGap), spineY));
    }

    private void placeServiceLeavesAndHosts() {
        List<DeviceId> leaves = deviceCategories.get(LEAF);
        List<HostId> computes = hostCategories.get(COMPUTE);
        List<HostId> gateways = hostCategories.get(GATEWAY);
        Set<HostId> placed = Sets.newHashSet();

        serviceLeaf = 1;
        leaves.stream().sorted(Comparators.ELEMENT_ID_COMPARATOR).forEach(id -> {
            gateway = 1;
            place(id, c(serviceLeaf++, leaves.size(), serviceGap), serviceY);

            List<HostId> gwHosts = hostService.getConnectedHosts(id).stream()
                    .map(Host::id)
                    .filter(gateways::contains)
                    .filter(hid -> !placed.contains(hid))
                    .sorted(Comparators.ELEMENT_ID_COMPARATOR)
                    .collect(Collectors.toList());

            gwHosts.forEach(hid -> {
                place(hid, serviceLeaf <= 2 ? -gatewayX : gatewayX,
                      c(gateway++, gwHosts.size(), gatewayGap, gatewayOffset));
                placed.add(hid);
            });

            List<HostId> hosts = hostService.getConnectedHosts(id).stream()
                    .map(Host::id)
                    .filter(computes::contains)
                    .filter(hid -> !placed.contains(hid))
                    .sorted(Comparators.ELEMENT_ID_COMPARATOR)
                    .collect(Collectors.toList());

            placeHostBlock(hosts, serviceLeaf <= 2 ? -computeOffset : computeOffset,
                           computeY, computePerRow, computeRowGap,
                           serviceLeaf <= 2 ? -colGap : colGap);
            placed.addAll(hosts);
        });
    }

    private void placeAccessLeavesAndHosts() {
        List<DeviceId> spines = deviceCategories.get(AGGREGATION);
        List<DeviceId> leaves = deviceCategories.get(ACCESS);
        Set<DeviceId> placed = Sets.newHashSet();

        aggregation = 1;
        accessLeaf = 1;
        if (spines.isEmpty()) {
            leaves.stream().sorted(Comparators.ELEMENT_ID_COMPARATOR)
                    .forEach(lid -> placeAccessLeafAndHosts(lid, leaves.size(), placed));
        } else {
            spines.stream().sorted(Comparators.ELEMENT_ID_COMPARATOR).forEach(id -> {
                place(id, c(aggregation++, spines.size(), aggregationGap), aggregationY);
                linkService.getDeviceEgressLinks(id).stream()
                        .map(l -> l.dst().deviceId())
                        .filter(leaves::contains)
                        .filter(lid -> !placed.contains(lid))
                        .sorted(Comparators.ELEMENT_ID_COMPARATOR)
                        .forEach(lid -> placeAccessLeafAndHosts(lid, leaves.size(), placed));
            });
        }
    }

    private void placeAccessLeafAndHosts(DeviceId leafId, int leafCount, Set<DeviceId> placed) {
        double x = c(accessLeaf++, leafCount, accessGap);
        place(leafId, x, accessY);
        placed.add(leafId);
        placeHostBlock(hostService.getConnectedHosts(leafId).stream()
                               .map(Host::id)
                               .sorted(Comparators.ELEMENT_ID_COMPARATOR)
                               .collect(Collectors.toList()), x, hostsY,
                       hostsPerRow, rowGap, colGap);
    }

}
