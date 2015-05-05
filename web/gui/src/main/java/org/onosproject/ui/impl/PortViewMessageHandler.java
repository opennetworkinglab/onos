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
import org.onosproject.ui.table.AbstractTableRow;
import org.onosproject.ui.table.TableRequestHandler;
import org.onosproject.ui.table.TableRow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


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

    @Override
    protected Collection<RequestHandler> getHandlers() {
        return ImmutableSet.of(new PortDataRequest());
    }

    // handler for port table requests
    private final class PortDataRequest extends TableRequestHandler {

        private PortDataRequest() {
            super(PORT_DATA_REQ, PORT_DATA_RESP, PORTS);
        }

        @Override
        protected TableRow[] generateTableRows(ObjectNode payload) {
            String uri = string(payload, "devId");
            if (Strings.isNullOrEmpty(uri)) {
                return new TableRow[0];
            }
            DeviceId deviceId = DeviceId.deviceId(uri);
            DeviceService service = get(DeviceService.class);
            List<TableRow> list = new ArrayList<>();
            for (PortStatistics stat : service.getPortStatistics(deviceId)) {
                list.add(new PortTableRow(stat));
            }
            return list.toArray(new TableRow[list.size()]);
        }
    }

    /**
     * TableRow implementation for
     * {@link org.onosproject.net.device.PortStatistics port statistics}.
     */
    private static class PortTableRow extends AbstractTableRow {

        private static final String[] COL_IDS = {
                ID, PKT_RX, PKT_TX, BYTES_RX, BYTES_TX,
                PKT_RX_DRP, PKT_TX_DRP, DURATION
        };

        public PortTableRow(PortStatistics stat) {
            add(ID, Integer.toString(stat.port()));
            add(PKT_RX, Long.toString(stat.packetsReceived()));
            add(PKT_TX, Long.toString(stat.packetsSent()));
            add(BYTES_RX, Long.toString(stat.bytesReceived()));
            add(BYTES_TX, Long.toString(stat.bytesSent()));
            add(PKT_RX_DRP, Long.toString(stat.packetsRxDropped()));
            add(PKT_TX_DRP, Long.toString(stat.packetsTxDropped()));
            add(DURATION, Long.toString(stat.durationSec()));
        }

        @Override
        protected String[] columnIds() {
            return COL_IDS;
        }
    }

}
