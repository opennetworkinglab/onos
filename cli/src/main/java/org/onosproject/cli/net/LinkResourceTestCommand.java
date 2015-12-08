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

import com.google.common.collect.Lists;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.intent.IntentId;
import org.onosproject.net.resource.link.DefaultLinkResourceRequest;
import org.onosproject.net.resource.link.LinkResourceAllocations;
import org.onosproject.net.resource.link.LinkResourceRequest;
import org.onosproject.net.resource.link.LinkResourceService;
import org.onosproject.net.topology.PathService;

import java.util.List;
import java.util.Set;

/**
 * Commands to test out LinkResourceManager directly.
 *
 * @deprecated in Emu release
 */
@Deprecated
@Command(scope = "onos", name = "resource-request",
        description = "request or remove resources"
                    + "[Using deprecated LinkResourceService]")
public class LinkResourceTestCommand extends AbstractShellCommand {

    // default is bandwidth.
    @Option(name = "-m", aliases = "--mpls", description = "MPLS resource",
            required = false, multiValued = false)
    private boolean isMpls = false;

    @Option(name = "-o", aliases = "--optical", description = "Optical resource",
            required = false, multiValued = false)
    private boolean isOptical = false;

    @Option(name = "-d", aliases = "--delete", description = "Delete resource by intent ID",
            required = false, multiValued = false)
    private boolean remove = false;

    @Argument(index = 0, name = "srcString", description = "Link source",
            required = true, multiValued = false)
    String srcString = null;

    @Argument(index = 1, name = "dstString", description = "Link destination",
            required = true, multiValued = false)
    String dstString = null;

    @Argument(index = 2, name = "id", description = "Identifier",
            required = true, multiValued = false)
    int id;

    private LinkResourceService resService;
    private PathService pathService;

    private static final int BANDWIDTH = 1_000_000;

    @Override
    protected void execute() {
        resService = get(LinkResourceService.class);
        pathService = get(PathService.class);

        DeviceId src = DeviceId.deviceId(getDeviceId(srcString));
        DeviceId dst = DeviceId.deviceId(getDeviceId(dstString));
        IntentId intId = IntentId.valueOf(id);

        Set<Path> paths = pathService.getPaths(src, dst);

        if (paths == null || paths.isEmpty()) {
            print("No path between %s and %s", srcString, dstString);
            return;
        }

        if (remove) {
            LinkResourceAllocations lra = resService.getAllocations(intId);
            resService.releaseResources(lra);
            return;
        }

        for (Path p : paths) {
            List<Link> links = p.links();
            LinkResourceRequest.Builder request = null;
            if (isMpls) {
                List<Link> nlinks = Lists.newArrayList();
                try {
                    nlinks.addAll(links.subList(1, links.size() - 2));
                    request = DefaultLinkResourceRequest.builder(intId, nlinks)
                            .addMplsRequest();
                } catch (IndexOutOfBoundsException e) {
                    log.warn("could not allocate MPLS path", e);
                    continue;
                }
            } else if (isOptical) {
                request = DefaultLinkResourceRequest.builder(intId, links)
                        .addLambdaRequest();
            } else {
                request = DefaultLinkResourceRequest.builder(intId, links)
                        .addBandwidthRequest(BANDWIDTH);
            }

            if (request != null) {
                LinkResourceRequest lrr = request.build();
                LinkResourceAllocations lra = resService.requestResources(lrr);
                if (lra != null) {
                    break;
                }
                print("Allocated:\n%s", lra);
            } else {
                log.info("nothing to request");
            }
        }
    }

    public String getDeviceId(String deviceString) {
        int slash = deviceString.indexOf('/');
        if (slash <= 0) {
            return "";
        }
        return deviceString.substring(0, slash);
    }

}
