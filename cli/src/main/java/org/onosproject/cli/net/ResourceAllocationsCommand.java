/*
 * Copyright 2014 Open Networking Laboratory
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

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.resource.LinkResourceAllocations;
import org.onosproject.net.resource.LinkResourceService;

import static org.onosproject.cli.net.AddPointToPointIntentCommand.getDeviceId;
import static org.onosproject.cli.net.AddPointToPointIntentCommand.getPortNumber;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.PortNumber.portNumber;

/**
 * Lists allocations by link.
 */
@Command(scope = "onos", name = "resource-allocations",
         description = "Lists allocations by link")
public class ResourceAllocationsCommand extends AbstractShellCommand {

    private static final String FMT = "src=%s/%s, dst=%s/%s, type=%s%s";
    private static final String COMPACT = "%s/%s-%s/%s";

    @Argument(index = 0, name = "srcString", description = "Link source",
              required = false, multiValued = false)
    String srcString = null;
    @Argument(index = 1, name = "dstString", description = "Link destination",
              required = false, multiValued = false)
    String dstString = null;

    @Override
    protected void execute() {
        LinkResourceService resourceService = get(LinkResourceService.class);
        LinkService linkService = get(LinkService.class);

        Iterable<LinkResourceAllocations> itr = null;
        try {
            DeviceId ingressDeviceId = deviceId(getDeviceId(srcString));
            PortNumber ingressPortNumber = portNumber(getPortNumber(srcString));
            ConnectPoint src = new ConnectPoint(ingressDeviceId, ingressPortNumber);

            DeviceId egressDeviceId = deviceId(getDeviceId(dstString));
            PortNumber egressPortNumber = portNumber(getPortNumber(dstString));
            ConnectPoint dst = new ConnectPoint(egressDeviceId, egressPortNumber);

            Link link = linkService.getLink(src, dst);

            itr = resourceService.getAllocations(link);

            for (LinkResourceAllocations allocation : itr) {
                print("%s", allocation.getResourceAllocation(link));
            }

        } catch (Exception e) {
            print("----- Displaying all resource allocations -----", e.getMessage());
            itr = resourceService.getAllocations();
            for (LinkResourceAllocations allocation : itr) {
                print("%s", allocation);
            }

        }
    }
}
