/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.drivers.gnmi;

import io.grpc.StatusRuntimeException;
import org.onosproject.gnmi.api.GnmiClient;
import org.onosproject.gnmi.api.GnmiClientKey;
import org.onosproject.gnmi.api.GnmiController;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Abstract implementation of a behaviour handler for a gNMI device.
 */
public class AbstractGnmiHandlerBehaviour extends AbstractHandlerBehaviour {

    // Default timeout in seconds for device operations.
    private static final String DEVICE_REQ_TIMEOUT = "deviceRequestTimeout";
    private static final int DEFAULT_DEVICE_REQ_TIMEOUT = 60;

    private static final String GNMI_SERVER_ADDR_KEY = "gnmi_ip";
    private static final String GNMI_SERVER_PORT_KEY = "gnmi_port";

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected DeviceId deviceId;
    protected DeviceService deviceService;
    protected Device device;
    protected GnmiController controller;
    protected GnmiClient client;

    protected boolean setupBehaviour() {
        deviceId = handler().data().deviceId();
        deviceService = handler().get(DeviceService.class);
        controller = handler().get(GnmiController.class);
        client = controller.getClient(deviceId);

        if (client == null) {
            log.warn("Unable to find client for {}, aborting operation", deviceId);
            return false;
        }

        return true;
    }

    GnmiClient createClient() {
        deviceId = handler().data().deviceId();
        controller = handler().get(GnmiController.class);

        final String serverAddr = this.data().value(GNMI_SERVER_ADDR_KEY);
        final String serverPortString = this.data().value(GNMI_SERVER_PORT_KEY);

        if (serverAddr == null || serverPortString == null) {
            log.warn("Unable to create client for {}, missing driver data key (required is {} and {})",
                    deviceId, GNMI_SERVER_ADDR_KEY, GNMI_SERVER_PORT_KEY);
            return null;
        }

        final int serverPort;
        try {
            serverPort = Integer.parseUnsignedInt(serverPortString);
        } catch (NumberFormatException e) {
            log.error("{} is not a valid port number", serverPortString);
            return null;
        }
        GnmiClientKey clientKey = new GnmiClientKey(deviceId, serverAddr, serverPort);
        if (!controller.createClient(clientKey)) {
            log.warn("Unable to create client for {}, aborting operation", deviceId);
            return null;
        }
        return controller.getClient(deviceId);
    }

    /**
     * Returns the device request timeout driver property, or a default value
     * if the property is not present or cannot be parsed.
     *
     * @return timeout value
     */
    private int getDeviceRequestTimeout() {
        final String timeout = handler().driver()
                .getProperty(DEVICE_REQ_TIMEOUT);
        if (timeout == null) {
            return DEFAULT_DEVICE_REQ_TIMEOUT;
        } else {
            try {
                return Integer.parseInt(timeout);
            } catch (NumberFormatException e) {
                log.error("{} driver property '{}' is not a number, using default value {}",
                        DEVICE_REQ_TIMEOUT, timeout, DEFAULT_DEVICE_REQ_TIMEOUT);
                return DEFAULT_DEVICE_REQ_TIMEOUT;
            }
        }
    }

    /**
     * Convenience method to get the result of a completable future while
     * setting a timeout and checking for exceptions.
     *
     * @param future        completable future
     * @param opDescription operation description to use in log messages. Should
     *                      be a sentence starting with a verb ending in -ing,
     *                      e.g. "reading...", "writing...", etc.
     * @param defaultValue  value to return if operation fails
     * @param <U>           type of returned value
     * @return future result or default value
     */
    <U> U getFutureWithDeadline(CompletableFuture<U> future, String opDescription,
                                U defaultValue) {
        try {
            return future.get(getDeviceRequestTimeout(), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("Exception while {} on {}", opDescription, deviceId);
        } catch (ExecutionException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof StatusRuntimeException) {
                final StatusRuntimeException grpcError = (StatusRuntimeException) cause;
                log.warn("Error while {} on {}: {}", opDescription, deviceId, grpcError.getMessage());
            } else {
                log.error("Exception while {} on {}", opDescription, deviceId, e.getCause());
            }
        } catch (TimeoutException e) {
            log.error("Operation TIMEOUT while {} on {}", opDescription, deviceId);
        }
        return defaultValue;
    }
}
