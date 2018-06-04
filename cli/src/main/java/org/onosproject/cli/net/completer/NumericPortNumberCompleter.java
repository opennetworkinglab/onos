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
package org.onosproject.cli.net.completer;

import static org.onlab.osgi.DefaultServiceDirectory.getService;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.onosproject.cli.net.PortNumberCompleter;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;

/**
 * PortNumber completer, which returns candidates in decimal form.
 *
 * Assumes argument right before the one being completed is DeviceId.
 */
public class NumericPortNumberCompleter extends PortNumberCompleter {

    @Override
    protected List<String> choices() {
        DeviceId deviceId = lookForDeviceId();

        if (deviceId == null) {
            return Collections.emptyList();
        }

        DeviceService deviceService = getService(DeviceService.class);
        return StreamSupport.stream(deviceService.getPorts(deviceId).spliterator(), false)
            .map(port -> Long.toString(port.number().toLong()))
            .collect(Collectors.toList());
    }
}
