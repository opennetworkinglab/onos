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
import org.onosproject.mapping.MappingTreatment;
import org.onosproject.mapping.MappingValue;
import org.onosproject.mapping.addresses.MappingAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.table.CellFormatter;
import org.onosproject.ui.table.TableModel;
import org.onosproject.ui.table.TableRequestHandler;
import org.onosproject.ui.table.cell.EnumFormatter;
import org.onosproject.ui.table.cell.HexLongFormatter;

import java.util.Collection;
import java.util.List;

import static org.onosproject.mapping.MappingStore.Type.MAP_CACHE;
import static org.onosproject.mapping.MappingStore.Type.MAP_DATABASE;

/**
 * Message handler for mapping management view related messages.
 */
public class MappingsViewMessageHandler extends UiMessageHandler {

    private static final String MAPPING_DATA_REQ = "mappingDataRequest";
    private static final String MAPPING_DATA_RESP = "mappingDataResponse";
    private static final String MAPPINGS = "mappings";

    private static final String ID = "id";
    private static final String MAPPING_KEY = "mappingKey";
    private static final String MAPPING_VALUE = "mappingValue";
    private static final String MAPPING_ACTION = "mappingAction";
    private static final String TYPE = "type";
    private static final String STATE = "state";
    private static final String DATABASE = "database";
    private static final String CACHE = "cache";

    private static final String COMMA = ", ";
    private static final String OX = "0x";
    private static final String EMPTY = "";

    private static final String NULL_ADDRESS_MSG = "(No mapping address for this mapping)";

    private static final String[] COL_IDS = {
            ID, MAPPING_KEY, MAPPING_VALUE, STATE, MAPPING_ACTION, TYPE
    };

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(new MappingMessageRequest());
    }

    /**
     * Handler for mapping message requests.
     */
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
        protected TableModel createTableModel() {
            TableModel tm = super.createTableModel();
            tm.setFormatter(ID, HexLongFormatter.INSTANCE);
            tm.setFormatter(TYPE, EnumFormatter.INSTANCE);
            tm.setFormatter(STATE, EnumFormatter.INSTANCE);
            tm.setFormatter(MAPPING_KEY, new MappingKeyFormatter());
            tm.setFormatter(MAPPING_VALUE, new MappingValueFormatter());
            return tm;
        }

        @Override
        protected void populateTable(TableModel tm, ObjectNode payload) {
            String uri = string(payload, "devId");
            if (!Strings.isNullOrEmpty(uri)) {
                DeviceId deviceId = DeviceId.deviceId(uri);
                MappingService ms = get(MappingService.class);

                for (MappingEntry mapping : ms.getMappingEntries(MAP_DATABASE, deviceId)) {
                    populateRow(tm.addRow(), mapping, DATABASE);
                }

                for (MappingEntry mapping : ms.getMappingEntries(MAP_CACHE, deviceId)) {
                    populateRow(tm.addRow(), mapping, CACHE);
                }
            }
        }

        private void populateRow(TableModel.Row row, MappingEntry mapping,
                                         String type) {
            row.cell(ID, mapping.id().value())
                    .cell(STATE, mapping.state())
                    .cell(TYPE, type)
                    .cell(MAPPING_ACTION, mapping.value().action())
                    .cell(MAPPING_KEY, mapping)
                    .cell(MAPPING_VALUE, mapping);
        }
    }

    /**
     * A formatter for formatting mapping key.
     */
    private final class MappingKeyFormatter implements CellFormatter {

        @Override
        public String format(Object value) {
            MappingEntry mapping = (MappingEntry) value;
            MappingAddress address = mapping.key().address();

            if (address == null) {
                return NULL_ADDRESS_MSG;
            }
            StringBuilder sb = new StringBuilder("Mapping address: ");
            sb.append(address.toString());

            return sb.toString();
        }
    }

    /**
     * A formatter for formatting mapping value.
     */
    private final class MappingValueFormatter implements CellFormatter {

        @Override
        public String format(Object value) {
            MappingEntry mapping = (MappingEntry) value;
            MappingValue mappingValue = mapping.value();
            List<MappingTreatment> treatments = mappingValue.treatments();

            StringBuilder sb = new StringBuilder("Treatments: ");
            formatTreatments(sb, treatments);

            return sb.toString();
        }

        private void formatTreatments(StringBuilder sb,
                                      List<MappingTreatment> treatments) {
            if (!treatments.isEmpty()) {
                for (MappingTreatment t : treatments) {
                    sb.append(t).append(COMMA);
                }
            }
        }
    }
}
