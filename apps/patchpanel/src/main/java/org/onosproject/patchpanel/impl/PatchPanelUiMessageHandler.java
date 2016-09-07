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
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiMessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.onosproject.net.Device;
import org.onosproject.net.Port;
import org.onosproject.net.device.DeviceService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *  ONOS UI Custom-View message handler.
 *
 *  This class contains the request handlers that handle the response
 *  to each event. In this particular implementation the second message
 *  handler creates the patch and the first message handler loads the data
 */
public class PatchPanelUiMessageHandler extends UiMessageHandler {

    private static final String SAMPLE_CUSTOM_DATA_REQ = "sampleCustomDataRequest";
    private static final String SAMPLE_CUSTOM_DATA_RESP = "sampleCustomDataResponse";
    private static final String SAMPLE_CUSTOM_DATA_REQ2 = "sampleCustomDataRequest2";
    private static final String SAMPLE_CUSTOM_DATA_RESP2 = "sampleCustomDataResponse2";
    private static final String SAMPLE_CUSTOM_DATA_REQ3 = "sampleCustomDataRequest3";
    private static final String SAMPLE_CUSTOM_DATA_RESP3 = "sampleCustomDataResponse3";
    private String message = "";
    private String cpoints = "";
    private List<ConnectPoint> previous = new ArrayList<>();
    private static ConnectPoint cp1;
    private static ConnectPoint cp2;
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(new DataRequestHandler(), new SecondDataRequestHandler(), new ThirdDataRequestHandler());
    }

    // handler for data requests/events
    private final class DataRequestHandler extends RequestHandler {

        private DataRequestHandler() {
            super(SAMPLE_CUSTOM_DATA_REQ);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            DeviceService service = get(DeviceService.class);
            ObjectNode result = objectNode();
            ArrayNode cps = arrayNode();
            result.set("cps", cps);

            for (Device device : service.getDevices()) {
                cps.add(device.id().toString());
                for (Port port : service.getPorts(device.id())) {
                    if (!port.number().isLogical()) {
                        cps.add(port.number().toString());
                        log.info(device.id().toString() + "/" + port.number());
                    }
                }
            }
            sendMessage(SAMPLE_CUSTOM_DATA_RESP, 0, result);
        }
    }

    private final class SecondDataRequestHandler extends RequestHandler {

        private SecondDataRequestHandler() {
            super(SAMPLE_CUSTOM_DATA_REQ2);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            boolean done;
            String deviceId = payload.get("result").get(0).asText();
            cp1 = ConnectPoint.deviceConnectPoint(deviceId + "/" + payload.get("result").get(1).asText());
            cp2 = ConnectPoint.deviceConnectPoint(deviceId + "/" + payload.get("result").get(2).asText());
            PatchPanelService patchPanelService;
            patchPanelService = get(PatchPanelService.class);
            done = patchPanelService.addPatch(cp1, cp2);
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
            payload.put("message", message);
            sendMessage(SAMPLE_CUSTOM_DATA_RESP2, sid, payload);

        }
    }
    private final class ThirdDataRequestHandler extends RequestHandler {
        private ThirdDataRequestHandler() {
            super(SAMPLE_CUSTOM_DATA_REQ3);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            cpoints = "";
            for (int i = 0; i < previous.size(); i++) {
                if (i % 2 == 1) {
                    cpoints += previous.get(i) + "\n";
                } else {
                    cpoints += previous.get(i) + " with ";
                }
            }
            payload.put("cpoints", cpoints);
            sendMessage(SAMPLE_CUSTOM_DATA_RESP3, sid, payload);
        }
    }
}
