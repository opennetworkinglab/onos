/*
 * Copyright 2015-present Open Networking Laboratory
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

import static org.onlab.osgi.DefaultServiceDirectory.getService;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.karaf.shell.console.completer.ArgumentCompleter.ArgumentList;
import org.onosproject.cli.AbstractChoicesCompleter;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;

/**
 * PortNumber completer.
 *
 * Assumes argument right before the one being completed is DeviceId.
 */
public class PortNumberCompleter extends AbstractChoicesCompleter {

    @Override
    protected List<String> choices() {
        ArgumentList args = getArgumentList();
        //parse argument list for deviceId
        DeviceService deviceService = getService(DeviceService.class);
        Device dev = null;
        for (String str : args.getArguments()) {
            if (str.contains(":")) {
                dev = deviceService.getDevice(DeviceId.deviceId(str));
                if (dev != null) {
                    break;
                }
            }
        }
        if (dev == null) {
            return Collections.singletonList("Missing device");
        }
        DeviceId deviceId = dev.id();

        return StreamSupport.stream(deviceService.getPorts(deviceId).spliterator(), false)
            .map(port -> port.number().toString())
            .collect(Collectors.toList());
    }

}
