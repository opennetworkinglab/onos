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

package org.onosproject.gui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.net.Device;
import org.onosproject.net.device.DeviceService;

import java.util.HashMap;
import java.util.Map;

public class DeviceTableRow implements TableRow {

    private static final String ID = "id";
    private static final String AVAILABLE = "available";
    private static final String AVAILABLE_IID = "_iconid_available";
    private static final String TYPE = "type";
    private static final String ROLE = "role";
    private static final String MFR = "mfr";
    private static final String HW = "hw";
    private static final String SW = "sw";
    private static final String SERIAL = "serial";
    private static final String PROTOCOL = "protocol";

    private static final String ICON_ID_ONLINE = "deviceOnline";
    private static final String ICON_ID_OFFLINE = "deviceOffline";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Map<String, String> data = new HashMap<>();

    public DeviceTableRow(DeviceService service, Device d) {
        boolean available = service.isAvailable(d.id());
        String iconId = available ? ICON_ID_ONLINE : ICON_ID_OFFLINE;

        data.put(ID, d.id().toString());
        data.put(AVAILABLE, Boolean.toString(available));
        data.put(AVAILABLE_IID, iconId);
        data.put(TYPE, d.type().toString());
        data.put(ROLE, service.getRole(d.id()).toString());
        data.put(MFR, d.manufacturer());
        data.put(HW, d.hwVersion());
        data.put(SW, d.swVersion());
        data.put(SERIAL, d.serialNumber());
        data.put(PROTOCOL, d.annotations().value(PROTOCOL));
    }

    @Override
    public String get(String key) {
        return data.get(key);
    }

    @Override
    public ObjectNode toJsonNode() {
        ObjectNode result = MAPPER.createObjectNode();
        result.put(ID, data.get(ID));
        result.put(AVAILABLE, data.get(AVAILABLE));
        result.put(AVAILABLE_IID, data.get(AVAILABLE_IID));
        result.put(TYPE, data.get(TYPE));
        result.put(ROLE, data.get(ROLE));
        result.put(MFR, data.get(MFR));
        result.put(HW, data.get(HW));
        result.put(SW, data.get(SW));
        result.put(SERIAL, data.get(SERIAL));
        result.put(PROTOCOL, data.get(PROTOCOL));
        return result;
    }
}
