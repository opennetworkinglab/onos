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
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.newresource.ResourcePath;
import org.onosproject.net.newresource.ResourceService;

import com.google.common.base.Strings;

/**
 * Lists available resources.
 */
@Command(scope = "onos", name = "resources",
         description = "Lists available resources")
public class ResourcesCommand extends AbstractShellCommand {

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

        if (deviceIdStr != null && portNumberStr != null) {
            DeviceId deviceId = deviceId(deviceIdStr);
            PortNumber portNumber = PortNumber.fromString(portNumberStr);

            printResource(ResourcePath.discrete(deviceId, portNumber), 0);
        } else if (deviceIdStr != null) {
            DeviceId deviceId = deviceId(deviceIdStr);

            printResource(ResourcePath.discrete(deviceId), 0);
        } else {
            printResource(ResourcePath.ROOT, 0);
        }
    }

    private void printResource(ResourcePath resource, int level) {
        if (resource.equals(ResourcePath.ROOT)) {
            print("ROOT");
        } else {
            String name = resource.last().getClass().getSimpleName();
            String toString = String.valueOf(resource.last());
            if (toString.startsWith(name)) {
                print("%s%s", Strings.repeat(" ", level),
                              toString);

            } else {
                print("%s%s:%s", Strings.repeat(" ", level),
                                 name,
                                 toString);
            }
        }

        Collection<ResourcePath> resources = resourceService.getAvailableResources(resource);
        // TODO: Should consider better output for leaf nodes
        resources.forEach(r -> printResource(r, level + 1));
    }
}
