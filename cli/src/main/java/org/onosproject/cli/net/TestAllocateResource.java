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

import java.util.Optional;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.DeviceId;
import org.onosproject.net.GridType;
import org.onosproject.net.OchSignal;
import org.onosproject.net.PortNumber;
import org.onosproject.net.intent.IntentId;
import org.onosproject.net.resource.ResourceAllocation;
import org.onosproject.net.resource.ResourceConsumer;
import org.onosproject.net.resource.Resource;
import org.onosproject.net.resource.ResourceService;
import org.onosproject.net.resource.Resources;

/**
 * Test tool to allocate resources.
 */
@Command(scope = "onos", name = "test-allocate-resources",
         description = "Test tool to allocate resources")
public class TestAllocateResource extends AbstractShellCommand {

    // TODO add support for other resource types

    // FIXME provide a proper way to specify a lambda and lambda ranges
    @Option(name = "-l", aliases = "--lambda",
            description = "Lambda Resource to allocate",
            required = false, multiValued = false)
    private String lambda = "1";

    @Option(name = "-i", aliases = "--intentId",
            description = "IntentId to use for allocation",
            required = false, multiValued = false)
    private int nIntendId = 42;


    @Argument(index = 0, name = "deviceId", description = "Device ID",
            required = true, multiValued = false)
    String deviceIdStr = null;

    @Argument(index = 1, name = "portNumber", description = "PortNumber",
            required = true, multiValued = false)
    String portNumberStr = null;

    private ResourceService resourceService;

    @Override
    protected void execute() {
        resourceService = get(ResourceService.class);
        DeviceId did = DeviceId.deviceId(deviceIdStr);
        PortNumber portNum = PortNumber.fromString(portNumberStr);

        ResourceConsumer consumer = IntentId.valueOf(nIntendId);

        Resource resource = Resources.discrete(did, portNum,
                createLambda(Integer.parseInt(lambda))).resource();

        Optional<ResourceAllocation> allocate = resourceService.allocate(consumer, resource);
        if (allocate.isPresent()) {
            print("Allocated: %s", allocate.get());
        } else {
            print("Failed to allocate %s for %s", resource, consumer);
        }
    }

    private OchSignal createLambda(int i) {
        return new OchSignal(GridType.FLEX,
                             ChannelSpacing.CHL_6P25GHZ,
                             i,
                             1);
    }

}
