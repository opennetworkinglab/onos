/*
 * Copyright 2019-present Open Networking Foundation
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

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.BasicSystemOperations;
import org.onosproject.net.device.DeviceService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.CompletableFuture;

/**
 * Gets time since epoch.
 */
@Service
@Command(scope = "onos", name = "device-time",
         description = "Returns the current time on the target device")
public class DeviceTimeCommand extends AbstractShellCommand {
    @Argument(index = 0, name = "deviceId", description = "Device ID",
            required = true, multiValued = false)
    @Completion(DeviceIdCompleter.class)
    String deviceId = null;

    @Override
    protected void doExecute() {
        Device dev = get(DeviceService.class).getDevice(DeviceId.deviceId(deviceId));
        if (dev == null) {
            print(" %s", "Device does not exist");
            return;
        }

        if (dev.is(BasicSystemOperations.class)) {
            try {
                CompletableFuture<Long> timeFuture = dev.as(BasicSystemOperations.class).time();
                print("Current time on the device: %s %d", deviceId, timeFuture.get());
            } catch (InterruptedException | ExecutionException e) {
                log.error("Exception while getting system time for device" + deviceId, e);
            }
        } else {
            log.error("Device does not support {} behaviour", BasicSystemOperations.class.getName());
        }
    }
}
