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

package org.onosproject.ui.impl;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortStatistics;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.table.TableModel;
import org.onosproject.ui.table.TableRequestHandler;
import org.onosproject.ui.table.cell.NumberFormatter;

import java.util.Collection;
import java.util.List;

import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.PortNumber.portNumber;


/**
 * Message handler for port view related messages.
 */
public class PortViewMessageHandler extends UiMessageHandler {

    private static final String PORT_DATA_REQ = "portDataRequest";
    private static final String PORT_DATA_RESP = "portDataResponse";
    private static final String PORTS = "ports";
    private static final String DELTA = "showDelta";
    private static final String NZ = "nzFilter";

    private static final String PORT_DETAILS_REQ = "portDetailsRequest";
    private static final String PORT_DETAILS_RESP = "portDetailsResponse";
    private static final String DETAILS = "details";
    private static final String PORT = "port";

    private static final String DEV_ID = "devId";
    private static final String ID = "id";
    private static final String PKT_RX = "pkt_rx";
    private static final String PKT_TX = "pkt_tx";
    private static final String BYTES_RX = "bytes_rx";
    private static final String BYTES_TX = "bytes_tx";
    private static final String PKT_RX_DRP = "pkt_rx_drp";
    private static final String PKT_TX_DRP = "pkt_tx_drp";
    private static final String DURATION = "duration";
    private static final String SPEED = "speed";
    private static final String ENABLED = "enabled";
    private static final String TYPE = "type";
    private static final String TYPE_IID = "_iconid_type";
    private static final String PORT_ICON_PREFIX = "portIcon_";



    private static final String[] COL_IDS = {
            ID, PKT_RX, PKT_TX, BYTES_RX, BYTES_TX,
            PKT_RX_DRP, PKT_TX_DRP, DURATION
    };

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(
                new PortDataRequest(),
                new DetailRequestHandler()
        );
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
            boolean nz = bool(payload, NZ);
            boolean delta = bool(payload, DELTA);
            if (!Strings.isNullOrEmpty(uri)) {
                DeviceId deviceId = DeviceId.deviceId(uri);
                DeviceService ds = get(DeviceService.class);
                List<PortStatistics> stats = delta ?
                        ds.getPortDeltaStatistics(deviceId) :
                        ds.getPortStatistics(deviceId);
                for (PortStatistics stat : stats) {
                    populateRow(tm.addRow(), stat);
                }
            }
        }

        private void populateRow(TableModel.Row row, PortStatistics stat) {
            row.cell(ID, stat.portNumber())
                .cell(PKT_RX, stat.packetsReceived())
                .cell(PKT_TX, stat.packetsSent())
                .cell(BYTES_RX, stat.bytesReceived())
                .cell(BYTES_TX, stat.bytesSent())
                .cell(PKT_RX_DRP, stat.packetsRxDropped())
                .cell(PKT_TX_DRP, stat.packetsTxDropped())
                .cell(DURATION, stat.durationSec());
        }
    }

    private final class DetailRequestHandler extends RequestHandler {
        private DetailRequestHandler() {
            super(PORT_DETAILS_REQ);
        }

        @Override
        public void process(ObjectNode payload) {
            String id = string(payload, ID);
            String devId = string(payload, DEV_ID);

            DeviceService deviceService = get(DeviceService.class);
            Port port = deviceService.getPort(deviceId(devId), portNumber(id));

            ObjectNode data = objectNode();

            data.put(ID, id);
            data.put(DEV_ID, devId);
            data.put(TYPE, displayType(port.type()));
            data.put(SPEED, displaySpeed(port.portSpeed()));
            data.put(ENABLED, port.isEnabled());

            data.put(TYPE_IID, getIconIdForPortType(port.type()));

            ObjectNode rootNode = objectNode();
            rootNode.set(DETAILS, data);

            // NOTE: ... an alternate way of getting all the details of an item:
            // Use the codec context to get a JSON of the port. See ONOS-5976.
            rootNode.set(PORT, getJsonCodecContext().encode(port, Port.class));

            sendMessage(PORT_DETAILS_RESP, rootNode);
        }

        private String getIconIdForPortType(Port.Type type) {
            String typeStr = "DEFAULT";
            // TODO: consider providing alternate icon ID for different types
            return PORT_ICON_PREFIX + typeStr;
            // NOTE: look in icon.js for glyphMapping structure to see which
            //        glyph will be used for the port type.
        }

        /**
         * Returns the port type as a displayable string.
         *
         * @param type the port type
         * @return human readable port type
         */
        private String displayType(Port.Type type) {
            // TODO: consider better display values?
            return type.toString();
        }

        /**
         * Returns port speed as a displayable string.
         *
         * @param portSpeed port speed in Mbps
         * @return human readable port speed
         */
        private String displaySpeed(long portSpeed) {
            // TODO: better conversion between Gbps, Mbps, Kbps, etc.
            return "" + portSpeed + " Mbps";
        }
    }
}
