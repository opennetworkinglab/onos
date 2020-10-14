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
package org.onosproject.inbandtelemetry.app.ui;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import org.onosproject.inbandtelemetry.api.IntIntent;
import org.onosproject.inbandtelemetry.api.IntIntentId;
import org.onosproject.net.behaviour.inbandtelemetry.IntMetadataType;
import org.onosproject.inbandtelemetry.api.IntService;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.criteria.TcpPortCriterion;
import org.onosproject.net.flow.criteria.UdpPortCriterion;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.table.TableModel;
import org.onosproject.ui.table.TableRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Message handler for installed INT intents table in the application UI.
 */
public class IntAppTableMessageHandler extends UiMessageHandler {
    private static final String INT_APP_INT_INTENT = "intAppIntIntent";
    private static final String INT_APP_INT_INTENT_PAYLOAD = "intAppIntIntents";
    private static final String INT_APP_INT_INTENT_DATA_REQUEST = INT_APP_INT_INTENT + "DataRequest";
    private static final String INT_APP_INT_INTENT_DATA_RESPONSE = INT_APP_INT_INTENT + "DataResponse";

    private static final String INT_APP_DEL_INT_INTENT_REQ = "intAppDelIntIntentRequest";

    private static final String NO_ROWS_MESSAGE = "No IntIntent found";

    private static final String ID = "id";
    private static final String SRC_ADDR = "srcAddr";
    private static final String DST_ADDR = "dstAddr";
    private static final String SRC_PORT = "srcPort";
    private static final String DST_PORT = "dstPort";
    private static final String PROTOCOL = "protocol";
    private static final String METADATA = "metadata";
    private static final String TELEMETRY_MODE = "telemetryMode";

    private static final String[] COLUMN_IDS = {
            ID, SRC_ADDR, DST_ADDR, SRC_PORT, DST_PORT, PROTOCOL, METADATA, TELEMETRY_MODE};

    private final Logger log = LoggerFactory.getLogger(getClass());

    protected IntService intService;

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(
                new IntAppIntIntentRequestHandler(),
                new IntAppDelIntIntentRequestHandler()
        );
    }

    // handler for table requests
    private final class IntAppIntIntentRequestHandler extends TableRequestHandler {

        private IntAppIntIntentRequestHandler() {
            super(INT_APP_INT_INTENT_DATA_REQUEST, INT_APP_INT_INTENT_DATA_RESPONSE, INT_APP_INT_INTENT_PAYLOAD);
        }

        @Override
        protected String[] getColumnIds() {
            return COLUMN_IDS;
        }

        @Override
        protected String noRowsMessage(ObjectNode payload) {
            return NO_ROWS_MESSAGE;
        }

        private Map<IntIntentId, IntIntent> getAllIntIntents() {
            intService = get(IntService.class);
            return intService.getIntIntents();
        }

        @Override
        protected void populateTable(TableModel tm, ObjectNode payload) {
            Map<IntIntentId, IntIntent> intentMap = getAllIntIntents();
            intentMap.entrySet().forEach(entry ->
                                                 populateRow(tm.addRow(), entry.getKey(), entry.getValue()));
        }

        private void populateRow(TableModel.Row row, IntIntentId intentId, IntIntent intent) {
            IPCriterion ip4Src = (IPCriterion) intent.selector().getCriterion(Criterion.Type.IPV4_SRC);
            IPCriterion ip4Dst = (IPCriterion) intent.selector().getCriterion(Criterion.Type.IPV4_DST);
            TcpPortCriterion tcpSrcPort = (TcpPortCriterion) intent.selector().getCriterion(Criterion.Type.TCP_SRC);
            TcpPortCriterion tcpDstPort = (TcpPortCriterion) intent.selector().getCriterion(Criterion.Type.TCP_DST);
            UdpPortCriterion udpSrcPort = (UdpPortCriterion) intent.selector().getCriterion(Criterion.Type.UDP_SRC);
            UdpPortCriterion udpDstPort = (UdpPortCriterion) intent.selector().getCriterion(Criterion.Type.UDP_DST);
            Set<IntMetadataType> metadataTypes = intent.metadataTypes();
            row.cell(ID, intentId.toString())
                    .cell(SRC_ADDR, ip4Src == null ? "N/A" : ip4Src.ip().toString())
                    .cell(DST_ADDR, ip4Dst == null ? "N/A" : ip4Dst.ip().toString());
            if (tcpSrcPort != null || tcpDstPort != null) {
                row.cell(PROTOCOL, "TCP")
                        .cell(SRC_PORT, tcpSrcPort == null ? "N/A" : tcpSrcPort.tcpPort().toString())
                        .cell(DST_PORT, tcpDstPort == null ? "N/A" : tcpDstPort.tcpPort().toString());
            } else if (udpSrcPort != null || udpDstPort != null) {
                row.cell(PROTOCOL, "UDP")
                        .cell(SRC_PORT, udpSrcPort == null ? "N/A" : udpSrcPort.udpPort().toString())
                        .cell(DST_PORT, udpDstPort == null ? "N/A" : udpDstPort.udpPort().toString());
            } else {
                row.cell(PROTOCOL, "N/A")
                        .cell(SRC_PORT, "N/A")
                        .cell(DST_PORT, "N/A");
            }
            String metaStr = "";
            for (IntMetadataType metadataType : metadataTypes) {
                metaStr += metadataType.toString();
                metaStr += ", ";
            }
            row.cell(METADATA, metaStr);
            row.cell(TELEMETRY_MODE, intent.telemetryMode());
        }
    }

    private final class IntAppDelIntIntentRequestHandler extends RequestHandler {

        private IntAppDelIntIntentRequestHandler() {
            super(INT_APP_DEL_INT_INTENT_REQ);
        }

        @Override
        public void process(ObjectNode payload) {
            intService = get(IntService.class);
            if (payload.get(ID) != null) {
                intService.removeIntIntent(IntIntentId.valueOf(payload.get(ID).asLong()));
            }
        }
    }
}
