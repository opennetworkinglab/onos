/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.provider.lldp.cli;

import java.util.Optional;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cli.net.DeviceIdCompleter;
import org.onosproject.cli.net.PortNumberCompleter;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.provider.lldp.impl.LinkDiscoveryFromDevice;
import org.onosproject.provider.lldp.impl.LinkDiscoveryFromPort;

/**
 *
 */
@Command(scope = "onos", name = "config-link-discovery",
         description = "Adds configuration to disable LLDP link discovery")
public class ConfigLinkDiscoveryCommand extends AbstractShellCommand {

    // OSGi workaround to introduce package dependency
    DeviceIdCompleter deviceIdCompleter;
    @Argument(index = 0, name = "device",
            description = "DeviceID",
            required = true)
    String device = null;


    // OSGi workaround to introduce package dependency
    PortNumberCompleter portNumberCompleter;
    @Argument(index = 1, name = "port",
            description = "Port number",
            required = false)
    String port = null;

    @Option(name = "--remove", aliases = "-r",
            description = "remove configuration",
            required = false)
    boolean remove = false;

    @Option(name = "--enable",
            description = "add configuration to enable LinkDiscovery",
            required = false)
    boolean enable = false;


    @Override
    protected void execute() {
        DeviceService deviceService = get(DeviceService.class);
        NetworkConfigService netcfgService = get(NetworkConfigService.class);

        DeviceId did = DeviceId.deviceId(device);

        ConnectPoint cp = Optional.ofNullable(port)
                .map(PortNumber::fromString)
                .map(pn -> new ConnectPoint(did, pn))
                .orElse(null);

        if (cp == null) {
            // device config
            if (!remove) {
                if (deviceService.getDevice(did) == null) {
                    print("[WARN] configuring about unknown device %s", did);
                }
                LinkDiscoveryFromDevice cfg;
                cfg = netcfgService.addConfig(did, LinkDiscoveryFromDevice.class);
                cfg.enabled(enable);
                cfg.apply();
            } else {
                netcfgService.removeConfig(did, LinkDiscoveryFromDevice.class);
            }
        } else {
            // port config
            if (!remove) {
                if (deviceService.getPort(cp) == null) {
                    print("[WARN] configuring about unknown port %s", cp);
                }

                LinkDiscoveryFromPort cfg;
                cfg = netcfgService.addConfig(cp, LinkDiscoveryFromPort.class);
                cfg.enabled(enable);
                cfg.apply();
            } else {
                netcfgService.removeConfig(cp, LinkDiscoveryFromPort.class);
            }
        }
    }

}
