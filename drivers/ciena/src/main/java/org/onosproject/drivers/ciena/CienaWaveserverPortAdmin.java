/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.drivers.ciena;

import org.onlab.util.Tools;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.PortAdmin;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.protocol.rest.RestSBController;
import org.slf4j.Logger;

import javax.ws.rs.core.MediaType;
import java.util.concurrent.CompletableFuture;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import javax.ws.rs.core.Response.Status;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;


public class CienaWaveserverPortAdmin extends AbstractHandlerBehaviour
        implements PortAdmin {
    private final Logger log = getLogger(getClass());
    private static final String APP_JSON = "application/json";
    private static final String ENABLE = "enabled";
    private static final String DISABLE = "disabled";

    private final String generateUri(long number) {
        return String.format("ws-ptps/ptps/%d/state", number);
    }

    private final String generateRequest(String state) {
        String request = "{\n" +
                "\"state\": {\n" +
                "\"admin-state\": \"" + state + "\"\n}\n}";
        log.debug("generated request: \n{}", request);
        return request;
    }

    private final boolean put(long number, String state) {
        String uri = generateUri(number);
        String request = generateRequest(state);
        DeviceId deviceId = handler().data().deviceId();
        RestSBController controller =
                checkNotNull(handler().get(RestSBController.class));
        InputStream payload =
                new ByteArrayInputStream(request.getBytes(StandardCharsets.UTF_8));
        int response = controller.put(deviceId, uri, payload,
                                      MediaType.valueOf(APP_JSON));
        log.debug("response: {}", response);
        // expecting 204/NO_CONTENT_RESPONSE as successful response
        return response == Status.NO_CONTENT.getStatusCode();
    }

    // returns null if specified port number was not a line side port
    private Long getLineSidePort(PortNumber number) {
        DeviceService deviceService = checkNotNull(handler().get(DeviceService.class));
        DeviceId deviceId = handler().data().deviceId();
        Port port = deviceService.getPort(deviceId, number);
        if (port != null) {
            String channelId = port.annotations().value(AnnotationKeys.CHANNEL_ID);
            // any port that has channel is lineSidePort and will have TX and RX
            if (channelId != null) {
                String portName = port.annotations().value(AnnotationKeys.PORT_NAME);
                // last three characters of portName will always be " TX" or " RX"
                portName = portName.substring(0, portName.length() - 3);
                log.debug("port number {} is mapped to {} lineside port",
                          number, portName);
                return new Long(portName);
            }
        }
        // not a line-side port
        return null;
    }

    @Override
    public CompletableFuture<Boolean> disable(PortNumber number) {
        log.debug("disabling port {}", number);
        Long lineSidePort = getLineSidePort(number);
        long devicePortNum;
        if (lineSidePort != null) {
            devicePortNum = lineSidePort.longValue();
        } else {
            devicePortNum = number.toLong();
        }
        CompletableFuture<Boolean> result =
                CompletableFuture.completedFuture(put(devicePortNum, DISABLE));
        return result;
    }

    @Override
    public CompletableFuture<Boolean> enable(PortNumber number) {
        log.debug("enabling port {}", number);
        Long lineSidePort = getLineSidePort(number);
        long devicePortNum;
        if (lineSidePort != null) {
            devicePortNum = lineSidePort.longValue();
        } else {
            devicePortNum = number.toLong();
        }
        CompletableFuture<Boolean> result =
                CompletableFuture.completedFuture(put(devicePortNum, ENABLE));
        return result;
    }

    @Override
    public CompletableFuture<Boolean> isEnabled(PortNumber number) {
        return Tools.exceptionalFuture(
                new UnsupportedOperationException("isEnabled is not supported"));

    }
}
