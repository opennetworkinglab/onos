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
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Arranges access network according to roles assigned to devices and hosts.
 */
public class AccessNetworkLayout extends LayoutAlgorithm {

    private static final double COMPUTE_Y = -400.0;
    private static final double SERVICE_Y = -200.0;
    private static final double SPINE_Y = 0.0;
    private static final double AGGREGATION_Y = +200.0;
    private static final double ACCESS_Y = +400.0;
    private static final double HOSTS_Y = +700.0;
    private static final double GATEWAY_X = 900.0;

    private static final int HOSTS_PER_ROW = 6;
    private static final double ROW_GAP = 70;
    private static final double COL_GAP = 50;
    private static final double COMPUTE_GAP = 60.0;
    private static final double COMPUTE_OFFSET = 400.0;
    private static final double GATEWAY_GAP = 200.0;
    private static final double GATEWAY_OFFSET = -200.0;

    private int spine, aggregation, accessLeaf, serviceLeaf, compute, gateway;

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
                .forEach(d -> place(d, c(spine++, spines.size()), SPINE_Y));
    }

    private void placeServiceLeavesAndHosts() {
        List<DeviceId> leaves = deviceCategories.get(LEAF);
        List<HostId> computes = hostCategories.get(COMPUTE);
        List<HostId> gateways = hostCategories.get(GATEWAY);
        Set<HostId> placed = Sets.newHashSet();

        serviceLeaf = 1;
        leaves.stream().sorted(Comparators.ELEMENT_ID_COMPARATOR).forEach(id -> {
            gateway = 1;
            place(id, c(serviceLeaf++, leaves.size()), SERVICE_Y);

            List<HostId> gwHosts = hostService.getConnectedHosts(id).stream()
                    .map(Host::id)
                    .filter(gateways::contains)
                    .filter(hid -> !placed.contains(hid))
                    .sorted(Comparators.ELEMENT_ID_COMPARATOR)
                    .collect(Collectors.toList());

            gwHosts.forEach(hid -> {
                place(hid, serviceLeaf <= 2 ? -GATEWAY_X : GATEWAY_X,
                      c(gateway++, gwHosts.size(), GATEWAY_GAP, GATEWAY_OFFSET));
                placed.add(hid);
            });

            compute = 1;
            List<HostId> hosts = hostService.getConnectedHosts(id).stream()
                    .map(Host::id)
                    .filter(computes::contains)
                    .filter(hid -> !placed.contains(hid))
                    .sorted(Comparators.ELEMENT_ID_COMPARATOR)
                    .collect(Collectors.toList());

            hosts.forEach(hid -> {
                place(hid, c(compute++, hosts.size(), COMPUTE_GAP,
                             serviceLeaf <= 2 ? -COMPUTE_OFFSET : COMPUTE_OFFSET),
                      COMPUTE_Y);
                placed.add(hid);
            });

        });
    }

    private void placeAccessLeavesAndHosts() {
        List<DeviceId> spines = deviceCategories.get(AGGREGATION);
        List<DeviceId> leaves = deviceCategories.get(ACCESS);
        Set<DeviceId> placed = Sets.newHashSet();

        aggregation = 1;
        accessLeaf = 1;
        spines.stream().sorted(Comparators.ELEMENT_ID_COMPARATOR).forEach(id -> {
            place(id, c(aggregation++, spines.size()), AGGREGATION_Y);
            linkService.getDeviceEgressLinks(id).stream()
                    .map(l -> l.dst().deviceId())
                    .filter(leaves::contains)
                    .filter(lid -> !placed.contains(lid))
                    .sorted(Comparators.ELEMENT_ID_COMPARATOR)
                    .forEach(lid -> {
                        double x = c(accessLeaf++, leaves.size());
                        place(lid, x, ACCESS_Y);
                        placed.add(lid);
                        placeHostBlock(hostService.getConnectedHosts(lid).stream()
                                               .map(Host::id)
                                               .sorted(Comparators.ELEMENT_ID_COMPARATOR)
                                               .collect(Collectors.toList()), x, HOSTS_Y,
                                       HOSTS_PER_ROW, ROW_GAP, COL_GAP);
                    });
        });
    }

}
