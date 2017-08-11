/*
 * Copyright 2015-present Open Networking Foundation
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

package org.onosproject.ui.topo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.onosproject.ui.JsonUtils.envelope;

/**
 * JSON utilities for the Topology View.
 */
public final class TopoJson {
    // package-private for unit test access
    static final String SHOW_HIGHLIGHTS = "showHighlights";
    static final String TOPO2_HIGHLIGHTS = "topo2Highlights";

    static final String DEVICES = "devices";
    static final String HOSTS = "hosts";
    static final String LINKS = "links";
    static final String SUBDUE = "subdue";
    static final String DELAY = "delay";

    static final String ID = "id";
    static final String LABEL = "label";
    static final String CSS = "css";
    static final String BADGE = "badge";
    static final String STATUS = "status";
    static final String TXT = "txt";
    static final String GID = "gid";
    static final String MSG = "msg";

    static final String TITLE = "title";
    static final String GLYPH_ID = "glyphId";
    static final String NAV_PATH = "navPath";
    static final String PROP_ORDER = "propOrder";
    static final String PROP_LABELS = "propLabels";
    static final String PROP_VALUES = "propValues";
    static final String BUTTONS = "buttons";


    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static ObjectNode objectNode() {
        return MAPPER.createObjectNode();
    }

    private static ArrayNode arrayNode() {
        return MAPPER.createArrayNode();
    }

    // non-instantiable
    private TopoJson() {
    }

    /**
     * Returns a formatted message ready to send to the topology view
     * to render highlights.
     *
     * @param highlights highlights model to transform
     * @return fully formatted "show highlights" message
     */
    public static ObjectNode highlightsMessage(Highlights highlights) {
        return envelope(SHOW_HIGHLIGHTS, json(highlights));
    }

    /**
     * Returns a formatted message ready to send to the topology-2 view
     * to render highlights.
     *
     * @param highlights highlights model to transform
     * @return fully formatted "show highlights" message
     */
    public static ObjectNode topo2HighlightsMessage(Highlights highlights) {
        return envelope(TOPO2_HIGHLIGHTS, json(highlights));
    }

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

        Highlights.Amount toSubdue = highlights.subdueLevel();
        if (!toSubdue.equals(Highlights.Amount.ZERO)) {
            payload.put(SUBDUE, toSubdue.toString());
        }
        int delay = highlights.delayMs();
        if (delay > 0) {
            payload.put(DELAY, delay);
        }
        return payload;
    }

    private static ObjectNode json(NodeBadge b) {
        ObjectNode n = objectNode()
                .put(STATUS, b.status().code())
                .put(b.isGlyph() ? GID : TXT, b.text());
        if (b.message() != null) {
            n.put(MSG, b.message());
        }
        return n;
    }

    private static ObjectNode json(DeviceHighlight dh) {
        ObjectNode n = objectNode()
                .put(ID, dh.elementId());
        if (dh.subdued()) {
            n.put(SUBDUE, true);
        }
        NodeBadge badge = dh.badge();
        if (badge != null) {
            n.set(BADGE, json(badge));
        }
        return n;
    }

    private static ObjectNode json(HostHighlight hh) {
        ObjectNode n = objectNode()
                .put(ID, hh.elementId());
        if (hh.subdued()) {
            n.put(SUBDUE, true);
        }
        NodeBadge badge = hh.badge();
        if (badge != null) {
            n.set(BADGE, json(badge));
        }
        return n;
    }

    private static ObjectNode json(LinkHighlight lh) {
        ObjectNode n = objectNode()
                .put(ID, lh.elementId())
                .put(LABEL, lh.label())
                .put(CSS, lh.cssClasses());
        if (lh.subdued()) {
            n.put(SUBDUE, true);
        }
        return n;
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
                .put(GLYPH_ID, pp.glyphId())
                .put(ID, pp.id());

        if (pp.navPath() != null) {
            result.put(NAV_PATH, pp.navPath());
        }

        ObjectNode plabels = objectNode();
        ObjectNode pvalues = objectNode();
        ArrayNode porder = arrayNode();
        for (PropertyPanel.Prop p : pp.properties()) {
            porder.add(p.key());
            plabels.put(p.key(), p.label());
            pvalues.put(p.key(), p.value());
        }
        result.set(PROP_ORDER, porder);
        result.set(PROP_LABELS, plabels);
        result.set(PROP_VALUES, pvalues);

        ArrayNode buttons = arrayNode();
        for (ButtonId b : pp.buttons()) {
            buttons.add(b.id());
        }
        result.set(BUTTONS, buttons);
        return result;
    }

}
