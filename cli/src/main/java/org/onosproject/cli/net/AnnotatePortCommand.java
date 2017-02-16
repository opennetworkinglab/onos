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
package org.onosproject.cli.net;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.PortAnnotationConfig;
import org.onosproject.net.device.DeviceService;

/**
 * Annotates network device port model.
 */
@Command(scope = "onos", name = "annotate-port",
        description = "Annotates port entities")
public class AnnotatePortCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "port", description = "Device Port",
              required = true)
    String port = null;

    @Argument(index = 1, name = "key", description = "Annotation key",
             required = false)
    String key = null;

    @Argument(index = 2, name = "value",
              description = "Annotation value (null to remove)",
              required = false)
    String value = null;

    @Option(name = "--remove-config",
            description = "Remove annotation config")
    private boolean removeCfg = false;

    @Override
    protected void execute() {
        DeviceService deviceService = get(DeviceService.class);
        NetworkConfigService netcfgService = get(NetworkConfigService.class);


        ConnectPoint connectPoint = ConnectPoint.deviceConnectPoint(port);
        if (deviceService.getPort(connectPoint) == null) {
            print("Port %s does not exist.", port);
            return;
        }

        if (removeCfg && key == null) {
            // remove whole port annotation config
            netcfgService.removeConfig(connectPoint, PortAnnotationConfig.class);
            print("Annotation Config about %s removed", connectPoint);
            return;
        }

        if (key == null) {
            print("[ERROR] Annotation key not specified.");
            return;
        }

        PortAnnotationConfig cfg = netcfgService.addConfig(connectPoint, PortAnnotationConfig.class);
        if (removeCfg) {
            // remove config about entry
            cfg.annotation(key);
        } else {
            // add remove request config
            cfg.annotation(key, value);
        }
        cfg.apply();
    }

}
