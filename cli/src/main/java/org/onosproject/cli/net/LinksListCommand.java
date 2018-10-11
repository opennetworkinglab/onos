/*
 * Copyright 2014-present Open Networking Foundation
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

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onlab.util.Tools;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.Link;
import org.onosproject.net.link.LinkService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.LinkKey.linkKey;

import java.util.Comparator;

/**
 * Lists all infrastructure links.
 */
@Service
@Command(scope = "onos", name = "links",
         description = "Lists all infrastructure links")
public class LinksListCommand extends AbstractShellCommand {

    private static final String FMT = "src=%s/%s, dst=%s/%s, type=%s, state=%s%s, expected=%s";
    private static final String COMPACT = "%s/%s-%s/%s";

    @Argument(index = 0, name = "uri", description = "Device ID",
              required = false, multiValued = false)
    @Completion(DeviceIdCompleter.class)
    String uri = null;

    @Override
    protected void doExecute() {
        LinkService service = get(LinkService.class);
        Iterable<Link> links = uri != null ?
                service.getDeviceLinks(deviceId(uri)) : service.getLinks();
        if (outputJson()) {
            print("%s", json(this, links));
        } else {
            Tools.stream(links)
                .sorted(Comparator.comparing(link -> linkKey(link).toString()))
                .forEach(link -> {
                print(linkString(link));
            });
        }
    }

    /**
     * Produces a JSON array containing the specified links.
     *
     * @param context context to use for looking up codecs
     * @param links collection of links
     * @return JSON array
     */
    public static JsonNode json(AbstractShellCommand context, Iterable<Link> links) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();

        links.forEach(link -> result.add(context.jsonForEntity(link, Link.class)));

        return result;
    }

    /**
     * Produces a JSON object for the specified link.
     *
     * @param context context to use for looking up codecs
     * @param link   link to encode
     * @return JSON object
     */
    public static ObjectNode json(AbstractShellCommand context, Link link) {
         return context.jsonForEntity(link, Link.class);
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
                             annotations(link.annotations()),
                             link.isExpected());
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
