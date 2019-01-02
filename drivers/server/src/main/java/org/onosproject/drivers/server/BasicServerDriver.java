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

import org.onosproject.drivers.server.devices.RestServerSBDevice;

import org.onosproject.net.DeviceId;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.protocol.rest.RestSBController;
import org.onosproject.protocol.rest.RestSBDevice;

import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.EnumSet;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.slf4j.LoggerFactory.getLogger;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The basic functionality of the server driver.
 */
public class BasicServerDriver extends AbstractHandlerBehaviour {

    private final Logger log = getLogger(getClass());

    /**
     * Resource endpoints of the server agent (REST server-side).
     */
    public    static final MediaType  JSON = MediaType.valueOf(MediaType.APPLICATION_JSON);
    protected static final String ROOT_URL = "";
    protected static final String SLASH = "/";
    public    static final String BASE_URL = ROOT_URL + SLASH + "metron";

    /**
     * Common parameters to be exchanged with the server's agent.
     */
    public static final String PARAM_ID             = "id";
    public static final String PARAM_NICS           = "nics";
    public static final String PARAM_CPUS           = "cpus";
    public static final String NIC_PARAM_RX_FILTER  = "rxFilter";
    public static final String NIC_PARAM_RX_METHOD  = "method";


    /**
     * Successful HTTP status codes.
     */
    private static final int STATUS_OK = Response.Status.OK.getStatusCode();
    private static final int STATUS_CREATED = Response.Status.CREATED.getStatusCode();
    private static final int STATUS_ACCEPTED = Response.Status.ACCEPTED.getStatusCode();

    /**
     * Messages for error handlers.
     */
    protected static final String MASTERSHIP_NULL = "Mastership service is null";
    protected static final String CONTROLLER_NULL = "RestSB controller is null";
    protected static final String DEVICE_ID_NULL  = "Device ID cannot be null";
    protected static final String HANDLER_NULL    = "Handler cannot be null";
    protected static final String DEVICE_NULL     = "Device cannot be null";

    /**
     * A unique controller that handles the REST-based communication.
     */
    protected static RestSBController controller = null;
    protected static DriverHandler       handler = null;
    private static final Object CONTROLLER_LOCK = new Object();

    public BasicServerDriver() {};

    /**
     * Retrieve an instance of the driver handler.
     *
     * @return DriverHandler instance
     */
    protected DriverHandler getHandler() {
        synchronized (CONTROLLER_LOCK) {
            handler = handler();
            checkNotNull(handler, HANDLER_NULL);
        }

        return handler;
    }

    /**
     * Retrieve an instance of the REST SB controller.
     *
     * @return RestSBController instance
     */
    public RestSBController getController() {
        synchronized (CONTROLLER_LOCK) {
            if (controller == null) {
                controller = getHandler().get(RestSBController.class);
                checkNotNull(controller, CONTROLLER_NULL);
            }
        }

        return controller;
    }

    /**
     * Finds the NIC name that corresponds to a device's port number.
     *
     * @param deviceId a device ID
     * @param port a NIC port number
     * @return device's NIC name
     */
    public static String findNicInterfaceWithPort(DeviceId deviceId, long port) {
        if (controller == null) {
            return null;
        }

        RestServerSBDevice device = null;
        try {
            device = (RestServerSBDevice) controller.getDevice(deviceId);
        } catch (ClassCastException ccEx) {
            return null;
        }
        checkNotNull(device, DEVICE_NULL);

        return device.portNameFromNumber(port);
    }

    /**
     * Return all the enumeration's types in a space-separated string.
     *
     * @param <E> the expected class of the enum
     * @param enumType the enum class to get its types
     * @return String with all enumeration types
     */
    public static <E extends Enum<E>> String enumTypesToString(Class<E> enumType) {
        String allTypes = "";
        for (E en : EnumSet.allOf(enumType)) {
            allTypes += en.toString() + " ";
        }

        return allTypes.trim();
    }

    /**
     * Return a string value after reading the input
     * attribute from the input JSON node.
     *
     * @param jsonNode JSON node to read from
     * @param attribute to lookup in the JSON node
     * @return string value mapped to the attribute
     */
    public static String get(JsonNode jsonNode, String attribute) {
        if (jsonNode == null || (attribute == null || attribute.isEmpty())) {
            return null;
        }

        String result = "";

        try {
            result = jsonNode.get(attribute).asText();
        } catch (Exception ex) {
            throw new IllegalArgumentException(
                "Failed to read JSON attribute: " + attribute
            );
        }

        return result;
    }

    /**
     * Assess a given HTTP status code.
     *
     * @param statusCode the HTTP status code to check
     * @return boolean status (success or failure)
     */
    public static boolean checkStatusCode(int statusCode) {
        if (statusCode == STATUS_OK || statusCode == STATUS_CREATED || statusCode == STATUS_ACCEPTED) {
            return true;
        }

        return false;
    }

    /**
     * Raise a connect event by setting the
     * activity flag of this device.
     *
     * @param device a device to connect
     */
    protected void raiseDeviceReconnect(RestSBDevice device) {
        // Already done!
        if (device.isActive()) {
            return;
        }

        log.debug("Setting device {} active", device.deviceId());
        device.setActive(true);
    }

    /**
     * Upon a failure to contact a device, the driver
     * raises a disconnect event by resetting the
     * activity flag of this device.
     *
     * @param device a device to disconnect
     */
    protected void raiseDeviceDisconnect(RestSBDevice device) {
        // Already done!
        if (!device.isActive()) {
            return;
        }

        log.info("Setting device {} inactive", device.deviceId());
        device.setActive(false);
    }

}
