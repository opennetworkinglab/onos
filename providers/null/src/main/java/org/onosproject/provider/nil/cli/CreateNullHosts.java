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

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onlab.packet.IpAddress;
import org.onlab.util.Tools;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.BasicHostConfig;
import org.onosproject.provider.nil.CustomTopologySimulator;
import org.onosproject.provider.nil.NullProviders;
import org.onosproject.provider.nil.TopologySimulator;

import java.util.List;

/**
 * Adds a simulated end-station host to the custom topology simulation.
 */
@Service
@Command(scope = "onos", name = "null-create-hosts",
        description = "Adds a simulated end-station host to the custom topology simulation")
public class CreateNullHosts extends CreateNullEntity {

    private static final double NONE = -99999999.9999;

    @Argument(index = 0, name = "deviceName", description = "Name of device where hosts are attached",
            required = true)
    String deviceName = null;

    @Argument(index = 1, name = "hostIpPattern", description = "Host IP pattern",
            required = true)
    String hostIpPattern = null;

    @Argument(index = 2, name = "hostCount", description = "Number of hosts to create",
            required = true)
    int hostCount = 0;

    @Argument(index = 3, name = "gridY", description = "Grid y-coord for top of host block")
    double gridY = NONE;

    @Argument(index = 4, name = "gridX", description = "Grid X-coord for center of host block")
    double gridX = NONE;

    @Argument(index = 5, name = "hostsPerRow", description = "Number of hosts to render per row in block")
    int hostsPerRow = 5;

    @Argument(index = 6, name = "rowGap", description = "Y gap between rows")
    double rowGap = 70;

    @Argument(index = 7, name = "colGap", description = "X gap between rows")
    double colGap = 50;


    @Override
    protected void doExecute() {
        NullProviders service = get(NullProviders.class);
        NetworkConfigService cfgService = get(NetworkConfigService.class);

        TopologySimulator simulator = service.currentSimulator();
        if (!validateSimulator(simulator)) {
            return;
        }

        CustomTopologySimulator sim = (CustomTopologySimulator) simulator;

        List<ConnectPoint> points = findAvailablePorts(sim.deviceId(deviceName));
        String pattern = hostIpPattern.replace("*", "%d");
        double yStep = rowGap / hostsPerRow;
        double y = gridY;
        double x = gridX - (colGap * (hostsPerRow - 1)) / 2;

        for (int h = 0; h < hostCount; h++) {
            HostLocation location = new HostLocation(points.get(h), System.currentTimeMillis());
            IpAddress ip = IpAddress.valueOf(String.format(pattern, h));
            HostId id = sim.nextHostId();

            if (gridY != NONE) {
                BasicHostConfig cfg = cfgService.addConfig(id, BasicHostConfig.class);
                setUiCoordinates(cfg, GRID, y, x);
                if (((h + 1) % hostsPerRow) == 0) {
                    x = gridX - (colGap * (hostsPerRow - 1)) / 2;
                } else {
                    x += colGap;
                    y += yStep;
                }
            }

            Tools.delay(10);
            sim.createHost(id, location, ip);
        }
    }

}
