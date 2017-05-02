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
package org.onosproject.mapping.web.gui;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import org.onosproject.mapping.MappingEntry;
import org.onosproject.mapping.MappingService;
import org.onosproject.net.DeviceId;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.table.TableModel;
import org.onosproject.ui.table.TableRequestHandler;

import java.util.Collection;

import static org.onosproject.mapping.MappingStore.Type.MAP_DATABASE;

/**
 * Message handler for mapping management view related messages.
 */
public class MappingsViewMessageHandler extends UiMessageHandler {

    private static final String MAPPING_DATA_REQ = "mappingDataRequest";
    private static final String MAPPING_DATA_RESP = "mappingDataResponse";
    private static final String MAPPINGS = "mappings";

    private static final String ID = "id";

    private static final String[] COL_IDS = {ID};

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(new MappingMessageRequest());
    }

    private final class MappingMessageRequest extends TableRequestHandler {

        private static final String NO_ROWS_MESSAGE = "No mappings found";

        private MappingMessageRequest() {
            super(MAPPING_DATA_REQ, MAPPING_DATA_RESP, MAPPINGS);
        }

        @Override
        protected String[] getColumnIds() {
            return COL_IDS;
        }

        @Override
        protected String noRowsMessage(ObjectNode payload) {
            return NO_ROWS_MESSAGE;
        }

        @Override
        protected void populateTable(TableModel tm, ObjectNode payload) {
            String uri = string(payload, "devId");
            if (!Strings.isNullOrEmpty(uri)) {
                DeviceId deviceId = DeviceId.deviceId(uri);
                MappingService ms = get(MappingService.class);
                for (MappingEntry mapping : ms.getMappingEntries(MAP_DATABASE, deviceId)) {
                    populateRow(tm.addRow(), mapping);
                }
            }
        }

        private void populateRow(TableModel.Row row, MappingEntry mapping) {
            row.cell(ID, mapping.id().value());
        }
    }
}
