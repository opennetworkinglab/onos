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
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.link.LinkService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//import org.onosproject.net.Link;
//import java.util.Set;

/**
 * Message handler for device view related messages.
 */
public class DeviceViewMessageHandler extends AbstractTabularViewMessageHandler {

    /**
     * Creates a new message handler for the device messages.
     */
    protected DeviceViewMessageHandler() {
        super(ImmutableSet.of("deviceDataRequest"));
    }

    @Override
    public void process(ObjectNode message) {
        ObjectNode payload = payload(message);
        String sortCol = string(payload, "sortCol", "id");
        String sortDir = string(payload, "sortDir", "asc");

        DeviceService service = get(DeviceService.class);
        MastershipService mastershipService = get(MastershipService.class);
        LinkService linkService = get(LinkService.class);
        TableRow[] rows = generateTableRows(service,
                                            mastershipService,
                                            linkService);
        RowComparator rc =
                new RowComparator(sortCol, RowComparator.direction(sortDir));
        Arrays.sort(rows, rc);
        ArrayNode devices = generateArrayNode(rows);
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.set("devices", devices);

        connection().sendMessage("deviceDataResponse", 0, rootNode);
    }

    private TableRow[] generateTableRows(DeviceService service,
                                         MastershipService mastershipService,
                                         LinkService linkService) {
        List<TableRow> list = new ArrayList<>();
        for (Device dev : service.getDevices()) {
            list.add(new DeviceTableRow(service,
                                        mastershipService,
                                        linkService,
                                        dev));
        }
        return list.toArray(new TableRow[list.size()]);
    }

    /**
     * TableRow implementation for {@link Device devices}.
     */
    private static class DeviceTableRow extends AbstractTableRow {

        private static final String ID = "id";
        private static final String AVAILABLE = "available";
        private static final String AVAILABLE_IID = "_iconid_available";
        private static final String TYPE_IID = "_iconid_type";
        private static final String DEV_ICON_PREFIX = "devIcon_";
        private static final String NUM_PORTS = "num_ports";
        private static final String NUM_EGRESS_LINKS = "num_elinks";
        private static final String MFR = "mfr";
        private static final String HW = "hw";
        private static final String SW = "sw";
        private static final String PROTOCOL = "protocol";
        private static final String MASTERID = "masterid";
        private static final String CHASSISID = "chassisid";
        private static final String SERIAL = "serial";

        private static final String[] COL_IDS = {
                AVAILABLE, AVAILABLE_IID, TYPE_IID, ID,
                NUM_PORTS, NUM_EGRESS_LINKS, MASTERID, MFR, HW, SW,
                PROTOCOL, CHASSISID, SERIAL
        };

        private static final String ICON_ID_ONLINE = "deviceOnline";
        private static final String ICON_ID_OFFLINE = "deviceOffline";

        // TODO: use in details pane
//        private String getPorts(List<Port> ports) {
//            String formattedString = "";
//            int numPorts = 0;
//
//            for (Port p : ports) {
//                numPorts++;
//                formattedString += p.number().toString() + ", ";
//            }
//            return formattedString + "Total: " + numPorts;
//        }

        // TODO: use in details pane
//        private String getEgressLinks(Set<Link> links) {
//            String formattedString = "";
//
//            for (Link l : links) {
//                formattedString += l.dst().port().toString() + ", ";
//            }
//            return formattedString;
//        }

        // TODO: include "extra" backend information in device details pane
        public DeviceTableRow(DeviceService service,
                              MastershipService ms,
                              LinkService ls,
                              Device d) {
            boolean available = service.isAvailable(d.id());
            String iconId = available ? ICON_ID_ONLINE : ICON_ID_OFFLINE;
            DeviceId id = d.id();
            List<Port> ports = service.getPorts(id);
//            Set<Link> links = ls.getDeviceEgressLinks(id);

            add(ID, id.toString());
            add(AVAILABLE, Boolean.toString(available));
            add(AVAILABLE_IID, iconId);
            add(TYPE_IID, getTypeIconId(d));
            add(MFR, d.manufacturer());
            add(HW, d.hwVersion());
            add(SW, d.swVersion());
//            add(SERIAL, d.serialNumber());
            add(PROTOCOL, d.annotations().value(PROTOCOL));
            add(NUM_PORTS, Integer.toString(ports.size()));
//            add(NUM_EGRESS_LINKS, Integer.toString(links.size()));
//            add(CHASSISID, d.chassisId().toString());
            add(MASTERID, ms.getMasterFor(d.id()).toString());
        }

        private String getTypeIconId(Device d) {
            return DEV_ICON_PREFIX + d.type().toString();
        }

        @Override
        protected String[] columnIds() {
            return COL_IDS;
        }
    }

}
