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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Message handler for device view related messages.
 */
public class DeviceViewMessageHandler extends AbstractTabularViewMessageHandler {

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


    /**
     * Creates a new message handler for the device messages.
     */
    protected DeviceViewMessageHandler() {
        super(ImmutableSet.of("deviceDataRequest", "deviceDetailsRequest"));
    }

    @Override
    public void process(ObjectNode event) {
        String type = string(event, "event", "unknown");
        if (type.equals("deviceDataRequest")) {
            dataRequest(event);
        } else if (type.equals("deviceDetailsRequest")) {
            detailsRequest(event);
        }
    }

    private void dataRequest(ObjectNode event) {
        ObjectNode payload = payload(event);
        String sortCol = string(payload, "sortCol", "id");
        String sortDir = string(payload, "sortDir", "asc");

        DeviceService service = get(DeviceService.class);
        MastershipService mastershipService = get(MastershipService.class);

        TableRow[] rows = generateTableRows(service, mastershipService);
        RowComparator rc =
                new RowComparator(sortCol, RowComparator.direction(sortDir));
        Arrays.sort(rows, rc);
        ArrayNode devices = generateArrayNode(rows);
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.set("devices", devices);

        connection().sendMessage("deviceDataResponse", 0, rootNode);
    }

    private void detailsRequest(ObjectNode event) {
        ObjectNode payload = payload(event);
        String id = string(payload, "id", "of:0000000000000000");

        DeviceId deviceId = DeviceId.deviceId(id);
        DeviceService service = get(DeviceService.class);
        MastershipService ms = get(MastershipService.class);
        Device device = service.getDevice(deviceId);
        ObjectNode data = mapper.createObjectNode();

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

        ArrayNode ports = mapper.createArrayNode();

        List<Port> portList = new ArrayList<>(service.getPorts(deviceId));
        Collections.sort(portList, (p1, p2) -> {
            long delta = p1.number().toLong() - p2.number().toLong();
            return delta == 0 ? 0 : (delta < 0 ? -1 : +1);
        });

        for (Port p : portList) {
            ports.add(portData(p, deviceId));
        }
        data.set(PORTS, ports);

        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.set("details", data);
        connection().sendMessage("deviceDetailsResponse", 0, rootNode);
    }

    private TableRow[] generateTableRows(DeviceService service,
                                         MastershipService mastershipService) {
        List<TableRow> list = new ArrayList<>();
        for (Device dev : service.getDevices()) {
            list.add(new DeviceTableRow(service,
                                        mastershipService,
                                        dev));
        }
        return list.toArray(new TableRow[list.size()]);
    }

    private ObjectNode portData(Port p, DeviceId id) {
        ObjectNode port = mapper.createObjectNode();
        LinkService ls = get(LinkService.class);
        String name = p.annotations().value(AnnotationKeys.PORT_NAME);

        port.put(ID, p.number().toString());
        port.put(TYPE, p.type().toString());
        port.put(SPEED, p.portSpeed());
        port.put(ENABLED, p.isEnabled());
        port.put(NAME, name != null ? name : "");

        Set<Link> links = ls.getEgressLinks(new ConnectPoint(id, p.number()));
        if (!links.isEmpty()) {
            String egressLinks = "";
            for (Link l : links) {
                ConnectPoint dest = l.dst();
                egressLinks += dest.elementId().toString()
                        + "/" + dest.port().toString() + " ";
            }
            port.put(LINK_DEST, egressLinks);
        }

        return port;
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
