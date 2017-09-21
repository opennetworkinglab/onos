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

import org.onlab.util.Frequency;
import org.onosproject.net.DeviceId;
import org.onosproject.net.OchSignal;
import org.onosproject.net.PortNumber;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.protocol.rest.RestSBController;
import org.slf4j.Logger;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

public class CienaRestDevice {
    private DeviceId deviceId;
    private RestSBController controller;

    private final Logger log = getLogger(getClass());
    private static final String ENABLED = "enabled";
    private static final String DISABLED = "disabled";
    private static final Frequency BASE_FREQUENCY = Frequency.ofGHz(193_950);
    private static final int MULTIPLIER_OFFSET = 80;

    //URIs
    private static final String PORT_URI = "ws-ptps/ptps/%s";
    private static final String TRANSMITTER_URI = PORT_URI + "/properties/transmitter";
    private static final String PORT_STATE_URI = PORT_URI  + "/state";
    private static final String WAVELENGTH_URI = TRANSMITTER_URI + "/wavelength";
    private static final String FREQUENCY_URI = TRANSMITTER_URI + "/ciena-ws-ptp-modem:frequency";
    private static final String CHANNEL_URI = TRANSMITTER_URI + "/ciena-ws-ptp-modem:line-system-channel-number";

    public CienaRestDevice(DriverHandler handler) throws NullPointerException {
        deviceId = handler.data().deviceId();
        controller = checkNotNull(handler.get(RestSBController.class));
    }

    private final String genPortStateRequest(String state) {
        String request = "{\n" +
                "\"state\": {\n" +
                "\"admin-state\": \"" + state + "\"\n}\n}";
        log.debug("generated request: \n{}", request);
        return request;
    }

    private String genWavelengthChangeRequest(String wavelength) {
        String request = "{\n" +
                "\"wavelength\": {\n" +
                "\"value\": " + wavelength + "\n" +
                "}\n" +
                "}";
        log.debug("request:\n{}", request);
        return request;

    }

    private String genFrequencyChangeRequest(long wavelength) {
        String request = "{\n" +
                "\"ciena-ws-ptp-modem:frequency\": {\n" +
                "\"value\": " + Long.toString(wavelength) + "\n" +
                "}\n" +
                "}";
        log.debug("request:\n{}", request);
        return request;

    }

    private String genChannelChangeRequest(int channel) {
        String request = "{\n" +
                "\"ciena-ws-ptp-modem:line-system-channel-number\": " +
                Integer.toString(channel) + "\n}";
        log.debug("request:\n{}", request);
        return request;

    }


    private final String genUri(String uriFormat, PortNumber port) {
        return String.format(uriFormat, port.name());
    }

    private boolean changePortState(PortNumber number, String state) {
        log.debug("changing the port {} state to {}", number, state);
        String uri = genUri(PORT_STATE_URI, number);
        String request = genPortStateRequest(state);
        return putNoReply(uri, request);
    }

    public boolean disablePort(PortNumber number) {
        return changePortState(number, DISABLED);
    }

    public boolean enablePort(PortNumber number) {
        return changePortState(number, ENABLED);
    }

    public final boolean changeFrequency(OchSignal signal, PortNumber outPort) {
        String uri = genUri(FREQUENCY_URI, outPort);
        long frequency = toFrequency(signal);
        String request = genFrequencyChangeRequest(frequency);
        return putNoReply(uri, request);
    }

    public final boolean changeChannel(OchSignal signal, PortNumber outPort) {
        String uri = genUri(CHANNEL_URI, outPort);
        int channel = signal.spacingMultiplier() + MULTIPLIER_OFFSET;
        log.debug("channel is {} for port {} on device {}", channel, outPort.name(), deviceId);
        String request = genChannelChangeRequest(channel);
        return putNoReply(uri, request);
    }

    private final long toFrequency(OchSignal signal) {
        double frequency = BASE_FREQUENCY.asGHz() +
                (signal.channelSpacing().frequency().asGHz() * (double) signal.slotGranularity());
        return Double.valueOf(frequency).longValue();
    }

    public static int getMultiplierOffset() {
        return MULTIPLIER_OFFSET;
    }

    private int put(String uri, String request) {
        InputStream payload = new ByteArrayInputStream(request.getBytes(StandardCharsets.UTF_8));
        int response = controller.put(deviceId, uri, payload, MediaType.valueOf(MediaType.APPLICATION_JSON));
        log.debug("response: {}", response);
        return response;
    }

    private boolean putNoReply(String uri, String request) {
        return put(uri, request) == Response.Status.NO_CONTENT.getStatusCode();
    }

}
