/*
 * Copyright 2015-present Open Networking Foundation
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

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.DeviceId;
import org.onosproject.net.driver.DriverService;

import java.util.Map;

@Service
@Command(scope = "onos", name = "device-drivers",
        description = "list all devices and their driver names or a driver name of a device")
public class DeviceDriversCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "uri", description = "Device ID",
            required = false, multiValued = false)
    @Completion(DeviceIdCompleter.class)
    String uri = null;

    @Override
    protected void doExecute() {
        DriverService service = get(DriverService.class);

        if (uri == null) {
            Map<DeviceId, String> deviceDriverNameMap = service.getDeviceDrivers();
            if (outputJson()) {
                json(deviceDriverNameMap);
            } else {
                deviceDriverNameMap.forEach((k, v) -> print("%s : %s", k.toString(), v));
            }
        } else {
            DeviceId deviceId = DeviceId.deviceId(uri);
            String driverName = service.getDriver(deviceId).name();
            if (outputJson()) {
                json(deviceId, driverName);
            } else {
                print("%s : %s", deviceId.toString(), driverName);
            }
        }
    }

    private void json(Map<DeviceId, String> map) {
        ObjectNode result = mapper().createObjectNode();
        map.forEach((k, v) -> {
            result.put(k.toString(), v);
        });
        print("%s", result.toString());
    }

    private void json(DeviceId deviceId, String driverName) {
        ObjectNode result = mapper().createObjectNode();
        result.put(deviceId.toString(), driverName);
        print("%s", result.toString());
    }

}
