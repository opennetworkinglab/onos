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
import com.google.common.collect.Range;
import org.onlab.osgi.ServiceDirectory;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.DeviceId;
import org.onosproject.net.optical.OpticalAnnotations;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiConnection;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.table.TableModel;
import org.onosproject.ui.table.TableRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

/**
 * Table-View message handler for ROADM port view.
 */
public class RoadmPortViewMessageHandler extends UiMessageHandler {

    private static final String ROADM_PORT_DATA_REQ = "roadmPortDataRequest";
    private static final String ROADM_PORT_DATA_RESP = "roadmPortDataResponse";
    private static final String ROADM_PORTS = "roadmPorts";

    private static final String ROADM_SET_TARGET_POWER_REQ = "roadmSetTargetPowerRequest";
    private static final String ROADM_SET_TARGET_POWER_RESP = "roadmSetTargetPowerResponse";

    private static final String NO_ROWS_MESSAGE = "No items found";

    private static final String DEV_ID = "devId";

    private static final String ID = "id";
    private static final String TYPE = "type";
    private static final String NAME = "name";
    private static final String ENABLED = "enabled";
    private static final String MIN_FREQ = "minFreq";
    private static final String MAX_FREQ = "maxFreq";
    private static final String GRID = "grid";
    private static final String INPUT_POWER_RANGE = "inputPowerRange";
    private static final String CURRENT_POWER = "currentPower";
    private static final String TARGET_POWER = "targetPower";
    private static final String HAS_TARGET_POWER = "hasTargetPower";

    private static final String[] COLUMN_IDS = {
            ID, TYPE, NAME, ENABLED, MIN_FREQ, MAX_FREQ, GRID, INPUT_POWER_RANGE,
            CURRENT_POWER, TARGET_POWER, HAS_TARGET_POWER,
    };

    private static final String NA = "N/A";
    private static final String UNKNOWN = "Unknown";

    private static final long GHZ = 1_000_000_000L;
    private static final long THZ = 1_000_000_000_000L;

    private DeviceService deviceService;
    private RoadmService roadmService;

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public void init(UiConnection connection, ServiceDirectory directory) {
        super.init(connection, directory);
        deviceService = get(DeviceService.class);
        roadmService = get(RoadmService.class);
    }

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(
                new PortTableDataRequestHandler(),
                new SetTargetPowerRequestHandler()
        );
    }

    private String asGHz(String value) {
        return String.valueOf(Double.valueOf(value) / GHZ);
    }

    private String asTHz(String value) {
        return String.valueOf(Double.valueOf(value) / THZ);
    }

    private String annotation(Port port, String key, String defaultValue) {
        String value = port.annotations().value(key);
        return value != null ? value : defaultValue;
    }

    private String annotation(Port port, String key) {
        return annotation(port, key, NA);
    }

    // Handler for sample table requests
    private final class PortTableDataRequestHandler extends TableRequestHandler {

        private PortTableDataRequestHandler() {
            super(ROADM_PORT_DATA_REQ, ROADM_PORT_DATA_RESP, ROADM_PORTS);
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
            DeviceId deviceId = DeviceId.deviceId(string(payload, DEV_ID, "(none)"));

            if (deviceService.isAvailable(deviceId)) {
                List<Port> ports = deviceService.getPorts(deviceId);
                for (Port port : ports) {
                    populateRow(tm.addRow(), port, deviceId);
                }
            }
        }

        private void populateRow(TableModel.Row row, Port port, DeviceId deviceId) {
            row.cell(ID, port.number().toLong())
                    .cell(TYPE, port.type())
                    .cell(ENABLED, port.isEnabled())
                    .cell(NAME, annotation(port, AnnotationKeys.PORT_NAME))
                    .cell(MIN_FREQ, asTHz(annotation(port, OpticalAnnotations.MIN_FREQ_HZ)))
                    .cell(MAX_FREQ, asTHz(annotation(port, OpticalAnnotations.MAX_FREQ_HZ)))
                    .cell(GRID, asGHz(annotation(port, OpticalAnnotations.GRID_HZ)))
                    .cell(INPUT_POWER_RANGE, getInputPowerRange(deviceId, port.number()))
                    .cell(CURRENT_POWER, getCurrentPower(deviceId, port.number()))
                    .cell(TARGET_POWER, getTargetPower(deviceId, port.number()))
                    .cell(HAS_TARGET_POWER, roadmService.hasPortTargetPower(deviceId, port.number()));
        }

        // Returns the input power range as a string, N/A if the port is not an
        // input port
        private String getInputPowerRange(DeviceId deviceId, PortNumber portNumber) {
            Range<Long> range =
                    roadmService.inputPortPowerRange(deviceId, portNumber);
            if (range != null) {
                return range.toString();
            }
            return NA;
        }

        // Returns the current power as a string, Unknown if no value can be found.
        private String getCurrentPower(DeviceId deviceId, PortNumber portNumber) {
            Long currentPower =
                    roadmService.getCurrentPortPower(deviceId, portNumber);
            if (currentPower != null) {
                return String.valueOf(currentPower);
            }
            return UNKNOWN;
        }

        // Returns target power as a string, Unknown if target power is expected but
        // cannot be found, N/A if port does not have configurable target power
        private String getTargetPower(DeviceId deviceId, PortNumber portNumber) {
            if (roadmService.hasPortTargetPower(deviceId, portNumber)) {
                Long targetPower =
                        roadmService.getTargetPortPower(deviceId, portNumber);
                if (targetPower != null) {
                    return String.valueOf(targetPower);
                } else {
                    return UNKNOWN;
                }
            }
            return NA;
        }
    }


    // Handler for setting port target power
    private final class SetTargetPowerRequestHandler extends RequestHandler {

        private static final String VALID = "valid";
        private static final String MESSAGE = "message";

        private static final String TARGET_POWER_ERR_MSG = "Target power range is %s.";

        private SetTargetPowerRequestHandler() {
            super(ROADM_SET_TARGET_POWER_REQ);
        }

        @Override
        public void process(ObjectNode payload) {
            DeviceId deviceId = DeviceId.deviceId(string(payload, DEV_ID, "(none)"));
            PortNumber portNumber = PortNumber.portNumber(payload.get(ID).asLong());
            long targetPower = payload.get(TARGET_POWER).asLong();
            boolean validTargetPower;

            Range<Long> range =
                    roadmService.targetPortPowerRange(deviceId, portNumber);
            if (range != null) {
                validTargetPower = range.contains(targetPower);

                if (validTargetPower) {
                    roadmService.setTargetPortPower(deviceId, portNumber, targetPower);
                }

                ObjectNode rootNode = objectNode();
                rootNode.put(ID, payload.get(ID).asText());
                rootNode.put(VALID, validTargetPower);
                rootNode.put(MESSAGE, String.format(TARGET_POWER_ERR_MSG, range.toString()));
                sendMessage(ROADM_SET_TARGET_POWER_RESP, rootNode);

            } else {
                log.warn("Unable to determine target power range for device {}", deviceId);
            }
        }
    }
}
