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

package org.onosproject.provider.nil.cli;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onlab.packet.IpAddress;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.provider.nil.CustomTopologySimulator;
import org.onosproject.provider.nil.NullProviders;
import org.onosproject.provider.nil.TopologySimulator;

import java.util.List;

/**
 * Adds a simulated end-station host to the custom topology simulation.
 */
@Command(scope = "onos", name = "null-create-hosts",
        description = "Adds a simulated end-station host to the custom topology simulation")
public class CreateNullHosts extends CreateNullEntity {

    @Argument(index = 0, name = "deviceName", description = "Name of device where hosts are attached",
            required = true)
    String deviceName = null;

    @Argument(index = 1, name = "hostIpPattern", description = "Host IP pattern",
            required = true)
    String hostIpPattern = null;

    @Argument(index = 2, name = "hostCount", description = "Number of hosts to create",
            required = true)
    int hostCount = 0;

    @Override
    protected void execute() {
        NullProviders service = get(NullProviders.class);

        TopologySimulator simulator = service.currentSimulator();
        if (!validateSimulator(simulator)) {
            return;
        }

        CustomTopologySimulator sim = (CustomTopologySimulator) simulator;

        List<ConnectPoint> points = findAvailablePorts(sim.deviceId(deviceName));
        String pattern = hostIpPattern.replace("*", "%d");
        for (int h = 0; h < hostCount; h++) {
            HostLocation location = new HostLocation(points.get(h), System.currentTimeMillis());
            IpAddress ip = IpAddress.valueOf(String.format(pattern, h));
            HostId id = sim.nextHostId();
            sim.createHost(id, location, ip);
        }
    }

}
