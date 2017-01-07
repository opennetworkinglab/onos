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

package org.onosproject.patchpanel.impl;


import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.Port;
import org.onosproject.net.device.DeviceService;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiMessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.onosproject.net.ConnectPoint.deviceConnectPoint;

/**
 * ONOS UI Custom-View message handler.
 * <p>
 * This class contains the request handlers that handle the response
 * to each event. In this particular implementation the second message
 * handler creates the patch and the first message handler loads the data.
 */
public class PatchPanelUiMessageHandler extends UiMessageHandler {

    private static final String SAMPLE_CUSTOM_DATA_REQ = "sampleCustomDataRequest";
    private static final String SAMPLE_CUSTOM_DATA_RESP = "sampleCustomDataResponse";
    private static final String SAMPLE_CUSTOM_DATA_REQ2 = "sampleCustomDataRequest2";
    private static final String SAMPLE_CUSTOM_DATA_RESP2 = "sampleCustomDataResponse2";
    private static final String SAMPLE_CUSTOM_DATA_REQ3 = "sampleCustomDataRequest3";
    private static final String SAMPLE_CUSTOM_DATA_RESP3 = "sampleCustomDataResponse3";

    private static final String SLASH = "/";
    private static final String CPS = "cps";
    private static final String RESULT = "result";
    private static final String MESSAGE = "message";

    private static final String EOL = String.format("%n");
    private static final String WITH = " with ";
    private static final String CPOINTS = "cpoints";

    private List<ConnectPoint> previous = new ArrayList<>();
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(
                new DataRequestHandler(),
                new SecondDataRequestHandler(),
                new ThirdDataRequestHandler()
        );
    }

    // handler for data requests/events
    private final class DataRequestHandler extends RequestHandler {
        private DataRequestHandler() {
            super(SAMPLE_CUSTOM_DATA_REQ);
        }

        @Override
        public void process(ObjectNode payload) {
            DeviceService service = get(DeviceService.class);
            ObjectNode result = objectNode();
            ArrayNode cps = arrayNode();
            result.set(CPS, cps);

            for (Device device : service.getDevices()) {
                cps.add(device.id().toString());
                for (Port port : service.getPorts(device.id())) {
                    if (!port.number().isLogical()) {
                        cps.add(port.number().toString());
                        log.info(device.id() + SLASH + port.number());
                    }
                }
            }
            sendMessage(SAMPLE_CUSTOM_DATA_RESP, result);
        }
    }

    private final class SecondDataRequestHandler extends RequestHandler {
        private SecondDataRequestHandler() {
            super(SAMPLE_CUSTOM_DATA_REQ2);
        }

        @Override
        public void process(ObjectNode payload) {
            String deviceId = payload.get(RESULT).get(0).asText();
            ConnectPoint cp1 = deviceConnectPoint(deviceId + SLASH + payload.get(RESULT).get(1).asText());
            ConnectPoint cp2 = deviceConnectPoint(deviceId + SLASH + payload.get(RESULT).get(2).asText());
            PatchPanelService pps = get(PatchPanelService.class);

            boolean done = pps.addPatch(cp1, cp2);
            String message;
            if (done) {
                message = "Patch has been created";
                previous.add(cp1);
                previous.add(cp2);
            } else {
                message = "One or both of these ports are already in use";
                if (cp1.port().equals(cp2.port())) {
                    message = "Both ports can not be the same";
                }
            }
            payload.put(MESSAGE, message);
            sendMessage(SAMPLE_CUSTOM_DATA_RESP2, payload);

        }
    }

    private final class ThirdDataRequestHandler extends RequestHandler {
        private ThirdDataRequestHandler() {
            super(SAMPLE_CUSTOM_DATA_REQ3);
        }

        @Override
        public void process(ObjectNode payload) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < previous.size(); i++) {
                sb.append(previous.get(i)).append(i % 2 == 0 ? WITH : EOL);
            }
            payload.put(CPOINTS, sb.toString());
            sendMessage(SAMPLE_CUSTOM_DATA_RESP3, payload);
        }
    }

}
