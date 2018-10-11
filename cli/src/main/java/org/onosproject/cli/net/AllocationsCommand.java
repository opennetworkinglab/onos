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

import static org.onosproject.net.DeviceId.deviceId;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.action.Option;
import org.onlab.packet.MplsLabel;
import org.onlab.packet.VlanId;
import org.onlab.util.Bandwidth;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.OchSignal;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.TributarySlot;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.intent.IntentId;
import org.onosproject.net.resource.ResourceConsumerId;
import org.onosproject.net.resource.Resources;
import org.onosproject.net.resource.DiscreteResourceId;
import org.onosproject.net.resource.ResourceAllocation;
import org.onosproject.net.resource.ResourceService;

/**
 * Lists allocated resources.
 */
@Service
@Command(scope = "onos", name = "allocations",
         description = "Lists allocated resources")
public class AllocationsCommand extends AbstractShellCommand {

    @Option(name = "-t", aliases = "--type",
            description = "resource types to include in the list",
            required = false, multiValued = true)
    String[] typeStrings = null;

    Set<String> typesToPrint;

    @Option(name = "-i", aliases = "--intentId",
            description = "Intent ID to include in the list",
            required = false, multiValued = true)
    String[] intentStrings;

    Set<String> intentsToPrint;

    @Argument(index = 0, name = "deviceIdString", description = "Device ID",
              required = false, multiValued = false)
    @Completion(DeviceIdCompleter.class)
    String deviceIdStr = null;

    @Argument(index = 1, name = "portNumberString", description = "PortNumber",
              required = false, multiValued = false)
    @Completion(PortNumberCompleter.class)
    String portNumberStr = null;



    private DeviceService deviceService;
    private ResourceService resourceService;

    @Override
    protected void doExecute() {
        deviceService = get(DeviceService.class);
        resourceService = get(ResourceService.class);

        if (typeStrings != null) {
            typesToPrint = new HashSet<>(Arrays.asList(typeStrings));
        } else {
            typesToPrint = Collections.emptySet();
        }

        if (intentStrings != null) {
            intentsToPrint = new HashSet<>(Arrays.asList(intentStrings));
        } else {
            intentsToPrint = Collections.emptySet();
        }

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

        DiscreteResourceId resourceId = Resources.discrete(did, num).id();

        List<String> portConsumers = resourceService.getResourceAllocations(resourceId)
            .stream()
            .filter(this::isSubjectToPrint)
            .map(ResourceAllocation::consumerId)
            .map(AllocationsCommand::asVerboseString)
            .collect(Collectors.toList());
        if (portConsumers.isEmpty()) {
            print("%s%s", Strings.repeat(" ", level), asVerboseString(num));
        } else {
            print("%s%s allocated by %s", Strings.repeat(" ", level), asVerboseString(num),
                                        portConsumers);
        }

        // FIXME: This workaround induces a lot of distributed store access.
        //        ResourceService should have an API to get all allocations under a parent resource.
        Set<Class<?>> subResourceTypes = ImmutableSet.<Class<?>>builder()
                .add(OchSignal.class)
                .add(VlanId.class)
                .add(MplsLabel.class)
                .add(Bandwidth.class)
                .add(TributarySlot.class)
                .build();

        for (Class<?> t : subResourceTypes) {
            resourceService.getResourceAllocations(resourceId, t).stream()
                    .filter(a -> isSubjectToPrint(a))
                    .forEach(a -> print("%s%s allocated by %s", Strings.repeat(" ", level + 1),
                            a.resource().valueAs(Object.class).orElse(""), asVerboseString(a.consumerId())));

        }
    }

    private boolean isSubjectToPrint(ResourceAllocation allocation) {
        if (!intentsToPrint.isEmpty()
                && allocation.consumerId().isClassOf(IntentId.class)
                && !intentsToPrint.contains(allocation.consumerId().toString())) {
            return false;
        }

        if (!typesToPrint.isEmpty()
                && !typesToPrint.contains(allocation.resource().simpleTypeName())) {
            return false;
        }

        return true;
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

    private static String asVerboseString(ResourceConsumerId consumerId) {
        return String.format("%s:%s", consumerId.consumerClass(), consumerId.value());
    }

}
