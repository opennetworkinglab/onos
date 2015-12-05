/*
 * Copyright 2015 Open Networking Laboratory
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

import static org.onosproject.net.DeviceId.deviceId;

import java.util.Collection;
import java.util.stream.StreamSupport;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.OchSignal;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.newresource.ResourceAllocation;
import org.onosproject.net.newresource.ResourcePath;
import org.onosproject.net.newresource.ResourceService;

import com.google.common.base.Strings;

/**
 * Lists allocated resources.
 */
@Command(scope = "onos", name = "allocations",
         description = "Lists allocated resources")
public class AllocationsCommand extends AbstractShellCommand {

    // TODO add other resource types
    @Option(name = "-l", aliases = "--lambda", description = "Lambda Resource",
            required = false, multiValued = false)
    private boolean lambda = true;

    @Argument(index = 0, name = "deviceIdString", description = "Device ID",
              required = false, multiValued = false)
    String deviceIdStr = null;

    @Argument(index = 1, name = "portNumberString", description = "PortNumber",
              required = false, multiValued = false)
    String portNumberStr = null;



    private DeviceService deviceService;
    private ResourceService resourceService;

    @Override
    protected void execute() {
        deviceService = get(DeviceService.class);
        resourceService = get(ResourceService.class);


        if (deviceIdStr != null && portNumberStr != null) {
            DeviceId deviceId = deviceId(deviceIdStr);
            PortNumber portNumber = PortNumber.fromString(portNumberStr);

            printAllocation(deviceId, portNumber, 0);
        } else if (deviceIdStr != null) {
            DeviceId deviceId = deviceId(deviceIdStr);

            printAllocation(deviceId, 0);
        } else {
            printAllocation();
        }

    }

    private void printAllocation() {
        print("ROOT");
        StreamSupport.stream(deviceService.getAvailableDevices().spliterator(), false)
            .map(Device::id)
            .forEach(did -> printAllocation(did, 1));
    }

    private void printAllocation(DeviceId did, int level) {
        print("%s%s", Strings.repeat(" ", level), did);
        StreamSupport.stream(deviceService.getPorts(did).spliterator(), false)
            .map(Port::number)
            .forEach(num -> printAllocation(did, num, level + 1));
    }

    private void printAllocation(DeviceId did, PortNumber num, int level) {
        if (level == 0) {
            // print DeviceId when Port was directly specified.
            print("%s", did);
        }
        print("%s%s", Strings.repeat(" ", level), asVerboseString(num));

        // TODO: Current design cannot deal with sub-resources
        //        (e.g., TX/RX under Port)

        ResourcePath path = ResourcePath.discrete(did, num);
        if (lambda) {
            //print("Lambda resources:");
            Collection<ResourceAllocation> allocations
                = resourceService.getResourceAllocations(path, OchSignal.class);

            for (ResourceAllocation a : allocations) {
                print("%s%s allocated by %s", Strings.repeat(" ", level + 1),
                                          a.resource().last(), asVerboseString(a.consumer()));
            }
        }
    }

    /**
     * Add type name if the toString does not start with them.
     *
     * e.g., IntentId#toString result in "42"
     *       asVerboseString(id) will result in "IntentId:42"
     *
     * @param obj non-null Object to print.
     * @return verbose String representation
     */
    private static String asVerboseString(Object obj) {
        String name = obj.getClass().getSimpleName();
        String toString = String.valueOf(obj);
        if (toString.startsWith(name)) {
            return toString;
        } else {
            return String.format("%s:%s", name, toString);
        }
    }

}
