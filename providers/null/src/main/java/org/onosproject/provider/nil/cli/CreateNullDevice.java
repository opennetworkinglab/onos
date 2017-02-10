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
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.BasicDeviceConfig;
import org.onosproject.provider.nil.CustomTopologySimulator;
import org.onosproject.provider.nil.NullProviders;
import org.onosproject.provider.nil.TopologySimulator;

/**
 * Adds a simulated device to the custom topology simulation.
 */
@Command(scope = "onos", name = "null-create-device",
        description = "Adds a simulated device to the custom topology simulation")
public class CreateNullDevice extends AbstractShellCommand {
    private static final String GEO = "geo";
    private static final String GRID = "grid";

    @Argument(index = 0, name = "type", description = "Device type, e.g. switch, roadm",
            required = true, multiValued = false)
    String type = null;

    @Argument(index = 1, name = "name", description = "Device name",
            required = true, multiValued = false)
    String name = null;

    @Argument(index = 2, name = "portCount", description = "Port count",
            required = true, multiValued = false)
    Integer portCount = null;

    @Argument(index = 3, name = "latOrY",
            description = "Geo latitude / Grid y-coord",
            required = true, multiValued = false)
    Double latOrY = null;

    @Argument(index = 4, name = "longOrX",
            description = "Geo longitude / Grid x-coord",
            required = true, multiValued = false)
    Double longOrX = null;

    @Argument(index = 5, name = "locType", description = "Location type {geo|grid}",
            required = false, multiValued = false)
    String locType = GEO;

    @Override
    protected void execute() {
        NullProviders service = get(NullProviders.class);
        NetworkConfigService cfgService = get(NetworkConfigService.class);

        TopologySimulator simulator = service.currentSimulator();
        if (!(simulator instanceof CustomTopologySimulator)) {
            error("Custom topology simulator is not active.");
            return;
        }

        if (!(GEO.equals(locType) || GRID.equals(locType))) {
            error("locType must be 'geo' or 'grid'.");
            return;
        }

        CustomTopologySimulator sim = (CustomTopologySimulator) simulator;
        DeviceId deviceId = sim.nextDeviceId();
        BasicDeviceConfig cfg = cfgService.addConfig(deviceId, BasicDeviceConfig.class);
        cfg.name(name)
                .locType(locType);

        if (GEO.equals(locType)) {
            cfg.latitude(latOrY).longitude(longOrX);
        } else {
            cfg.gridX(longOrX).gridY(latOrY);
        }
        cfg.apply();

        sim.createDevice(deviceId, name, Device.Type.valueOf(type.toUpperCase()), portCount);
    }

}
