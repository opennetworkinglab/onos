/*
 * Copyright 2014-present Open Networking Foundation
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.ImmutableSet;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.action.Option;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.Device;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverService;
import org.onosproject.utils.Comparators;

import java.util.Collections;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Lists all infrastructure devices.
 */
@Service
@Command(scope = "onos", name = "devices",
        description = "Lists all infrastructure devices")
public class DevicesListCommand extends AbstractShellCommand {

    private static final String FMT =
            "id=%s, available=%s, local-status=%s, role=%s, type=%s, mfr=%s, hw=%s, sw=%s, " +
                    "serial=%s, chassis=%s, driver=%s%s";

    private static final String FMT_SHORT =
            "id=%s, available=%s, role=%s, type=%s, driver=%s";

    @Option(name = "-s", aliases = "--short", description = "Show short output only",
            required = false, multiValued = false)
    private boolean shortOnly = false;

    @Override
    protected void doExecute() {
        DeviceService deviceService = get(DeviceService.class);
        if (outputJson()) {
            print("%s", json(getSortedDevices(deviceService)));
        } else {
            for (Device device : getSortedDevices(deviceService)) {
                printDevice(deviceService, device);
            }
        }
    }

    /**
     * Returns JSON node representing the specified devices.
     *
     * @param devices collection of devices
     * @return JSON node
     */
    private JsonNode json(Iterable<Device> devices) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();
        for (Device device : devices) {
            result.add(jsonForEntity(device, Device.class));
        }
        return result;
    }

    /**
     * Returns the list of devices sorted using the device ID URIs.
     *
     * @param service device service
     * @return sorted device list
     */
    public static List<Device> getSortedDevices(DeviceService service) {
        List<Device> devices = newArrayList(service.getDevices());
        Collections.sort(devices, Comparators.ELEMENT_COMPARATOR);
        return devices;
    }

    /**
     * Prints information about the specified device.
     *
     * @param deviceService device service
     * @param device        infrastructure device
     */
    protected void printDevice(DeviceService deviceService, Device device) {
        if (device != null) {
            String driver = get(DriverService.class).getDriver(device.id()).name();
            if (shortOnly) {
                print(FMT_SHORT, device.id(), deviceService.isAvailable(device.id()),
                      deviceService.getRole(device.id()), device.type(), driver);
            } else {
                print(FMT, device.id(), deviceService.isAvailable(device.id()),
                      deviceService.localStatus(device.id()),
                      deviceService.getRole(device.id()), device.type(),
                      device.manufacturer(), device.hwVersion(), device.swVersion(),
                      device.serialNumber(), device.chassisId(), driver,
                      annotations(device.annotations(), ImmutableSet.of(AnnotationKeys.DRIVER)));
            }
        }
    }
}
