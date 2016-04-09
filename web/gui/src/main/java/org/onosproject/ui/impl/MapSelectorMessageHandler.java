/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.ui.impl;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiMessageHandler;

import java.util.Collection;
import java.util.List;

/**
 * Message handler for map selection functionality.
 */
class MapSelectorMessageHandler extends UiMessageHandler {

    private static final String MAP_LIST_REQ = "mapSelectorRequest";
    private static final String MAP_LIST_RESP = "mapSelectorResponse";

    private static final String ORDER = "order";
    private static final String MAPS = "maps";
    private static final String MAP_ID = "id";
    private static final String DESCRIPTION = "description";
    private static final String SCALE = "scale";

    private static final List<Map> SUPPORTED_MAPS =
            ImmutableList.of(new Map("australia", "Australia", 1.0),
                    new Map("ns_america", "North, Central and South America", 0.7),
                    new Map("s_america", "South America", 0.9),
                    new Map("usa", "United States", 1.0),
                    new Map("bayarea", "Bay Area, California", 1.0),
                    new Map("europe", "Europe", 2.5),
                    new Map("italy", "Italy", 0.8),
                    new Map("uk", "United Kingdom and Ireland", 0.6),
                    new Map("japan", "Japan", 0.8),
                    new Map("s_korea", "South Korea", 0.75),
                    new Map("taiwan", "Taiwan", 0.7));

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(
                new MapListHandler()
        );
    }

    private final class MapListHandler extends RequestHandler {
        private MapListHandler() {
            super(MAP_LIST_REQ);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            sendMessage(MAP_LIST_RESP, 0, mapsJson());
        }
    }

    private ObjectNode mapsJson() {
        ObjectNode payload = objectNode();
        ArrayNode order = arrayNode();
        ObjectNode maps = objectNode();
        payload.set(ORDER, order);
        payload.set(MAPS, maps);
        SUPPORTED_MAPS.forEach(m -> {
            maps.set(m.id, objectNode().put(MAP_ID, m.id)
                    .put(DESCRIPTION, m.description)
                    .put(SCALE, m.scale));
            order.add(m.id);
        });
        return payload;
    }

    private static final class Map {
        private final String id;
        private final String description;
        private final double scale;

        private Map(String id, String description, double scale) {
            this.id = id;
            this.description = description;
            this.scale = scale;
        }
    }

}