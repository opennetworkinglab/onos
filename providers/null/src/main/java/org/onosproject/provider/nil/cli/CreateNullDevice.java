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

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import org.onlab.util.Tools;
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
@Service
@Command(scope = "onos", name = "null-create-device",
        description = "Adds a simulated device to the custom topology simulation")
public class CreateNullDevice extends CreateNullEntity {

    @Argument(index = 0, name = "type", description = "Device type, e.g. switch, roadm",
            required = true)
    String type = null;

    @Argument(index = 1, name = "name", description = "Device name",
            required = true)
    String name = null;

    @Argument(index = 2, name = "portCount", description = "Port count",
            required = true)
    Integer portCount = null;

    @Argument(index = 3, name = "latOrY",
            description = "Geo latitude / Grid y-coord",
            required = false)
    Double latOrY = null;

    @Argument(index = 4, name = "longOrX",
            description = "Geo longitude / Grid x-coord",
            required = false)
    Double longOrX = null;

    @Argument(index = 5, name = "locType", description = "Location type {geo|grid}",
            required = false)
    String locType = GEO;

    @Option(name = "-I", aliases = "--id", description = "Device identifier")
    String id = null;

    @Option(name = "-H", aliases = "--hw", description = "Hardware version")
    String hw = "0.1";

    @Option(name = "-S", aliases = "--sw", description = "Software version")
    String sw = "0.1.2";

    @Override
    protected void doExecute() {
        NullProviders service = get(NullProviders.class);
        NetworkConfigService cfgService = get(NetworkConfigService.class);

        TopologySimulator simulator = service.currentSimulator();
        if (!validateSimulator(simulator) || !validateLocType(locType)) {
            return;
        }

        CustomTopologySimulator sim = (CustomTopologySimulator) simulator;
        DeviceId deviceId = id == null ? sim.nextDeviceId() : DeviceId.deviceId(id);
        BasicDeviceConfig cfg = cfgService.addConfig(deviceId, BasicDeviceConfig.class);
        cfg.name(name);
        setUiCoordinates(cfg, locType, latOrY, longOrX);

        Tools.delay(10);
        sim.createDevice(deviceId, name, Device.Type.valueOf(type.toUpperCase()),
                         hw, sw, portCount);
    }

}
