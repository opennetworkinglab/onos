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

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import org.onosproject.mapping.MappingEntry;
import org.onosproject.mapping.MappingService;
import org.onosproject.mapping.MappingTreatment;
import org.onosproject.mapping.addresses.MappingAddress;
import org.onosproject.mapping.instructions.MappingInstruction;
import org.onosproject.mapping.instructions.MulticastMappingInstruction;
import org.onosproject.mapping.instructions.MulticastMappingInstruction.MulticastType;
import org.onosproject.mapping.instructions.UnicastMappingInstruction;
import org.onosproject.mapping.instructions.UnicastMappingInstruction.UnicastType;
import org.onosproject.net.DeviceId;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.table.CellFormatter;
import org.onosproject.ui.table.TableModel;
import org.onosproject.ui.table.TableRequestHandler;
import org.onosproject.ui.table.cell.EnumFormatter;
import org.onosproject.ui.table.cell.HexLongFormatter;

import java.util.Collection;

import static org.onosproject.mapping.MappingStore.Type.MAP_CACHE;
import static org.onosproject.mapping.MappingStore.Type.MAP_DATABASE;
import static org.onosproject.mapping.instructions.MappingInstruction.Type.MULTICAST;
import static org.onosproject.mapping.instructions.MappingInstruction.Type.UNICAST;

/**
 * Message handler for mapping management view related messages.
 */
public class MappingsViewMessageHandler extends UiMessageHandler {

    private static final String MAPPING_DATA_REQ = "mappingDataRequest";
    private static final String MAPPING_DATA_RESP = "mappingDataResponse";
    private static final String MAPPINGS = "mappings";

    private static final String MAPPING_DETAIL_REQ = "mappingDetailsRequest";
    private static final String MAPPING_DETAIL_RESP = "mappingDetailsResponse";
    private static final String DETAILS = "details";

    private static final String ID = "id";
    private static final String MAPPING_ID = "mappingId";
    private static final String MAPPING_KEY = "mappingKey";
    private static final String MAPPING_VALUE = "mappingValue";
    private static final String MAPPING_ACTION = "mappingAction";
    private static final String TYPE = "type";
    private static final String STATE = "state";
    private static final String DATABASE = "database";
    private static final String CACHE = "cache";
    private static final String MAPPING_TREATMENTS = "mappingTreatments";

    private static final String MAPPING_ADDRESS = "address";
    private static final String UNICAST_WEIGHT = "unicastWeight";
    private static final String UNICAST_PRIORITY = "unicastPriority";
    private static final String MULTICAST_WEIGHT = "multicastWeight";
    private static final String MULTICAST_PRIORITY = "multicastPriority";

    private static final String COMMA = ", ";
    private static final String OX = "0x";
    private static final String EMPTY = "";

    private static final String NULL_ADDRESS_MSG = "(No mapping address for this mapping)";

    private static final String[] COL_IDS = {
            ID, MAPPING_KEY, MAPPING_VALUE, STATE, MAPPING_ACTION, TYPE
    };

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(
                new MappingMessageRequest(),
                new DetailRequestHandler()
        );
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
                    .cell(MAPPING_KEY, mapping);
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

            return address.toString();
        }
    }

    /**
     * Handler for detailed mapping message requests.
     */
    private final class DetailRequestHandler extends RequestHandler {

        private DetailRequestHandler() {
            super(MAPPING_DETAIL_REQ);
        }

        private MappingEntry findMappingById(String mappingId) {
            MappingService ms = get(MappingService.class);
            Iterable<MappingEntry> dbEntries = ms.getAllMappingEntries(MAP_DATABASE);
            Iterable<MappingEntry> cacheEntries = ms.getAllMappingEntries(MAP_CACHE);

            for (MappingEntry entry : dbEntries) {
                if (entry.id().toString().equals(mappingId)) {
                    return entry;
                }
            }

            for (MappingEntry entry : cacheEntries) {
                if (entry.id().toString().equals(mappingId)) {
                    return entry;
                }
            }

            return null;
        }

        /**
         * Generates a node object of a given mapping treatment.
         *
         * @param treatment mapping treatment
         * @return node object
         */
        private ObjectNode getTreatmentNode(MappingTreatment treatment) {
            ObjectNode data = objectNode();

            data.put(MAPPING_ADDRESS, treatment.address().toString());

            for (MappingInstruction instruct : treatment.instructions()) {
                if (instruct.type() == UNICAST) {
                    UnicastMappingInstruction unicastInstruct =
                            (UnicastMappingInstruction) instruct;
                    if (unicastInstruct.subtype() == UnicastType.WEIGHT) {
                        data.put(UNICAST_WEIGHT,
                                ((UnicastMappingInstruction.WeightMappingInstruction)
                                        unicastInstruct).weight());
                    }
                    if (unicastInstruct.subtype() == UnicastType.PRIORITY) {
                        data.put(UNICAST_PRIORITY,
                                ((UnicastMappingInstruction.PriorityMappingInstruction)
                                        unicastInstruct).priority());
                    }
                }

                if (instruct.type() == MULTICAST) {
                    MulticastMappingInstruction multicastInstruct =
                            (MulticastMappingInstruction) instruct;
                    if (multicastInstruct.subtype() == MulticastType.WEIGHT) {
                        data.put(MULTICAST_WEIGHT,
                                ((MulticastMappingInstruction.WeightMappingInstruction)
                                        multicastInstruct).weight());
                    }
                    if (multicastInstruct.subtype() == MulticastType.PRIORITY) {
                        data.put(MULTICAST_PRIORITY,
                                ((MulticastMappingInstruction.PriorityMappingInstruction)
                                        multicastInstruct).priority());
                    }
                }

                // TODO: extension address will be handled later
            }

            return data;
        }

        @Override
        public void process(ObjectNode payload) {
            String mappingId = string(payload, MAPPING_ID);
            String type = string(payload, TYPE);
            String strippedFlowId = mappingId.replaceAll(OX, EMPTY);

            MappingEntry mapping = findMappingById(strippedFlowId);
            if (mapping != null) {
                ArrayNode arrayNode = arrayNode();

                for (MappingTreatment treatment : mapping.value().treatments()) {
                    arrayNode.add(getTreatmentNode(treatment));
                }

                ObjectNode detailsNode = objectNode();
                detailsNode.put(MAPPING_ID, mappingId);
                detailsNode.put(STATE, mapping.state().name());
                detailsNode.put(TYPE, type);
                detailsNode.put(MAPPING_ACTION, mapping.value().action().toString());

                ObjectNode keyNode = objectNode();
                keyNode.put(MAPPING_ADDRESS, mapping.key().address().toString());

                ObjectNode valueNode = objectNode();
                valueNode.set(MAPPING_TREATMENTS, arrayNode);

                detailsNode.set(MAPPING_KEY, keyNode);
                detailsNode.set(MAPPING_VALUE, valueNode);

                ObjectNode rootNode = objectNode();
                rootNode.set(DETAILS, detailsNode);
                sendMessage(MAPPING_DETAIL_RESP, rootNode);
            }
        }
    }
}
