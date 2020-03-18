/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.drivers.server;

import org.onlab.packet.IpAddress;

import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.ControllerInfo;
import org.onosproject.protocol.rest.RestSBController;
import org.onosproject.protocol.rest.RestSBDevice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.sse.InboundSseEvent;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.onosproject.drivers.server.Constants.PARAM_CTRL;
import static org.onosproject.drivers.server.Constants.PARAM_CTRL_IP;
import static org.onosproject.drivers.server.Constants.PARAM_CTRL_PORT;
import static org.onosproject.drivers.server.Constants.PARAM_CTRL_TYPE;

/**
 * Test class for REST SB controller.
 */
public class RestSBControllerMock implements RestSBController {
    /**
     * Local memory of devices.
     */
    private Map<DeviceId, RestSBDevice> deviceMap = new ConcurrentHashMap<>();

    /**
     * Objects to populate our memory.
     */
    private DeviceId restDeviceId1;
    private RestSBDevice restDevice1;
    private static List<ControllerInfo> controllers;

    public RestSBControllerMock() {
        restDeviceId1 = TestConfig.REST_DEV_ID1;
        assertThat(restDeviceId1, notNullValue());

        restDevice1 = TestConfig.REST_DEV1;
        assertThat(restDevice1, notNullValue());

        controllers = TestConfig.CONTROLLERS;
        assertThat(controllers, notNullValue());

        deviceMap.put(restDeviceId1, restDevice1);
    }

    @Override
    public void addProxiedDevice(DeviceId deviceId, RestSBDevice proxy) {
        return;
    }

    @Override
    public void removeProxiedDevice(DeviceId deviceId) {
        return;
    }

    @Override
    public Set<DeviceId> getProxiedDevices(DeviceId proxyId) {
        return null;
    }

    @Override
    public RestSBDevice getProxySBDevice(DeviceId deviceId) {
        return null;
    }

    @Override
    public Map<DeviceId, RestSBDevice> getDevices() {
        return null;
    }

    @Override
    public RestSBDevice getDevice(DeviceId deviceInfo) {
        return null;
    }

    @Override
    public RestSBDevice getDevice(IpAddress ip, int port) {
        return null;
    }

    @Override
    public void addDevice(RestSBDevice device) {
        return;
    }

    @Override
    public void removeDevice(DeviceId deviceId) {
        return;
    }

    @Override
    public InputStream get(DeviceId device, String request, MediaType mediaType) {
        /**
         * We fake the HTTP get in order to
         * emulate the expected response.
         */
        ObjectMapper mapper = new ObjectMapper();

        // Create the object node to host the data
        ObjectNode sendObjNode = mapper.createObjectNode();

        // Insert header
        ArrayNode ctrlsArrayNode = sendObjNode.putArray(PARAM_CTRL);

        // Add each controller's information object
        for (ControllerInfo ctrl : controllers) {
            ObjectNode ctrlObjNode = mapper.createObjectNode();
            ctrlObjNode.put(PARAM_CTRL_IP,   ctrl.ip().toString());
            ctrlObjNode.put(PARAM_CTRL_PORT, ctrl.port());
            ctrlObjNode.put(PARAM_CTRL_TYPE, ctrl.type());
            ctrlsArrayNode.add(ctrlObjNode);
        }

        return new ByteArrayInputStream(sendObjNode.toString().getBytes());
    }

    @Override
    public int post(DeviceId device, String request, InputStream payload, MediaType mediaType) {
        return Response.Status.OK.getStatusCode();
    }

    @Override
    public int put(DeviceId device, String request, InputStream payload, MediaType mediaType) {
        return Response.Status.OK.getStatusCode();
    }

    @Override
    public int patch(DeviceId device, String request, InputStream payload, MediaType mediaType) {
        return Response.Status.OK.getStatusCode();
    }

    @Override
    public int delete(DeviceId device, String request, InputStream payload, MediaType mediaType) {
        return Response.Status.OK.getStatusCode();
    }

    @Override
    public <T> T post(DeviceId device, String request, InputStream payload,
        MediaType mediaType, Class<T> responseClass) {
        return null;
    }

    @Override
    public void startServerSentEvents(DeviceId deviceId, String eventsUrl) {
        return;
    }

    @Override
    public int getServerSentEvents(DeviceId deviceId, String request,
                                   Consumer<InboundSseEvent> onEvent, Consumer<Throwable> onError) {
        return 204;
    }

    @Override
    public int cancelServerSentEvents(DeviceId deviceId) {
        return 200;
    }

}