/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.segmentrouting.cli;

import org.apache.karaf.shell.commands.Command;
import org.onlab.packet.IpPrefix;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.DeviceId;
import org.onosproject.segmentrouting.SegmentRoutingService;

import java.util.Map;
import java.util.Set;

/**
 * Command to list device-subnet mapping in Segment Routing.
 */
@Command(scope = "onos", name = "sr-device-subnets",
        description = "List device-subnet mapping in Segment Routing")
public class DeviceSubnetListCommand extends AbstractShellCommand {
    @Override
    protected void execute() {
        SegmentRoutingService srService =
                AbstractShellCommand.get(SegmentRoutingService.class);
        printDeviceSubnetMap(srService.getDeviceSubnetMap());
    }

    private void printDeviceSubnetMap(Map<DeviceId, Set<IpPrefix>> deviceSubnetMap) {
        deviceSubnetMap.forEach(((deviceId, ipPrefices) -> {
            print("%s", deviceId);
            ipPrefices.forEach(ipPrefix -> print("    %s", ipPrefix));
        }));
    }
}
