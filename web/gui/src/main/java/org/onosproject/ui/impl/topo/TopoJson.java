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
 *
 */

package org.onosproject.ui.impl.topo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.ui.topo.ButtonId;
import org.onosproject.ui.topo.DeviceHighlight;
import org.onosproject.ui.topo.Highlights;
import org.onosproject.ui.topo.HostHighlight;
import org.onosproject.ui.topo.LinkHighlight;
import org.onosproject.ui.topo.PropertyPanel;

/**
 * JSON utilities for the Topology View.
 */
public final class TopoJson {
    private static final String DEVICES = "devices";
    private static final String HOSTS = "hosts";
    private static final String LINKS = "links";

    private static final String ID = "id";
    private static final String LABEL = "label";
    private static final String CSS = "css";

    private static final String TITLE = "title";
    private static final String TYPE = "type";
    private static final String PROP_ORDER = "propOrder";
    private static final String PROPS = "props";
    private static final String BUTTONS = "buttons";


    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static ObjectNode objectNode() {
        return MAPPER.createObjectNode();
    }

    private static ArrayNode arrayNode() {
        return MAPPER.createArrayNode();
    }

    // non-instantiable
    private TopoJson() { }

    /**
     * Transforms the given highlights model into a JSON message payload.
     *
     * @param highlights the model to transform
     * @return JSON payload
     */
    public static ObjectNode json(Highlights highlights) {
        ObjectNode payload = objectNode();

        ArrayNode devices = arrayNode();
        ArrayNode hosts = arrayNode();
        ArrayNode links = arrayNode();

        payload.set(DEVICES, devices);
        payload.set(HOSTS, hosts);
        payload.set(LINKS, links);

        highlights.devices().forEach(dh -> devices.add(json(dh)));
        highlights.hosts().forEach(hh -> hosts.add(json(hh)));
        highlights.links().forEach(lh -> links.add(json(lh)));

        return payload;
    }

    private static ObjectNode json(DeviceHighlight dh) {
        // TODO: implement this once we know what a device highlight looks like
        return objectNode();
    }

    private static ObjectNode json(HostHighlight hh) {
        // TODO: implement this once we know what a host highlight looks like
        return objectNode();
    }

    private static ObjectNode json(LinkHighlight lh) {
        return objectNode()
                .put(ID, lh.elementId())
                .put(LABEL, lh.label())
                .put(CSS, lh.cssClasses());
    }

    /**
     * Translates the given property panel into JSON, for returning
     * to the client.
     *
     * @param pp the property panel model
     * @return JSON payload
     */
    public static ObjectNode json(PropertyPanel pp) {
        ObjectNode result = objectNode()
                .put(TITLE, pp.title())
                .put(TYPE, pp.typeId())
                .put(ID, pp.id());

        ObjectNode pnode = objectNode();
        ArrayNode porder = arrayNode();
        for (PropertyPanel.Prop p : pp.properties()) {
            porder.add(p.key());
            pnode.put(p.key(), p.value());
        }
        result.set(PROP_ORDER, porder);
        result.set(PROPS, pnode);

        ArrayNode buttons = arrayNode();
        for (ButtonId b : pp.buttons()) {
            buttons.add(b.id());
        }
        result.set(BUTTONS, buttons);
        return result;
    }

}
