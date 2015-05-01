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

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.Port;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.link.LinkService;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.table.AbstractTableRow;
import org.onosproject.ui.table.TableRequestHandler;
import org.onosproject.ui.table.TableRow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Message handler for device view related messages.
 */
public class DeviceViewMessageHandler extends UiMessageHandler {

    private static final String DEV_DATA_REQ = "deviceDataRequest";
    private static final String DEV_DATA_RESP = "deviceDataResponse";
    private static final String DEVICES = "devices";

    private static final String DEV_DETAILS_REQ = "deviceDetailsRequest";
    private static final String DEV_DETAILS_RESP = "deviceDetailsResponse";
    private static final String DETAILS = "details";

    private static final String ID = "id";
    private static final String TYPE = "type";
    private static final String AVAILABLE = "available";
    private static final String AVAILABLE_IID = "_iconid_available";
    private static final String TYPE_IID = "_iconid_type";
    private static final String DEV_ICON_PREFIX = "devIcon_";
    private static final String NUM_PORTS = "num_ports";
    private static final String LINK_DEST = "elinks_dest";
    private static final String MFR = "mfr";
    private static final String HW = "hw";
    private static final String SW = "sw";
    private static final String PROTOCOL = "protocol";
    private static final String MASTER_ID = "masterid";
    private static final String CHASSIS_ID = "chassisid";
    private static final String SERIAL = "serial";
    private static final String PORTS = "ports";
    private static final String ENABLED = "enabled";
    private static final String SPEED = "speed";
    private static final String NAME = "name";


    @Override
    protected Collection<RequestHandler> getHandlers() {
        return ImmutableSet.of(
                new DataRequestHandler(),
                new DetailRequestHandler()
        );
    }

    // handler for device table requests
    private final class DataRequestHandler extends TableRequestHandler {
        private DataRequestHandler() {
            super(DEV_DATA_REQ, DEV_DATA_RESP, DEVICES);
        }

        @Override
        protected TableRow[] generateTableRows(ObjectNode payload) {
            DeviceService service = get(DeviceService.class);
            MastershipService mastershipService = get(MastershipService.class);
            List<TableRow> list = new ArrayList<>();
            for (Device dev : service.getDevices()) {
                list.add(new DeviceTableRow(service, mastershipService, dev));
            }
            return list.toArray(new TableRow[list.size()]);
        }
    }

    // handler for selected device detail requests
    private final class DetailRequestHandler extends RequestHandler {
        private DetailRequestHandler() {
            super(DEV_DETAILS_REQ);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            String id = string(payload, "id", "of:0000000000000000");

            DeviceId deviceId = DeviceId.deviceId(id);
            DeviceService service = get(DeviceService.class);
            MastershipService ms = get(MastershipService.class);
            Device device = service.getDevice(deviceId);
            ObjectNode data = MAPPER.createObjectNode();

            data.put(ID, deviceId.toString());
            data.put(TYPE, device.type().toString());
            data.put(TYPE_IID, getTypeIconId(device));
            data.put(MFR, device.manufacturer());
            data.put(HW, device.hwVersion());
            data.put(SW, device.swVersion());
            data.put(SERIAL, device.serialNumber());
            data.put(CHASSIS_ID, device.chassisId().toString());
            data.put(MASTER_ID, ms.getMasterFor(deviceId).toString());
            data.put(PROTOCOL, device.annotations().value(PROTOCOL));

            ArrayNode ports = MAPPER.createArrayNode();

            List<Port> portList = new ArrayList<>(service.getPorts(deviceId));
            Collections.sort(portList, (p1, p2) -> {
                long delta = p1.number().toLong() - p2.number().toLong();
                return delta == 0 ? 0 : (delta < 0 ? -1 : +1);
            });

            for (Port p : portList) {
                ports.add(portData(p, deviceId));
            }
            data.set(PORTS, ports);

            ObjectNode rootNode = MAPPER.createObjectNode();
            rootNode.set(DETAILS, data);
            sendMessage(DEV_DETAILS_RESP, 0, rootNode);
        }

        private ObjectNode portData(Port p, DeviceId id) {
            ObjectNode port = MAPPER.createObjectNode();
            LinkService ls = get(LinkService.class);
            String name = p.annotations().value(AnnotationKeys.PORT_NAME);

            port.put(ID, p.number().toString());
            port.put(TYPE, p.type().toString());
            port.put(SPEED, p.portSpeed());
            port.put(ENABLED, p.isEnabled());
            port.put(NAME, name != null ? name : "");

            Set<Link> links = ls.getEgressLinks(new ConnectPoint(id, p.number()));
            if (!links.isEmpty()) {
                StringBuilder egressLinks = new StringBuilder();
                for (Link l : links) {
                    ConnectPoint dest = l.dst();
                    egressLinks.append(dest.elementId()).append("/")
                            .append(dest.port()).append(" ");
                }
                port.put(LINK_DEST, egressLinks.toString());
            }

            return port;
        }

    }

    private static String getTypeIconId(Device d) {
        return DEV_ICON_PREFIX + d.type().toString();
    }

    /**
     * TableRow implementation for {@link Device devices}.
     */
    private static class DeviceTableRow extends AbstractTableRow {

        private static final String[] COL_IDS = {
                AVAILABLE, AVAILABLE_IID, TYPE_IID, ID,
                NUM_PORTS, MASTER_ID, MFR, HW, SW,
                PROTOCOL, CHASSIS_ID, SERIAL
        };

        private static final String ICON_ID_ONLINE = "active";
        private static final String ICON_ID_OFFLINE = "inactive";

        public DeviceTableRow(DeviceService service,
                              MastershipService ms,
                              Device d) {
            boolean available = service.isAvailable(d.id());
            String iconId = available ? ICON_ID_ONLINE : ICON_ID_OFFLINE;
            DeviceId id = d.id();
            List<Port> ports = service.getPorts(id);

            add(ID, id.toString());
            add(AVAILABLE, Boolean.toString(available));
            add(AVAILABLE_IID, iconId);
            add(TYPE_IID, getTypeIconId(d));
            add(MFR, d.manufacturer());
            add(HW, d.hwVersion());
            add(SW, d.swVersion());
            add(PROTOCOL, d.annotations().value(PROTOCOL));
            add(NUM_PORTS, Integer.toString(ports.size()));
            add(MASTER_ID, ms.getMasterFor(d.id()).toString());
        }

        @Override
        protected String[] columnIds() {
            return COL_IDS;
        }
    }

}
