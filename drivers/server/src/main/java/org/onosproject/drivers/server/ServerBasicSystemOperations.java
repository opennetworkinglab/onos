/*
 * Copyright 2020-present Open Networking Foundation
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

import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.BasicSystemOperations;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.protocol.rest.RestSBDevice;

import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javax.ws.rs.ProcessingException;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.onosproject.drivers.server.Constants.JSON;
import static org.onosproject.drivers.server.Constants.MSG_HANDLER_NULL;
import static org.onosproject.drivers.server.Constants.MSG_DEVICE_ID_NULL;
import static org.onosproject.drivers.server.Constants.MSG_DEVICE_NULL;
import static org.onosproject.drivers.server.Constants.PARAM_TIME;
import static org.onosproject.drivers.server.Constants.URL_SRV_TIME_DISCOVERY;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of basic system operations' behaviour for server devices.
 */
public class ServerBasicSystemOperations
        extends BasicServerDriver
        implements BasicSystemOperations {

    private final Logger log = getLogger(getClass());

    public ServerBasicSystemOperations() {
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
    public CompletableFuture<Boolean> reboot() {
        throw new UnsupportedOperationException("Reboot operation not supported");
    }

    @Override
    public CompletableFuture<Long> time() {
        // Retrieve the device ID from the handler
        DeviceId deviceId = super.getDeviceId();
        checkNotNull(deviceId, MSG_DEVICE_ID_NULL);

        // Get the device
        RestSBDevice device = super.getDevice(deviceId);
        checkNotNull(device, MSG_DEVICE_NULL);

        // Hit the path that provides the server's time
        InputStream response = null;
        try {
            response = getController().get(deviceId, URL_SRV_TIME_DISCOVERY, JSON);
        } catch (ProcessingException pEx) {
            log.error("Failed to get the time of device: {}", deviceId);
            return null;
        }

        // Load the JSON into object
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> jsonMap = null;
        JsonNode jsonNode = null;
        try {
            jsonMap  = mapper.readValue(response, Map.class);
            jsonNode = mapper.convertValue(jsonMap, JsonNode.class);
        } catch (IOException ioEx) {
            log.error("Failed to discover the device details of: {}", deviceId);
            return null;
        }

        if (jsonNode == null) {
            log.error("Failed to discover the device details of: {}", deviceId);
            return null;
        }

        long time = jsonNode.path(PARAM_TIME).asLong();
        checkArgument(time > 0, "Invalid time format: {}", time);

        return completedFuture(new Long(time));
    }

}
