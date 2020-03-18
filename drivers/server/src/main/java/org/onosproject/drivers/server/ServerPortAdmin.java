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

import org.onosproject.drivers.server.devices.RestServerSBDevice;
import org.onosproject.drivers.server.devices.nic.NicDevice;

import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.PortAdmin;
import org.onosproject.net.driver.DriverHandler;

import org.slf4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.ByteArrayInputStream;
import java.util.concurrent.CompletableFuture;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.onosproject.drivers.server.Constants.JSON;
import static org.onosproject.drivers.server.Constants.MSG_HANDLER_NULL;
import static org.onosproject.drivers.server.Constants.MSG_DEVICE_ID_NULL;
import static org.onosproject.drivers.server.Constants.PARAM_NIC_PORT;
import static org.onosproject.drivers.server.Constants.PARAM_NIC_PORT_STATUS;
import static org.onosproject.drivers.server.Constants.URL_NIC_PORT_ADMIN;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of port admin behaviour for server devices.
 */
public class ServerPortAdmin
        extends BasicServerDriver
        implements PortAdmin {

    private final Logger log = getLogger(getClass());

    public ServerPortAdmin() {
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
    public CompletableFuture<Boolean> enable(PortNumber number) {
        return doEnable(number, true);
    }

    @Override
    public CompletableFuture<Boolean> disable(PortNumber number) {
        return doEnable(number, false);
    }

    @Override
    public CompletableFuture<Boolean> isEnabled(PortNumber number) {
        // Retrieve the device ID
        DeviceId deviceId = super.getDeviceId();
        checkNotNull(deviceId, MSG_DEVICE_ID_NULL);

        // ...and the device itself
        RestServerSBDevice device = null;
        try {
            device = (RestServerSBDevice) getDevice(deviceId);
        } catch (ClassCastException ccEx) {
            log.error("Failed to discover ports for device {}", deviceId);
            return completedFuture(false);
        }

        // Iterate server's NICs to find the correct port
        for (NicDevice nic : device.nics()) {
            if (nic.portNumber() == number.toLong()) {
                return completedFuture(nic.status());
            }
        }

        return completedFuture(false);
    }

    /**
     * Perform a NIC port management command.
     *
     * @param portNumber port number to manage
     * @param enabled management flag (true to enable, false to disable the port)
     * @return boolean status
     */
    private CompletableFuture<Boolean> doEnable(PortNumber portNumber, boolean enabled) {
        // Retrieve the device ID from the handler
        DeviceId deviceId = super.getDeviceId();
        checkNotNull(deviceId, MSG_DEVICE_ID_NULL);

        // Create an object node with the port status
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode sendObjNode = mapper.createObjectNode();
        sendObjNode.put(PARAM_NIC_PORT, portNumber.toLong());
        sendObjNode.put(PARAM_NIC_PORT_STATUS, enabled ? "enable" : "disable");

        // Post the connect message to the server
        int response = getController().post(
            deviceId, URL_NIC_PORT_ADMIN,
            new ByteArrayInputStream(sendObjNode.toString().getBytes()), JSON);

        // Upon an error, return an empty set of rules
        if (!checkStatusCode(response)) {
            log.error("Failed to connect to device {}", deviceId);
            return completedFuture(false);
        }

        log.info("Successfully sent port {} command to device {}",
            enabled ? "enable" : "disable", deviceId);

        return completedFuture(true);
    }

}
