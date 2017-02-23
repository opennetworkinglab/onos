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

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import org.onosproject.cluster.NodeId;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.Link;
import org.onosproject.net.Port;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.BasicDeviceConfig;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.link.LinkService;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.table.TableModel;
import org.onosproject.ui.table.TableRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Strings.emptyToNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static org.apache.commons.lang.WordUtils.capitalizeFully;
import static org.onosproject.net.DeviceId.deviceId;

/**
 * Message handler for device view related messages.
 */
public class DeviceViewMessageHandler extends UiMessageHandler {

    private static final String DEV_DATA_REQ = "deviceDataRequest";
    private static final String DEV_DATA_RESP = "deviceDataResponse";
    private static final String DEVICES = "devices";
    private static final String DEVICE = "device";

    private static final String DEV_DETAILS_REQ = "deviceDetailsRequest";
    private static final String DEV_DETAILS_RESP = "deviceDetailsResponse";
    private static final String DETAILS = "details";

    private static final String DEV_NAME_CHANGE_REQ = "deviceNameChangeRequest";
    private static final String DEV_NAME_CHANGE_RESP = "deviceNameChangeResponse";

    private static final String ZERO_URI = "of:0000000000000000";

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
    private static final String WARN = "warn";

    private static final String NONE = "none";

    private static final String[] COL_IDS = {
            AVAILABLE, AVAILABLE_IID, TYPE_IID,
            NAME, ID, MASTER_ID, NUM_PORTS, MFR, HW, SW,
            PROTOCOL, CHASSIS_ID, SERIAL
    };

    private static final String ICON_ID_ONLINE = "active";
    private static final String ICON_ID_OFFLINE = "inactive";

    private final Logger log = LoggerFactory.getLogger(getClass());


    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(
                new DataRequestHandler(),
                new NameChangeHandler(),
                new DetailRequestHandler()
        );
    }

    // Get friendly name of the device from the annotations
    private static String deviceName(Device device) {
        String name = device.annotations().value(AnnotationKeys.NAME);
        return isNullOrEmpty(name) ? device.id().toString() : name;
    }

    private static String deviceProtocol(Device device) {
        String protocol = device.annotations().value(PROTOCOL);
        return protocol != null ? protocol : "";
    }

    private static String getTypeIconId(Device d) {
        return DEV_ICON_PREFIX + d.type().toString();
    }

    // handler for device table requests
    private final class DataRequestHandler extends TableRequestHandler {
        private static final String NO_ROWS_MESSAGE = "No devices found";

        private DataRequestHandler() {
            super(DEV_DATA_REQ, DEV_DATA_RESP, DEVICES);
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
        protected void populateTable(TableModel tm, ObjectNode payload) {
            DeviceService ds = get(DeviceService.class);
            MastershipService ms = get(MastershipService.class);
            for (Device dev : ds.getDevices()) {
                populateRow(tm.addRow(), dev, ds, ms);
            }
        }

        private void populateRow(TableModel.Row row, Device dev,
                                 DeviceService ds, MastershipService ms) {
            DeviceId id = dev.id();
            boolean available = ds.isAvailable(id);
            String iconId = available ? ICON_ID_ONLINE : ICON_ID_OFFLINE;

            row.cell(ID, id)
                .cell(NAME, deviceName(dev))
                .cell(AVAILABLE, available)
                .cell(AVAILABLE_IID, iconId)
                .cell(TYPE_IID, getTypeIconId(dev))
                .cell(MFR, dev.manufacturer())
                .cell(HW, dev.hwVersion())
                .cell(SW, dev.swVersion())
                .cell(PROTOCOL, deviceProtocol(dev))
                .cell(NUM_PORTS, ds.getPorts(id).size())
                .cell(MASTER_ID, ms.getMasterFor(id));
        }
    }

    // handler for selected device detail requests
    private final class DetailRequestHandler extends RequestHandler {
        private DetailRequestHandler() {
            super(DEV_DETAILS_REQ);
        }

        @Override
        public void process(ObjectNode payload) {
            String id = string(payload, ID, ZERO_URI);

            DeviceId deviceId = deviceId(id);
            DeviceService service = get(DeviceService.class);
            MastershipService ms = get(MastershipService.class);
            Device device = service.getDevice(deviceId);
            ObjectNode data = objectNode();
            NodeId masterFor = ms.getMasterFor(deviceId);

            data.put(ID, deviceId.toString());
            data.put(NAME, deviceName(device));
            data.put(TYPE, capitalizeFully(device.type().toString()));
            data.put(TYPE_IID, getTypeIconId(device));
            data.put(MFR, device.manufacturer());
            data.put(HW, device.hwVersion());
            data.put(SW, device.swVersion());
            data.put(SERIAL, device.serialNumber());
            data.put(CHASSIS_ID, device.chassisId().toString());
            data.put(MASTER_ID, masterFor != null ? masterFor.toString() : NONE);
            data.put(PROTOCOL, deviceProtocol(device));

            ArrayNode ports = arrayNode();

            List<Port> portList = new ArrayList<>(service.getPorts(deviceId));
            portList.sort((p1, p2) -> {
                long delta = p1.number().toLong() - p2.number().toLong();
                return delta == 0 ? 0 : (delta < 0 ? -1 : +1);
            });

            for (Port p : portList) {
                ports.add(portData(p, deviceId));
            }
            data.set(PORTS, ports);

            ObjectNode rootNode = objectNode();
            rootNode.set(DETAILS, data);

            // NOTE: ... an alternate way of getting all the details of an item:
            // Use the codec context to get a JSON of the device. See ONOS-5976.
            rootNode.set(DEVICE, getJsonCodecContext().encode(device, Device.class));

            sendMessage(DEV_DETAILS_RESP, rootNode);
        }

        private ObjectNode portData(Port p, DeviceId id) {
            ObjectNode port = objectNode();
            LinkService ls = get(LinkService.class);
            String name = p.annotations().value(AnnotationKeys.PORT_NAME);

            port.put(ID, capitalizeFully(p.number().toString()));
            port.put(TYPE, capitalizeFully(p.type().toString()));
            port.put(SPEED, p.portSpeed());
            port.put(ENABLED, p.isEnabled());
            port.put(NAME, name != null ? name : "");

            ConnectPoint connectPoint = new ConnectPoint(id, p.number());
            Set<Link> links = ls.getEgressLinks(connectPoint);
            if (!links.isEmpty()) {
                StringBuilder egressLinks = new StringBuilder();
                for (Link l : links) {
                    ConnectPoint dest = l.dst();
                    egressLinks.append(dest.elementId()).append("/")
                            .append(dest.port()).append(" ");
                }
                port.put(LINK_DEST, egressLinks.toString());
            } else {
                HostService hs = get(HostService.class);
                Set<Host> hosts = hs.getConnectedHosts(connectPoint);
                if (hosts != null && !hosts.isEmpty()) {
                    port.put(LINK_DEST, hosts.iterator().next().id().toString());
                }
            }

            return port;
        }
    }


    // handler for changing device friendly name
    private final class NameChangeHandler extends RequestHandler {
        private NameChangeHandler() {
            super(DEV_NAME_CHANGE_REQ);
        }

        @Override
        public void process(ObjectNode payload) {
            DeviceId deviceId = deviceId(string(payload, ID, ZERO_URI));
            String name = emptyToNull(string(payload, NAME, null));
            log.debug("Name change request: {} -- '{}'", deviceId, name);

            NetworkConfigService service = get(NetworkConfigService.class);
            BasicDeviceConfig cfg =
                    service.addConfig(deviceId, BasicDeviceConfig.class);

            // Name attribute missing from the payload (or empty string)
            // means that the friendly name should be unset.
            cfg.name(name);
            cfg.apply();
            sendMessage(DEV_NAME_CHANGE_RESP, payload);
        }
    }
}
