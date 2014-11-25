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
package org.onlab.onos.cli;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.karaf.shell.commands.Command;
import org.onlab.onos.cluster.ClusterService;
import org.onlab.onos.cluster.ControllerNode;
import org.onlab.onos.mastership.MastershipAdminService;
import org.onlab.onos.mastership.MastershipService;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.MastershipRole;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Forces device mastership rebalancing.
 */
@Command(scope = "onos", name = "balance-masters",
        description = "Forces device mastership rebalancing")
public class BalanceMastersCommand extends AbstractShellCommand {

    @Override
    protected void execute() {
        ClusterService service = get(ClusterService.class);
        MastershipService mastershipService = get(MastershipService.class);
        MastershipAdminService adminService = get(MastershipAdminService.class);

        List<ControllerNode> nodes = newArrayList(service.getNodes());

        Multimap<ControllerNode, DeviceId> controllerDevices = HashMultimap.create();

        // Create buckets reflecting current ownership.
        for (ControllerNode node : nodes) {
            controllerDevices.putAll(node, mastershipService.getDevicesOf(node.id()));
        }

        int bucketCount = nodes.size();
        for (int i = 0; i < bucketCount / 2; i++) {
            // Iterate over the buckets and find the smallest and the largest.
            ControllerNode smallest = findSmallestBucket(controllerDevices);
            ControllerNode largest = findLargestBucket(controllerDevices);
            balanceBuckets(smallest, largest, controllerDevices,
                           mastershipService, adminService);
        }
    }

    private ControllerNode findSmallestBucket(Multimap<ControllerNode, DeviceId> controllerDevices) {
        int minSize = Integer.MAX_VALUE;
        ControllerNode minNode = null;
        for (ControllerNode node : controllerDevices.keySet()) {
            int size = controllerDevices.get(node).size();
            if (size < minSize) {
                minSize = size;
                minNode = node;
            }
        }
        return minNode;
    }

    private ControllerNode findLargestBucket(Multimap<ControllerNode, DeviceId> controllerDevices) {
        int maxSize = -1;
        ControllerNode maxNode = null;
        for (ControllerNode node : controllerDevices.keySet()) {
            int size = controllerDevices.get(node).size();
            if (size >= maxSize) {
                maxSize = size;
                maxNode = node;
            }
        }
        return maxNode;
    }

    // FIXME: enhance to better handle cases where smallest cannot take any of the devices from largest

    private void balanceBuckets(ControllerNode smallest, ControllerNode largest,
                                Multimap<ControllerNode, DeviceId> controllerDevices,
                                MastershipService mastershipService,
                                MastershipAdminService adminService) {
        Collection<DeviceId> minBucket = controllerDevices.get(smallest);
        Collection<DeviceId> maxBucket = controllerDevices.get(largest);

        int delta = (maxBucket.size() - minBucket.size()) / 2;

        print("Attempting to move %d nodes from %s to %s...",
              delta, largest.id(), smallest.id());

        int i = 0;
        Iterator<DeviceId> it = maxBucket.iterator();
        while (it.hasNext() && i < delta) {
            DeviceId deviceId = it.next();

            // Check that the transfer can happen for the current element.
            if (mastershipService.getNodesFor(deviceId).backups().contains(smallest.id())) {
                print("Setting %s as the new master for %s", smallest.id(), deviceId);
                adminService.setRole(smallest.id(), deviceId, MastershipRole.MASTER);
                i++;
            }
        }

        controllerDevices.removeAll(smallest);
    }

}
