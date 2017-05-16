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

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.util.Frequency;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.protection.ProtectedTransportEndpointState;
import org.onosproject.net.behaviour.protection.TransportEndpointState;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.OchSignal;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.optical.OpticalAnnotations;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiConnection;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.table.TableModel;
import org.onosproject.ui.table.TableRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.onosproject.net.Device.Type;
import static org.onosproject.net.behaviour.protection.ProtectedTransportEndpointState.ACTIVE_UNKNOWN;
import static org.onosproject.roadm.RoadmUtil.OPS_OPT_AUTO;
import static org.onosproject.roadm.RoadmUtil.OPS_OPT_FORCE;
import static org.onosproject.roadm.RoadmUtil.OPS_OPT_MANUAL;

/**
 * Table-View message handler for ROADM port view.
 */
public class RoadmPortViewMessageHandler extends UiMessageHandler {

    private static final String ROADM_PORT_DATA_REQ = "roadmPortDataRequest";
    private static final String ROADM_PORT_DATA_RESP = "roadmPortDataResponse";
    private static final String ROADM_PORTS = "roadmPorts";
    private static final String ROADM_SET_TARGET_POWER_REQ = "roadmSetTargetPowerRequest";
    private static final String ROADM_SET_TARGET_POWER_RESP = "roadmSetTargetPowerResponse";
    private static final String ROADM_SHOW_ITEMS_REQ = "roadmShowPortItemsRequest";
    private static final String ROADM_SHOW_ITEMS_RESP = "roadmShowPortItemsResponse";
    private static final String ROADM_SET_OPS_MODE_REQ = "roadmSetOpsModeRequest";
    private static final String ROADM_SET_OPS_MODE_RESP = "roadmSetOpsModeResponse";

    private static final String ID = "id";
    private static final String NAME = "name";
    private static final String TYPE = "type";
    private static final String ENABLED = "enabled";
    private static final String MIN_FREQ = "minFreq";
    private static final String MAX_FREQ = "maxFreq";
    private static final String GRID = "grid";
    private static final String POWER_RANGE = "powerRange";
    private static final String CURRENT_POWER = "currentPower";
    private static final String TARGET_POWER = "targetPower";
    private static final String HAS_TARGET_POWER = "hasTargetPower";
    private static final String SERVICE_STATE = "serviceState";

    private static final String[] COLUMN_IDS = {
            ID, TYPE, NAME, ENABLED, MIN_FREQ, MAX_FREQ, GRID, POWER_RANGE,
            CURRENT_POWER, SERVICE_STATE, TARGET_POWER, HAS_TARGET_POWER
    };

    private RoadmService roadmService;
    private DeviceService deviceService;

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public void init(UiConnection connection, ServiceDirectory directory) {
        super.init(connection, directory);
        roadmService = get(RoadmService.class);
        deviceService = get(DeviceService.class);
    }

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(new PortTableDataRequestHandler(),
                new SetTargetPowerRequestHandler(),
                new CreateShowItemsRequestHandler(),
                new CreateOpsModeSetRequestHandler()
                );
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
            return RoadmUtil.NO_ROWS_MESSAGE;
        }

        @Override
        protected void populateTable(TableModel tm, ObjectNode payload) {
            DeviceId deviceId = DeviceId.deviceId(string(payload, RoadmUtil.DEV_ID));
            if (deviceService.isAvailable(deviceId)) {
                List<Port> ports = deviceService.getPorts(deviceId);
                for (Port port : ports) {
                    populateRow(tm.addRow(), port, deviceId);
                }
            }
        }

        private void populateRow(TableModel.Row row, Port port, DeviceId deviceId) {
            PortNumber portNum = port.number();
            getFrequencyLimit(deviceId, portNum);
            row.cell(ID, portNum.toLong())
                    .cell(TYPE, port.type())
                    .cell(ENABLED, port.isEnabled())
                    .cell(NAME, RoadmUtil.getAnnotation(port.annotations(), AnnotationKeys.PORT_NAME))
                    .cell(MIN_FREQ, RoadmUtil.asTHz(minFreq))
                    .cell(MAX_FREQ, RoadmUtil.asTHz(maxFreq))
                    .cell(GRID, RoadmUtil.asGHz(channelSpacing))
                    .cell(POWER_RANGE, getPowerRange(deviceId, portNum))
                    .cell(CURRENT_POWER, getCurrentPower(deviceId, portNum))
                    .cell(SERVICE_STATE, getPortServiceState(deviceId, portNum))
                    .cell(TARGET_POWER, getTargetPower(deviceId, portNum))
                    .cell(HAS_TARGET_POWER, roadmService.hasPortTargetPower(deviceId, portNum));
        }

        private String getPortServiceState(DeviceId deviceId, PortNumber portNumber) {
            if (deviceService.getDevice(deviceId).type() != Type.FIBER_SWITCH) {
                return RoadmUtil.NA;
            }
            Map<ConnectPoint, ProtectedTransportEndpointState> map =
                    roadmService.getProtectionSwitchStates(deviceId);
            for (ProtectedTransportEndpointState state : map.values()) {
                for (TransportEndpointState element : state.pathStates()) {
                    if (element.description().output().connectPoint().port().equals(portNumber)) {
                        return RoadmUtil.defaultString(element.attributes()
                            .get(OpticalAnnotations.INPUT_PORT_STATUS), RoadmUtil.UNKNOWN);
                    }
                }
            }
            return RoadmUtil.UNKNOWN;
        }

        private Frequency minFreq = null, maxFreq = null, channelSpacing = null;
        // Gets min frequency, max frequency, channel spacing
        private void getFrequencyLimit(DeviceId deviceId, PortNumber portNumber) {
            Set<OchSignal> signals = roadmService.queryLambdas(deviceId, portNumber);
            if (signals.isEmpty()) {
                return;
            }
            Comparator<OchSignal> compare =
                    (OchSignal a, OchSignal b) -> a.spacingMultiplier() - b.spacingMultiplier();
            OchSignal minOch = Collections.min(signals, compare);
            OchSignal maxOch = Collections.max(signals, compare);
            minFreq = minOch.centralFrequency();
            maxFreq = maxOch.centralFrequency();
            channelSpacing = minOch.channelSpacing().frequency();
        }

        // Returns the power range as a string, N/A if the power range not exists.
        // The power range would be input power range or target power range determined by port property.
        // If the port is RX direction then acquire the input power range from driver.
        // Otherwise there will be a TX direction port, thus acquire the target power range.
        private String getPowerRange(DeviceId deviceId, PortNumber portNumber) {
            Range<Long> range = roadmService.inputPortPowerRange(deviceId, portNumber);
            if (range == null) {
                range = roadmService.targetPortPowerRange(deviceId, portNumber);
            }
            return RoadmUtil.objectToString(range, RoadmUtil.NA);
        }

        // Returns the current power as a string, Unknown if no value can be found.
        private String getCurrentPower(DeviceId deviceId, PortNumber portNumber) {
            Long currentPower = roadmService.getCurrentPortPower(deviceId, portNumber);
            return RoadmUtil.objectToString(currentPower, RoadmUtil.UNKNOWN);
        }

        // Returns target power as a string, Unknown if target power is expected but
        // cannot be found, N/A if port does not have configurable target power
        private String getTargetPower(DeviceId deviceId, PortNumber portNumber) {
            if (!roadmService.hasPortTargetPower(deviceId, portNumber)) {
                return RoadmUtil.NA;
            }
            Long targetPower = roadmService.getTargetPortPower(deviceId, portNumber);
            return RoadmUtil.objectToString(targetPower, RoadmUtil.UNKNOWN);
        }
    }


    // Handler for setting port target power
    private final class SetTargetPowerRequestHandler extends RequestHandler {

        private static final String TARGET_POWER_ERR_MSG = "Target power range is %s.";

        private SetTargetPowerRequestHandler() {
            super(ROADM_SET_TARGET_POWER_REQ);
        }

        @Override
        public void process(ObjectNode payload) {
            DeviceId deviceId = DeviceId.deviceId(string(payload, RoadmUtil.DEV_ID));
            PortNumber portNumber = PortNumber.portNumber(payload.get(ID).asLong());
            Range<Long> range = roadmService.targetPortPowerRange(deviceId, portNumber);
            if (range == null) {
                log.warn("Unable to determine target power range for device {}", deviceId);
                return;
            }
            Long targetPower = payload.get(TARGET_POWER).asLong();
            boolean validTargetPower = range.contains(targetPower);
            if (validTargetPower) {
                roadmService.setTargetPortPower(deviceId, portNumber, targetPower);
            }
            ObjectNode rootNode = objectNode();
            rootNode.put(ID, payload.get(ID).asText());
            rootNode.put(RoadmUtil.VALID, validTargetPower);
            rootNode.put(RoadmUtil.MESSAGE, String.format(TARGET_POWER_ERR_MSG, range.toString()));
            sendMessage(ROADM_SET_TARGET_POWER_RESP, rootNode);
        }
    }

    // Protection switch operation type and path index
    private static final String OPS_ARRAY_INDEX = "index";
    private static final String OPS_ARRAY_OPERATION = "operation";
    private static final String[] OPS_NON_AUTO_OPTS = {OPS_OPT_FORCE, OPS_OPT_MANUAL};

    private final class CreateShowItemsRequestHandler extends RequestHandler {
        private static final String SHOW_TARGET_POWER = "showTargetPower";
        private static final String SHOW_SERVICE_STATE = "showServiceState";
        private static final String SHOW_FLOW_ICON = "showFlowIcon";
        private static final String OPS_PATHS = "opsOperations";
        private static final String OPS_ARRAY_NAME = "name";
        private static final String OPS_GROUP_FMT = "GROUP%d ";

        private CreateShowItemsRequestHandler() {
            super(ROADM_SHOW_ITEMS_REQ);
        }

        @Override
        public void process(ObjectNode payload) {
            DeviceId did = DeviceId.deviceId(string(payload, RoadmUtil.DEV_ID));
            Type devType = deviceService.getDevice(did).type();
            // Build response
            ObjectNode node = objectNode();
            node.put(SHOW_FLOW_ICON, devType == Type.ROADM);
            if (devType == Type.FIBER_SWITCH) {
                node.put(SHOW_TARGET_POWER, false);
                node.put(SHOW_SERVICE_STATE, true);
                // add protection switch paths
                putProtectionSwitchPaths(did, node);
            } else {
                node.put(SHOW_TARGET_POWER, true);
                node.put(SHOW_SERVICE_STATE, false);
            }
            sendMessage(ROADM_SHOW_ITEMS_RESP, node);
        }

        private void putProtectionSwitchPaths(DeviceId deviceId, ObjectNode node) {
            Map<ConnectPoint, ProtectedTransportEndpointState> states =
                    roadmService.getProtectionSwitchStates(deviceId);
            ArrayNode nodes = node.putArray(OPS_PATHS);
            // Add path names for every identifier.
            int groupIndex = 0;
            for (ConnectPoint identifier : states.keySet()) {
                // No group name needed if there is only one connection point identifier.
                String groupName = states.keySet().size() == 1 ? "" : String.format(OPS_GROUP_FMT, ++groupIndex);
                // Add AUTOMATIC operation.
                nodes.add(new ObjectNode(JsonNodeFactory.instance)
                          .put(OPS_ARRAY_INDEX, ACTIVE_UNKNOWN)
                          .put(OPS_ARRAY_OPERATION, OPS_OPT_AUTO)
                          .put(OPS_ARRAY_NAME, String.format("%s%s", groupName, OPS_OPT_AUTO)));
                // Add FORCE and MANUAL operations for every path.
                for (String opt : OPS_NON_AUTO_OPTS) {
                    int pathIndex = 0;
                    for (TransportEndpointState state : states.get(identifier).pathStates()) {
                        nodes.add(new ObjectNode(JsonNodeFactory.instance)
                                  .put(OPS_ARRAY_INDEX, pathIndex++)
                                  .put(OPS_ARRAY_OPERATION, opt)
                                  .put(OPS_ARRAY_NAME,
                                       String.format("%s%s %s", groupName, opt, state.id().id().toUpperCase())));
                    }
                }
            }


        }
    }

    private final class CreateOpsModeSetRequestHandler extends RequestHandler {
        private static final String DEVICE_INVALID_ERR_MSG = "Apply failed: device is offline or unavailable.";
        private static final String TYPE_INVALID_ERR_MSG = "Apply failed: invalid device type.";

        private CreateOpsModeSetRequestHandler() {
            super(ROADM_SET_OPS_MODE_REQ);
        }

        @Override
        public void process(ObjectNode payload) {
            DeviceId did = DeviceId.deviceId(string(payload, RoadmUtil.DEV_ID));
            ObjectNode node = objectNode();
            if (!deviceService.isAvailable(did)) {
                node.put(RoadmUtil.VALID, false);
                node.put(RoadmUtil.MESSAGE, DEVICE_INVALID_ERR_MSG);
                sendMessage(ROADM_SET_OPS_MODE_RESP, node);
                return;
            }
            Type devType = deviceService.getDevice(did).type();
            if (devType != Type.FIBER_SWITCH) {
                node.put(RoadmUtil.VALID, false);
                node.put(RoadmUtil.MESSAGE, TYPE_INVALID_ERR_MSG);
                sendMessage(ROADM_SET_OPS_MODE_RESP, node);
                return;
            }
            // get switch configuration from payload, and then switch the device.
            roadmService.configProtectionSwitch(did, string(payload, OPS_ARRAY_OPERATION),
                    roadmService.getProtectionSwitchStates(did).keySet().toArray(new ConnectPoint[0])[0],
                    (int) number(payload, OPS_ARRAY_INDEX));
            node.put(RoadmUtil.VALID, true);
            sendMessage(ROADM_SET_OPS_MODE_RESP, node);
        }
    }
}
