/*
 * Copyright 2020-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onosproject.drivers.server;

import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.device.DeviceHandshaker;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TableStatisticsEntry;
import org.onosproject.mastership.MastershipService;
import org.onosproject.protocol.rest.RestSBDevice;

import org.slf4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.onosproject.drivers.server.Constants.JSON;
import static org.onosproject.drivers.server.Constants.PARAM_CONNECTION_STATUS;
import static org.onosproject.drivers.server.Constants.MSG_HANDLER_NULL;
import static org.onosproject.drivers.server.Constants.MSG_DEVICE_ID_NULL;
import static org.onosproject.drivers.server.Constants.MSG_DEVICE_NULL;
import static org.onosproject.drivers.server.Constants.URL_SRV_PROBE_CONNECT;
import static org.onosproject.drivers.server.Constants.URL_SRV_PROBE_DISCONNECT;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of device handshaker behaviour for server devices.
 */
public class ServerHandshaker
        extends BasicServerDriver
        implements DeviceHandshaker {

    private final Logger log = getLogger(getClass());

    public ServerHandshaker() {
        super();
        log.debug("Started");
    }

    @Override
    public DriverHandler handler() {
        return super.getHandler();
    }

    @Override
    public void setHandler(DriverHandler handler) {
        checkNotNull(handler, MSG_HANDLER_NULL);
        this.handler = handler;
    }

    @Override
    public boolean connect() throws IllegalStateException {
        // Retrieve the device ID from the handler
        DeviceId deviceId = super.getDeviceId();
        checkNotNull(deviceId, MSG_DEVICE_ID_NULL);

        // Create an object node
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode sendObjNode = mapper.createObjectNode();
        sendObjNode.put(PARAM_CONNECTION_STATUS, "connect");

        // Post the connect message to the server
        int response = getController().post(
            deviceId, URL_SRV_PROBE_CONNECT,
            new ByteArrayInputStream(sendObjNode.toString().getBytes()), JSON);

        // Upon an error, return an empty set of rules
        if (!checkStatusCode(response)) {
            log.error("Failed to connect to device {}", deviceId);
            return false;
        }

        log.info("Successfully connected to device {}", deviceId);

        return true;
    }

    @Override
    public boolean hasConnection() {
        return isReachable();
    }

    @Override
    public void disconnect() {
        // Retrieve the device ID from the handler
        DeviceId deviceId = super.getDeviceId();
        checkNotNull(deviceId, MSG_DEVICE_ID_NULL);

        // Create an object node
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode sendObjNode = mapper.createObjectNode();
        sendObjNode.put(PARAM_CONNECTION_STATUS, "disconnect");

        // Post the disconnect message to the server
        int response = getController().post(
            deviceId, URL_SRV_PROBE_DISCONNECT,
            new ByteArrayInputStream(sendObjNode.toString().getBytes()), JSON);

        if (!checkStatusCode(response)) {
            log.error("Failed to disconnect from device {}", deviceId);
            return;
        }

        log.info("Successfully disconnected from device {}", deviceId);

        return;
    }

    @Override
    public boolean isReachable() {
        // Retrieve the device ID from the handler
        DeviceId deviceId = super.getDeviceId();
        checkNotNull(deviceId, MSG_DEVICE_ID_NULL);

        // Get the device
        RestSBDevice device = super.getDevice(deviceId);
        checkNotNull(device, MSG_DEVICE_NULL);

        return deviceIsActive(device);
    }

    @Override
    public boolean isAvailable() {
        return isReachable();
    }

    @Override
    public CompletableFuture<Boolean> probeReachability() {
        // Retrieve the device ID from the handler
        DeviceId deviceId = super.getDeviceId();
        checkNotNull(deviceId, MSG_DEVICE_ID_NULL);

        // Probe the driver to ask for flow rule service
        FlowRuleService flowService = getHandler().get(FlowRuleService.class);
        List<TableStatisticsEntry> tableStats = Lists.newArrayList(
            flowService.getFlowTableStatistics(deviceId));

        // If no statistics fetched, the server is not reachable
        return completedFuture(tableStats.isEmpty() ? false : true);
    }

    @Override
    public CompletableFuture<Boolean> probeAvailability() {
        return probeReachability();
    }

    @Override
    public void roleChanged(MastershipRole newRole) {
        throw new UnsupportedOperationException("Mastership operation not supported");
    }

    @Override
    public MastershipRole getRole() {
        // Retrieve the device ID from the handler
        DeviceId deviceId = super.getDeviceId();
        checkNotNull(deviceId, MSG_DEVICE_ID_NULL);

        // Probe the driver to ask for mastership service
        MastershipService mastershipService = getHandler().get(MastershipService.class);
        return mastershipService.getLocalRole(deviceId);
    }

}
