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
import org.onosproject.net.device.DeviceService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
        TableRow[] rows = generateTableRows(service, mastershipService);
        RowComparator rc =
                new RowComparator(sortCol, RowComparator.direction(sortDir));
        Arrays.sort(rows, rc);
        ArrayNode devices = generateArrayNode(rows);
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.set("devices", devices);

        connection().sendMessage("deviceDataResponse", 0, rootNode);
    }

    private TableRow[] generateTableRows(DeviceService service,
                                         MastershipService mastershipService) {
        List<TableRow> list = new ArrayList<>();
        for (Device dev : service.getDevices()) {
            list.add(new DeviceTableRow(service, mastershipService, dev));
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
        private static final String ROLE = "role";
        private static final String MFR = "mfr";
        private static final String HW = "hw";
        private static final String SW = "sw";
        private static final String SERIAL = "serial";
        private static final String PROTOCOL = "protocol";
        private static final String CHASSISID = "chassisid";
        private static final String MASTERID = "masterid";

        private static final String[] COL_IDS = {
                ID, AVAILABLE, AVAILABLE_IID, TYPE_IID, ROLE,
                MFR, HW, SW, SERIAL, PROTOCOL, CHASSISID, MASTERID
        };

        private static final String ICON_ID_ONLINE = "deviceOnline";
        private static final String ICON_ID_OFFLINE = "deviceOffline";

        public DeviceTableRow(DeviceService service,
                              MastershipService ms,
                              Device d) {
            boolean available = service.isAvailable(d.id());
            String iconId = available ? ICON_ID_ONLINE : ICON_ID_OFFLINE;

            add(ID, d.id().toString());
            add(AVAILABLE, Boolean.toString(available));
            add(AVAILABLE_IID, iconId);
            add(TYPE_IID, getTypeIconId(d));
            add(ROLE, service.getRole(d.id()).toString());
            add(MFR, d.manufacturer());
            add(HW, d.hwVersion());
            add(SW, d.swVersion());
            add(SERIAL, d.serialNumber());
            add(PROTOCOL, d.annotations().value(PROTOCOL));
            add(CHASSISID, d.chassisId().toString());
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
