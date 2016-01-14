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

import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.newresource.Resource;
import org.onosproject.net.newresource.ResourceService;

import com.google.common.base.Strings;

/**
 * Lists available resources.
 */
@Command(scope = "onos", name = "resources",
         description = "Lists available resources")
public class ResourcesCommand extends AbstractShellCommand {

    @Option(name = "-s", aliases = "--sort", description = "Sort output",
            required = false, multiValued = false)
    boolean sort = false;

    @Option(name = "-t", aliases = "--typeStrings", description = "List of resource types to be printed",
            required = false, multiValued = true)
    String[] typeStrings = null;

    Set<String> typesToPrint;

    @Argument(index = 0, name = "deviceIdString", description = "Device ID",
              required = false, multiValued = false)
    String deviceIdStr = null;

    @Argument(index = 1, name = "portNumberString", description = "PortNumber",
              required = false, multiValued = false)
    String portNumberStr = null;


    private ResourceService resourceService;

    @Override
    protected void execute() {
        resourceService = get(ResourceService.class);

        if (typeStrings != null) {
            typesToPrint = new HashSet<>(Arrays.asList(typeStrings));
        } else {
            typesToPrint = Collections.emptySet();
        }

        if (deviceIdStr != null && portNumberStr != null) {
            DeviceId deviceId = deviceId(deviceIdStr);
            PortNumber portNumber = PortNumber.fromString(portNumberStr);

            printResource(Resource.discrete(deviceId, portNumber), 0);
        } else if (deviceIdStr != null) {
            DeviceId deviceId = deviceId(deviceIdStr);

            printResource(Resource.discrete(deviceId), 0);
        } else {
            printResource(Resource.ROOT, 0);
        }
    }

    private void printResource(Resource resource, int level) {
        Collection<Resource> children = resourceService.getAvailableResources(resource);

        if (resource.equals(Resource.ROOT)) {
            print("ROOT");
        } else {
            String resourceName = resource.last().getClass().getSimpleName();

            if (children.isEmpty() && !typesToPrint.isEmpty() && !typesToPrint.contains(resourceName)) {
                // This resource is target of filtering
                return;
            }

            String toString = String.valueOf(resource.last());
            if (toString.startsWith(resourceName)) {
                print("%s%s", Strings.repeat(" ", level),
                              toString);
            } else {
                print("%s%s: %s", Strings.repeat(" ", level),
                                 resourceName,
                                 toString);
            }
        }

        if (sort) {
            children.stream()
                    .sorted((o1, o2) -> String.valueOf(o1.id()).compareTo(String.valueOf(o2.id())))
                    .forEach(r -> printResource(r, level + 1));
        } else {
            // TODO: Should consider better output for leaf nodes
            children.forEach(r -> printResource(r, level + 1));
        }
    }
}
