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
import org.onosproject.net.Link;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.resource.link.LinkResourceService;

/**
 * Lists allocations by link. Lists all allocations if link is unspecified.
 *
 * @deprecated in Emu release
 */
@Deprecated
@Command(scope = "onos", name = "resource-allocations",
        description = "Lists allocations by link. Lists all allocations if link is unspecified."
                    + "[Using deprecated LinkResourceService]")
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

        if (srcString == null || dstString == null) {
            print("----- Displaying all resource allocations -----");
            resourceService.getAllocations().forEach(alloc -> print("%s", alloc));
            return;
        }

        ConnectPoint src = ConnectPoint.deviceConnectPoint(srcString);
        ConnectPoint dst = ConnectPoint.deviceConnectPoint(dstString);

        Link link = linkService.getLink(src, dst);
        if (link != null) {
            resourceService.getAllocations(link).forEach(alloc -> print("%s", alloc));
        } else {
            print("No path found for endpoints: %s, %s", src, dst);
        }
    }
}
