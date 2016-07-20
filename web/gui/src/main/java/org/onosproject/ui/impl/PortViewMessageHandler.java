/*
 * Copyright 2015-present Open Networking Laboratory
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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortStatistics;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.table.TableModel;
import org.onosproject.ui.table.TableRequestHandler;
import org.onosproject.ui.table.cell.NumberFormatter;

import java.util.Collection;


/**
 * Message handler for port view related messages.
 */
public class PortViewMessageHandler extends UiMessageHandler {

    private static final String PORT_DATA_REQ = "portDataRequest";
    private static final String PORT_DATA_RESP = "portDataResponse";
    private static final String PORTS = "ports";

    private static final String ID = "id";
    private static final String PKT_RX = "pkt_rx";
    private static final String PKT_TX = "pkt_tx";
    private static final String BYTES_RX = "bytes_rx";
    private static final String BYTES_TX = "bytes_tx";
    private static final String PKT_RX_DRP = "pkt_rx_drp";
    private static final String PKT_TX_DRP = "pkt_tx_drp";
    private static final String DURATION = "duration";

    private static final String[] COL_IDS = {
            ID, PKT_RX, PKT_TX, BYTES_RX, BYTES_TX,
            PKT_RX_DRP, PKT_TX_DRP, DURATION
    };

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(new PortDataRequest());
    }

    // handler for port table requests
    private final class PortDataRequest extends TableRequestHandler {

        private static final String NO_ROWS_MESSAGE = "No ports found";

        private PortDataRequest() {
            super(PORT_DATA_REQ, PORT_DATA_RESP, PORTS);
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
            tm.setFormatter(PKT_RX, NumberFormatter.INTEGER);
            tm.setFormatter(PKT_TX, NumberFormatter.INTEGER);
            tm.setFormatter(BYTES_RX, NumberFormatter.INTEGER);
            tm.setFormatter(BYTES_TX, NumberFormatter.INTEGER);
            tm.setFormatter(PKT_RX_DRP, NumberFormatter.INTEGER);
            tm.setFormatter(PKT_TX_DRP, NumberFormatter.INTEGER);
            return tm;
        }

        @Override
        protected void populateTable(TableModel tm, ObjectNode payload) {
            String uri = string(payload, "devId");
            if (!Strings.isNullOrEmpty(uri)) {
                DeviceId deviceId = DeviceId.deviceId(uri);
                DeviceService ds = get(DeviceService.class);
                for (PortStatistics stat : ds.getPortStatistics(deviceId)) {
                    populateRow(tm.addRow(), stat);
                }
            }
        }

        private void populateRow(TableModel.Row row, PortStatistics stat) {
            row.cell(ID, stat.port())
                .cell(PKT_RX, stat.packetsReceived())
                .cell(PKT_TX, stat.packetsSent())
                .cell(BYTES_RX, stat.bytesReceived())
                .cell(BYTES_TX, stat.bytesSent())
                .cell(PKT_RX_DRP, stat.packetsRxDropped())
                .cell(PKT_TX_DRP, stat.packetsTxDropped())
                .cell(DURATION, stat.durationSec());
        }
    }
}
