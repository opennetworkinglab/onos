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

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import org.onlab.packet.IpAddress;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.Device;
import org.onosproject.net.device.DeviceService;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.table.TableModel;
import org.onosproject.ui.table.TableRequestHandler;
import org.onosproject.ui.table.cell.TimeFormatter;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.collect.ImmutableList.copyOf;


/**
 * Message handler for cluster view related messages.
 */
public class ClusterViewMessageHandler extends UiMessageHandler {

    private static final String CLUSTER_DATA_REQ = "clusterDataRequest";
    private static final String CLUSTER_DATA_RESP = "clusterDataResponse";
    private static final String CLUSTERS = "clusters";

    private static final String CLUSTER_DETAILS_REQ = "clusterDetailsRequest";
    private static final String CLUSTER_DETAILS_RESP = "clusterDetailsResponse";
    private static final String DETAILS = "details";

    private static final String DEVICES = "devices";
    private static final String ID = "id";
    private static final String IP = "ip";
    private static final String TCP_PORT = "tcp";
    private static final String STATE_IID = "_iconid_state";
    private static final String STARTED_IID = "_iconid_started";
    private static final String UPDATED = "updated";

    private static final String[] COL_IDS = {
            ID, IP, TCP_PORT, STATE_IID, STARTED_IID, UPDATED
    };

    private static final String URI = "id";
    private static final String TYPE = "type";
    private static final String CHASSIS_ID = "chassisid";
    private static final String HW = "hw";
    private static final String SW = "sw";
    private static final String MFR = "mfr";
    private static final String PROTOCOL = "protocol";
    private static final String SERIAL = "serial";


    private static final String ICON_ID_ONLINE = "active";
    private static final String ICON_ID_OFFLINE = "inactive";

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(
                new ClusterDataRequest(),
                new DetailRequestHandler());
    }

    // handler for cluster table requests
    private final class ClusterDataRequest extends TableRequestHandler {
        private static final String NO_ROWS_MESSAGE = "No cluster nodes found";

        private ClusterDataRequest() {
            super(CLUSTER_DATA_REQ, CLUSTER_DATA_RESP, CLUSTERS);
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
            tm.setFormatter(UPDATED, new TimeFormatter());
            return tm;
        }

        @Override
        protected void populateTable(TableModel tm, ObjectNode payload) {
            ClusterService cs = get(ClusterService.class);
            for (ControllerNode node : cs.getNodes()) {
                populateRow(tm.addRow(), node, cs);
            }
        }

        private void populateRow(TableModel.Row row, ControllerNode node,
                                 ClusterService cs) {
            NodeId id = node.id();
            Instant lastUpdated = cs.getLastUpdatedInstant(id);
            ControllerNode.State state = cs.getState(id);
            String iconId = state.isActive() ? ICON_ID_ONLINE : ICON_ID_OFFLINE;
            String startedId = state.isReady() ? ICON_ID_ONLINE : ICON_ID_OFFLINE;

            row.cell(ID, id)
                .cell(IP, node.ip())
                .cell(TCP_PORT, node.tcpPort())
                .cell(STATE_IID, iconId)
                .cell(STARTED_IID, startedId)
                .cell(UPDATED, lastUpdated);
        }
    }

    private final class DetailRequestHandler extends RequestHandler {

        public DetailRequestHandler() {
            super(CLUSTER_DETAILS_REQ);
        }

        private List<Device> populateDevices(ControllerNode node) {
            DeviceService ds = get(DeviceService.class);
            MastershipService ms = get(MastershipService.class);
            return copyOf(ds.getDevices()).stream()
                    .filter(d -> ms.getMasterFor(d.id()).equals(node.id()))
                    .collect(Collectors.toList());
        }

        private String deviceProtocol(Device device) {
            String protocol = device.annotations().value(PROTOCOL);
            return protocol != null ? protocol : "";
        }

        private ObjectNode deviceData(Device d) {
            ObjectNode device = objectNode();

            device.put(URI, d.id().toString());
            device.put(TYPE, d.type().toString());
            device.put(CHASSIS_ID, d.chassisId().toString());
            device.put(MFR, d.manufacturer());
            device.put(HW, d.hwVersion());
            device.put(SW, d.swVersion());
            device.put(PROTOCOL, deviceProtocol(d));
            device.put(SERIAL, d.serialNumber());

            return device;
        }

        @Override
        public void process(ObjectNode payload) {
            ObjectNode rootNode = objectNode();
            ObjectNode data = objectNode();
            ArrayNode devices = arrayNode();

            String id = string(payload, ID);
            ClusterService cs = get(ClusterService.class);
            ControllerNode node = cs.getNode(new NodeId(id));
            if (node != null) {
                IpAddress nodeIp = node.ip();
                List<Device> deviceList = populateDevices(node);

                data.put(ID, node.id().toString());
                data.put(IP, nodeIp != null ? nodeIp.toString() : node.host());

                for (Device d : deviceList) {
                    devices.add(deviceData(d));
                }

            } else {
                data.put(ID, "NONE");
                data.put(IP, "NONE");
            }
            data.set(DEVICES, devices);

            //TODO put more detail info to data
            rootNode.set(DETAILS, data);
            sendMessage(CLUSTER_DETAILS_RESP, rootNode);
        }
    }
}
