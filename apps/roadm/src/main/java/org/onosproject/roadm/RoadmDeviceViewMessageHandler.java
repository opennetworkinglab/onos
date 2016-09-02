/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.roadm;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.Device;
import org.onosproject.net.device.DeviceService;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.table.TableModel;
import org.onosproject.ui.table.TableRequestHandler;

import java.util.Collection;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Table-View message handler for ROADM device view.
 */
public class RoadmDeviceViewMessageHandler extends UiMessageHandler {

    private static final String ROADM_DEVICE_DATA_REQ = "roadmDeviceDataRequest";
    private static final String ROADM_DEVICE_DATA_RESP = "roadmDeviceDataResponse";
    private static final String ROADM_DEVICES = "roadmDevices";

    private static final String NO_ROWS_MESSAGE = "No items found";

    private static final String ID = "id";
    private static final String FRIENDLY_NAME = "name";
    private static final String MASTER = "master";
    private static final String PORTS = "ports";
    private static final String VENDOR = "vendor";
    private static final String HW_VERSION = "hwVersion";
    private static final String SW_VERSION = "swVersion";
    private static final String PROTOCOL = "protocol";

    private static final String[] COLUMN_IDS = {
            ID, FRIENDLY_NAME, MASTER, PORTS, VENDOR, HW_VERSION, SW_VERSION,
            PROTOCOL
    };

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(
                new DeviceTableDataRequestHandler()
        );
    }

    // Returns friendly name of the device from the annotations
    private static String deviceName(Device device) {
        String name = device.annotations().value(AnnotationKeys.NAME);
        return isNullOrEmpty(name) ? device.id().toString() : name;
    }

    // Returns the device protocol from annotations
    private static String deviceProtocol(Device device) {
        String protocol = device.annotations().value(PROTOCOL);
        return protocol != null ? protocol : "N/A";
    }

    // Handler for sample table requests
    private final class DeviceTableDataRequestHandler extends TableRequestHandler {

        private DeviceTableDataRequestHandler() {
            super(ROADM_DEVICE_DATA_REQ, ROADM_DEVICE_DATA_RESP, ROADM_DEVICES);
        }

        @Override
        protected String[] getColumnIds() {
            return COLUMN_IDS;
        }

        @Override
        protected String noRowsMessage(ObjectNode payload) {
            return NO_ROWS_MESSAGE;
        }

        @Override
        protected void populateTable(TableModel tm, ObjectNode payload) {
            DeviceService ds = get(DeviceService.class);
            MastershipService ms = get(MastershipService.class);
            for (Device device : ds.getDevices(Device.Type.ROADM)) {
                populateRow(tm.addRow(), device, ds, ms);
            }
        }

        private void populateRow(TableModel.Row row, Device device, DeviceService ds,
                MastershipService ms) {
            row.cell(ID, device.id().toString())
                    .cell(FRIENDLY_NAME, deviceName(device))
                    .cell(MASTER, ms.getMasterFor(device.id()))
                    .cell(PORTS, ds.getPorts(device.id()).size())
                    .cell(VENDOR, device.manufacturer())
                    .cell(HW_VERSION, device.hwVersion())
                    .cell(SW_VERSION, device.swVersion())
                    .cell(PROTOCOL, deviceProtocol(device));
        }
    }
}
