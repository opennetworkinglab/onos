/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onosproject.cordsupport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.net.Device;
import org.onosproject.net.Host;
import org.onosproject.net.Link;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.link.LinkService;
import org.onosproject.rest.AbstractWebResource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.util.HashSet;
import java.util.Set;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

/**
 * CORD support REST implementation.
 */
@Path("topo")
public class CordSupportWebResource extends AbstractWebResource {

    private static final String TILDE = "~";

    private final ObjectMapper mapper = new ObjectMapper();
    private final Set<String> seenLinks = new HashSet<>();

    @GET
    public Response queryTopology() {
        ObjectNode root = mapper.createObjectNode();

        addDevices(root);
        addHosts(root);
        addLinks(root);

        return Response.ok(root.toString(), APPLICATION_JSON_TYPE).build();
    }

    private void addDevices(ObjectNode root) {
        ArrayNode devices = createArray(root, "devices");
        get(DeviceService.class).getDevices()
                .forEach(dev -> devices.add(json(dev)));
    }

    private void addHosts(ObjectNode root) {
        ArrayNode hosts = createArray(root, "hosts");
        get(HostService.class).getHosts()
                .forEach(host -> hosts.add(json(host)));
    }

    private void addLinks(ObjectNode root) {
        ArrayNode links = createArray(root, "links");
        seenLinks.clear();
        get(LinkService.class).getLinks()
                .forEach(link -> {
                    String canon = canonicalRep(link);
                    if (!seenLinks.contains(canon)) {
                        seenLinks.add(canon);
                        links.add(json(link));
                    }
                });
    }

    private String canonicalRep(Link link) {
        String a = link.src().toString();
        String b = link.dst().toString();
        return a.compareTo(b) < 0 ? a + TILDE + b : b + TILDE + a;
    }

    private ArrayNode createArray(ObjectNode root, String key) {
        ArrayNode result = mapper.createArrayNode();
        root.set(key, result);
        return result;
    }

    private ObjectNode json(Device device) {
        return mapper.createObjectNode()
                .put("id", device.id().toString())
                .put("type", device.type().toString());
    }

    private ObjectNode json(Host host) {
        return mapper.createObjectNode()
                .put("id", host.id().toString())
                .put("location", host.location().toString());
    }

    private ObjectNode json(Link link) {
        return mapper.createObjectNode()
                .put("canon", canonicalRep(link))
                .put("src", link.src().toString())
                .put("dst", link.dst().toString());
    }

}
