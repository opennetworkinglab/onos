/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.segmentrouting;

import org.onlab.packet.IpPrefix;
import org.onosproject.net.ConnectPoint;

import java.util.Map;
import java.util.Set;

/**
 * Mock Default Routing Handler.
 */
public class MockDefaultRoutingHandler extends DefaultRoutingHandler {
    private Map<ConnectPoint, Set<IpPrefix>> subnetTable;

    MockDefaultRoutingHandler(SegmentRoutingManager srManager,
                              Map<ConnectPoint, Set<IpPrefix>> subnetTable) {
        super(srManager);
        this.subnetTable = subnetTable;
    }

    @Override
    protected void populateSubnet(Set<ConnectPoint> cpts, Set<IpPrefix> subnets) {
        subnetTable.forEach((k, v) -> {
            if (!cpts.contains(k)) {
                subnetTable.get(k).removeAll(subnets);
                if (subnetTable.get(k).isEmpty()) {
                    subnetTable.remove(k);
                }
            }
        });

        cpts.forEach(cpt -> subnetTable.put(cpt, subnets));
    }

    @Override
    protected boolean revokeSubnet(Set<IpPrefix> subnets) {
        for (Map.Entry<ConnectPoint, Set<IpPrefix>> entry : subnetTable.entrySet()) {
            entry.getValue().removeAll(subnets);
            if (entry.getValue().isEmpty()) {
                subnetTable.remove(entry.getKey());
            }
        }
        return true;
    }
}