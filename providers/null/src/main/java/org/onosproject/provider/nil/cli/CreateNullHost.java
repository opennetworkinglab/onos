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

package org.onosproject.provider.nil.cli;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onlab.packet.IpAddress;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.BasicHostConfig;
import org.onosproject.net.edge.EdgePortService;
import org.onosproject.net.host.HostService;
import org.onosproject.provider.nil.CustomTopologySimulator;
import org.onosproject.provider.nil.NullProviders;
import org.onosproject.provider.nil.TopologySimulator;

import java.util.Iterator;

/**
 * Adds a simulated end-station host to the custom topology simulation.
 */
@Command(scope = "onos", name = "null-create-host",
        description = "Adds a simulated end-station host to the custom topology simulation")
public class CreateNullHost extends AbstractShellCommand {

    @Argument(index = 0, name = "deviceName", description = "Name of device where host is attached",
            required = true, multiValued = false)
    String deviceName = null;

    @Argument(index = 1, name = "hostIp", description = "Host IP address",
            required = true, multiValued = false)
    String hostIp = null;

    @Argument(index = 2, name = "latitude", description = "Geo latitude",
            required = true, multiValued = false)
    Double latitude = null;

    @Argument(index = 3, name = "longitude", description = "Geo longitude",
            required = true, multiValued = false)
    Double longitude = null;

    @Override
    protected void execute() {
        NullProviders service = get(NullProviders.class);
        NetworkConfigService cfgService = get(NetworkConfigService.class);

        TopologySimulator simulator = service.currentSimulator();
        if (!(simulator instanceof CustomTopologySimulator)) {
            error("Custom topology simulator is not active.");
            return;
        }

        CustomTopologySimulator sim = (CustomTopologySimulator) simulator;
        DeviceId deviceId = sim.deviceId(deviceName);
        HostId id = sim.nextHostId();
        HostLocation location = findAvailablePort(deviceId);
        BasicHostConfig cfg = cfgService.addConfig(id, BasicHostConfig.class);
        cfg.latitude(latitude);
        cfg.longitude(longitude);
        cfg.apply();

        sim.createHost(id, location, IpAddress.valueOf(hostIp));
    }

    // Finds an available connect point among edge ports of the specified device
    private HostLocation findAvailablePort(DeviceId deviceId) {
        EdgePortService eps = get(EdgePortService.class);
        HostService hs = get(HostService.class);
        Iterator<ConnectPoint> points = eps.getEdgePoints(deviceId).iterator();

        while (points.hasNext()) {
            ConnectPoint point = points.next();
            if (hs.getConnectedHosts(point).isEmpty()) {
                return new HostLocation(point, System.currentTimeMillis());
            }
        }
        return null;
    }

}
