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
import org.onlab.osgi.ServiceDirectory;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiConnection;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.table.TableModel;
import org.onosproject.ui.table.TableRequestHandler;

import java.util.Collection;

import static org.onosproject.net.Device.Type;

/**
 * Table-View message handler for ROADM device view.
 */
public class RoadmDeviceViewMessageHandler extends UiMessageHandler {

    private static final String ROADM_DEVICE_DATA_REQ = "roadmDeviceDataRequest";
    private static final String ROADM_DEVICE_DATA_RESP = "roadmDeviceDataResponse";
    private static final String ROADM_DEVICES = "roadmDevices";

    private static final String ID = "id";
    private static final String NAME = "name";
    private static final String TYPE = "type";
    private static final String MASTER = "master";
    private static final String PORTS = "ports";
    private static final String VENDOR = "vendor";
    private static final String HW_VERSION = "hwVersion";
    private static final String SW_VERSION = "swVersion";
    private static final String PROTOCOL = "protocol";

    private static final String[] COLUMN_IDS = {
            ID, NAME, TYPE, MASTER, PORTS, VENDOR, HW_VERSION, SW_VERSION, PROTOCOL
    };

    private DeviceService deviceService;
    private MastershipService mastershipService;

    @Override
    public void init(UiConnection connection, ServiceDirectory directory) {
        super.init(connection, directory);
        deviceService = get(DeviceService.class);
        mastershipService = get(MastershipService.class);
    }

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(new DeviceTableDataRequestHandler());
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
            return RoadmUtil.NO_ROWS_MESSAGE;
        }

        @Override
        protected void populateTable(TableModel tm, ObjectNode payload) {
            for (Device device : deviceService.getDevices()) {
                Type type = device.type();
                if (type == Type.ROADM || type == Type.OPTICAL_AMPLIFIER || type == Type.FIBER_SWITCH) {
                    populateRow(tm.addRow(), device);
                }
            }
        }

        private void populateRow(TableModel.Row row, Device device) {
            DeviceId devId = device.id();
            String id = devId.toString();
            row.cell(ID, id)
                    .cell(NAME, RoadmUtil.getAnnotation(device.annotations(), AnnotationKeys.PORT_NAME, id))
                    .cell(TYPE, RoadmUtil.objectToString(device.type(), RoadmUtil.UNKNOWN))
                    .cell(MASTER, mastershipService.getMasterFor(devId))
                    .cell(PORTS, deviceService.getPorts(devId).size())
                    .cell(VENDOR, device.manufacturer())
                    .cell(HW_VERSION, device.hwVersion())
                    .cell(SW_VERSION, device.swVersion())
                    .cell(PROTOCOL, RoadmUtil.getAnnotation(device.annotations(), PROTOCOL));
        }
    }
}
