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
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.behaviour.SoftwareUpgrade;
import org.onosproject.net.behaviour.SoftwareUpgrade.Response;
import java.util.concurrent.CompletableFuture;

/**
 * CLI command to Administratively upgrade device.
 */
@Service
@Command(scope = "onos", name = "device-upgrade",
         description = "Administratively upgrades a device")
public class SoftwareUpgradeCommand extends AbstractShellCommand {
    @Argument(index = 0, name = "deviceId", description = "Device ID",
            required = true, multiValued = false)
    @Completion(DeviceIdCompleter.class)
    String deviceId = null;

    @Option(name = "-p", aliases = "--package-to-upload",
            description = "Path to the package to be installed",
            required = false, multiValued = false)
    String packageToUpload = null;

    @Option(name = "-d", aliases = "--device-local-path",
            description = "Path on target device where (if specified) the package will be uploaded "
                        + "and/or the package to be installed",
            required = false, multiValued = false)
    String deviceLocalPath = null;

    @Override
    protected void doExecute() {
        if (packageToUpload == null && deviceLocalPath == null) {
            log.warn("Please specify the path to the file you want to install");
            return;
        }
        Device device = get(DeviceService.class).getDevice(DeviceId.deviceId(deviceId));

        if (device == null) {
            log.warn("Device {} does not exist", deviceId);
            return;
        }

        if (!device.is(SoftwareUpgrade.class)) {
            log.warn("Device {} does not support {} behaviour", deviceId, SoftwareUpgrade.class.getName());
            return;
        }

        log.info("Starting upgrade for {} (check log for errors)...", deviceId);
        CompletableFuture.supplyAsync(() -> {
            if (packageToUpload != null) {
                return device.as(SoftwareUpgrade.class)
                    .uploadPackage(packageToUpload, deviceLocalPath)
                    .join();
            } else {
                return deviceLocalPath;
            }
        })
        .thenCompose((String pathOnDevice) -> {
            if (pathOnDevice == null) {
                log.warn("Package Upload for {} on {} failed", packageToUpload, deviceId);
                return CompletableFuture.completedFuture(new Response());
            }
            return device.as(SoftwareUpgrade.class)
                .swapAgent(pathOnDevice);
        })
        .whenComplete((Response result, Throwable exception) -> {
            if (exception != null) {
                log.error("Error while upgrading device {}", deviceId, exception);
            } else if (result == null || !result.isSuccess()) {
                log.warn("Upgrade on {} failed", deviceId);
            } else {
                log.info("Upgrade on {} succeeded! \n" +
                         "New SW version: {} \n" +
                         "Uptime: {} ns",
                         deviceId, result.getVersion(), result.getUptime());
            }
        });
    }
}
