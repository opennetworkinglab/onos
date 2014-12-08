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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.HostId;
import org.onosproject.net.Link;
import org.onosproject.net.link.LinkService;

import static org.onosproject.net.DeviceId.deviceId;

/**
 * Lists all infrastructure links.
 */
@Command(scope = "onos", name = "links",
         description = "Lists all infrastructure links")
public class LinksListCommand extends AbstractShellCommand {

    private static final String FMT = "src=%s/%s, dst=%s/%s, type=%s, state=%s%s";
    private static final String COMPACT = "%s/%s-%s/%s";

    @Argument(index = 0, name = "uri", description = "Device ID",
              required = false, multiValued = false)
    String uri = null;

    @Override
    protected void execute() {
        LinkService service = get(LinkService.class);
        Iterable<Link> links = uri != null ?
                service.getDeviceLinks(deviceId(uri)) : service.getLinks();
        if (outputJson()) {
            print("%s", json(links));
        } else {
            for (Link link : links) {
                print(linkString(link));
            }
        }
    }

    /**
     * Produces a JSON array containing the specified links.
     *
     * @param links collection of links
     * @return JSON array
     */
    public static JsonNode json(Iterable<Link> links) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();
        for (Link link : links) {
            result.add(json(mapper, link));
        }
        return result;
    }

    /**
     * Produces a JSON object for the specified link.
     *
     * @param mapper object mapper
     * @param link   link to encode
     * @return JSON object
     */
    public static ObjectNode json(ObjectMapper mapper, Link link) {
        ObjectNode result = mapper.createObjectNode();
        result.set("src", json(mapper, link.src()));
        result.set("dst", json(mapper, link.dst()));
        result.put("type", link.type().toString());
        result.put("state", link.state().toString());
        result.set("annotations", annotations(mapper, link.annotations()));
        return result;
    }

    /**
     * Produces a JSON object for the specified host ID.
     *
     * @param mapper    object mapper
     * @param hostId    host ID to encode
     * @return JSON object
     */
    public static ObjectNode json(ObjectMapper mapper, HostId hostId) {
        return mapper.createObjectNode()
                .put("mac", hostId.mac().toString())
                .put("vlanId", hostId.vlanId().toString());
    }

    /**
     * Produces a JSON object for the specified connect point.
     *
     * @param mapper       object mapper
     * @param connectPoint connection point to encode
     * @return JSON object
     */
    public static ObjectNode json(ObjectMapper mapper, ConnectPoint connectPoint) {
        return mapper.createObjectNode()
                .put("device", connectPoint.deviceId().toString())
                .put("port", connectPoint.port().toString());
    }

    /**
     * Returns a formatted string representing the given link.
     *
     * @param link infrastructure link
     * @return formatted link string
     */
    public static String linkString(Link link) {
        return String.format(FMT, link.src().deviceId(), link.src().port(),
                             link.dst().deviceId(), link.dst().port(),
                             link.type(), link.state(),
                             annotations(link.annotations()));
    }

    /**
     * Returns a compact string representing the given link.
     *
     * @param link infrastructure link
     * @return formatted link string
     */
    public static String compactLinkString(Link link) {
        return String.format(COMPACT, link.src().deviceId(), link.src().port(),
                             link.dst().deviceId(), link.dst().port());
    }

}
