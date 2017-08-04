/*
 * Copyright 2016-present Open Networking Foundation
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
import com.google.common.collect.ImmutableSet;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiExtensionService;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.UiTopoMap;
import org.onosproject.ui.UiTopoMapFactory;

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
    private static final String FILE_PATH = "filePath";
    private static final String SCALE = "scale";

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
        public void process(ObjectNode payload) {
            sendMessage(MAP_LIST_RESP, mapsJson());
        }
    }

    private ObjectNode mapsJson() {

        ObjectNode payload = objectNode();
        ArrayNode order = arrayNode();
        ObjectNode maps = objectNode();
        payload.set(ORDER, order);
        payload.set(MAPS, maps);

        UiExtensionService service = get(UiExtensionService.class);
        service.getExtensions().forEach(ext -> {
            UiTopoMapFactory mapFactory = ext.topoMapFactory();

            if (mapFactory != null) {
                List<UiTopoMap> topoMaps = mapFactory.geoMaps();

                topoMaps.forEach(m -> {
                    maps.set(m.id(), objectNode().put(MAP_ID, m.id())
                            .put(DESCRIPTION, m.description())
                            .put(FILE_PATH, m.filePath())
                            .put(SCALE, m.scale()));
                    order.add(m.id());
                });
            }
        });

        return payload;
    }
}