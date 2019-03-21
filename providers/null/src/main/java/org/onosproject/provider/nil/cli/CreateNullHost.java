/*
 * Copyright 2016-present Open Networking Foundation
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

import com.google.common.collect.ImmutableSet;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onlab.packet.IpAddress;
import org.onlab.util.Tools;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.BasicHostConfig;
import org.onosproject.provider.nil.CustomTopologySimulator;
import org.onosproject.provider.nil.NullProviders;
import org.onosproject.provider.nil.TopologySimulator;

import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * Adds a simulated end-station host to the custom topology simulation.
 */
@Service
@Command(scope = "onos", name = "null-create-host",
        description = "Adds a simulated end-station host to the custom topology simulation")
public class CreateNullHost extends CreateNullEntity {

    @Argument(index = 0, name = "deviceNames", description = "Name of device where host is attached; comma-separated",
            required = true)
    String deviceNames = null;

    @Argument(index = 1, name = "hostIps", description = "Host IP addresses; comma-separated",
            required = true)
    String hostIps = null;

    @Argument(index = 2, name = "latOrY",
            description = "Geo latitude / Grid y-coord",
            required = false)
    Double latOrY = null;

    @Argument(index = 3, name = "longOrX",
            description = "Geo longitude / Grid x-coord",
            required = false)
    Double longOrX = null;

    @Argument(index = 4, name = "locType", description = "Location type {geo|grid}",
            required = false)
    String locType = GEO;

    @Override
    protected void doExecute() {
        NullProviders service = get(NullProviders.class);
        NetworkConfigService cfgService = get(NetworkConfigService.class);

        TopologySimulator simulator = service.currentSimulator();
        if (!validateSimulator(simulator) || !validateLocType(locType)) {
            return;
        }

        CustomTopologySimulator sim = (CustomTopologySimulator) simulator;
        HostId id = sim.nextHostId();
        Set<HostLocation> locations;
        try {
            locations = getLocations(sim, deviceNames);
        } catch (NoLocationException e) {
            error("\u001B[1;31mHost not created - no location (free port) available on %s\u001B[0m", e.getMessage());
            return;
        }
        Set<IpAddress> ips = getIps(hostIps);

        BasicHostConfig cfg = cfgService.addConfig(id, BasicHostConfig.class);
        setUiCoordinates(cfg, locType, latOrY, longOrX);

        Tools.delay(10);
        sim.createHost(id, locations, ips);
    }

    private Set<IpAddress> getIps(String hostIps) {
        ImmutableSet.Builder<IpAddress> ips = ImmutableSet.builder();
        String[] csv = hostIps.split(",");
        for (String s : csv) {
            ips.add(IpAddress.valueOf(s));
        }
        return ips.build();
    }

    private Set<HostLocation> getLocations(CustomTopologySimulator sim, String deviceNames)
            throws NoLocationException {
        ImmutableSet.Builder<HostLocation> locations = ImmutableSet.builder();
        String[] csv = deviceNames.split(",");
        for (String s : csv) {
            HostLocation loc = findAvailablePort(sim.deviceId(s));
            if (loc == null) {
                throw new NoLocationException(deviceNames);
            }
            locations.add(requireNonNull(findAvailablePort(sim.deviceId(s))));
        }
        return locations.build();
    }

    // Finds an available connect point among edge ports of the specified device
    private HostLocation findAvailablePort(DeviceId deviceId) {
        ConnectPoint point = findAvailablePort(deviceId, null);
        return point == null ? null : new HostLocation(point, System.currentTimeMillis());
    }

}
