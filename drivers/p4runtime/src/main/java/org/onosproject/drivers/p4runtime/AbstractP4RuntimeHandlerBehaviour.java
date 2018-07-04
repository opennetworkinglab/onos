/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.drivers.p4runtime;

import io.grpc.StatusRuntimeException;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.model.PiPipeconfId;
import org.onosproject.net.pi.model.PiPipelineInterpreter;
import org.onosproject.net.pi.service.PiPipeconfService;
import org.onosproject.net.pi.service.PiTranslationService;
import org.onosproject.p4runtime.api.P4RuntimeClient;
import org.onosproject.p4runtime.api.P4RuntimeController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Abstract implementation of a behaviour handler for a P4Runtime device.
 */
public class AbstractP4RuntimeHandlerBehaviour extends AbstractHandlerBehaviour {

    // Default timeout in seconds for device operations.
    private static final String DEVICE_REQ_TIMEOUT = "deviceRequestTimeout";
    private static final int DEFAULT_DEVICE_REQ_TIMEOUT = 60;

    public static final String P4RUNTIME_SERVER_ADDR_KEY = "p4runtime_ip";
    public static final String P4RUNTIME_SERVER_PORT_KEY = "p4runtime_port";
    public static final String P4RUNTIME_DEVICE_ID_KEY = "p4runtime_deviceId";

    protected final Logger log = LoggerFactory.getLogger(getClass());

    // Initialized by setupBehaviour()
    protected DeviceId deviceId;
    protected DeviceService deviceService;
    protected Device device;
    protected P4RuntimeController controller;
    protected PiPipeconf pipeconf;
    protected P4RuntimeClient client;
    protected PiTranslationService piTranslationService;

    /**
     * Initializes this behaviour attributes. Returns true if the operation was
     * successful, false otherwise. This method assumes that the P4runtime
     * controller already has a client for this device and that the device has
     * been created in the core.
     *
     * @return true if successful, false otherwise
     */
    protected boolean setupBehaviour() {
        deviceId = handler().data().deviceId();

        deviceService = handler().get(DeviceService.class);
        device = deviceService.getDevice(deviceId);
        if (device == null) {
            log.warn("Unable to find device with id {}, aborting operation", deviceId);
            return false;
        }

        controller = handler().get(P4RuntimeController.class);
        client = controller.getClient(deviceId);
        if (client == null) {
            log.warn("Unable to find client for {}, aborting operation", deviceId);
            return false;
        }

        PiPipeconfService piPipeconfService = handler().get(PiPipeconfService.class);
        if (!piPipeconfService.ofDevice(deviceId).isPresent()) {
            log.warn("Unable to get assigned pipeconf for {} (mapping " +
                             "missing in PiPipeconfService), aborting operation",
                     deviceId);
            return false;
        }
        PiPipeconfId pipeconfId = piPipeconfService.ofDevice(deviceId).get();
        if (!piPipeconfService.getPipeconf(pipeconfId).isPresent()) {
            log.warn("Cannot find any pipeconf with ID '{}' ({}), aborting operation", pipeconfId, deviceId);
            return false;
        }
        pipeconf = piPipeconfService.getPipeconf(pipeconfId).get();

        piTranslationService = handler().get(PiTranslationService.class);

        return true;
    }

    /**
     * Returns an instance of the interpreter implementation for this device,
     * null if an interpreter cannot be retrieved.
     *
     * @return interpreter or null
     */
    PiPipelineInterpreter getInterpreter() {
        if (!device.is(PiPipelineInterpreter.class)) {
            log.warn("Unable to get interpreter for {}, missing behaviour",
                     deviceId);
            return null;
        }
        return device.as(PiPipelineInterpreter.class);
    }

    /**
     * Returns a P4Runtime client for this device, null if such client cannot be
     * created.
     *
     * @return client or null
     */
    P4RuntimeClient createClient() {
        deviceId = handler().data().deviceId();
        controller = handler().get(P4RuntimeController.class);

        final String serverAddr = this.data().value(P4RUNTIME_SERVER_ADDR_KEY);
        final String serverPortString = this.data().value(P4RUNTIME_SERVER_PORT_KEY);
        final String p4DeviceIdString = this.data().value(P4RUNTIME_DEVICE_ID_KEY);

        if (serverAddr == null || serverPortString == null || p4DeviceIdString == null) {
            log.warn("Unable to create client for {}, missing driver data key (required is {}, {}, and {})",
                     deviceId, P4RUNTIME_SERVER_ADDR_KEY, P4RUNTIME_SERVER_PORT_KEY, P4RUNTIME_DEVICE_ID_KEY);
            return null;
        }

        final int serverPort;
        final long p4DeviceId;

        try {
            serverPort = Integer.parseUnsignedInt(serverPortString);
        } catch (NumberFormatException e) {
            log.error("{} is not a valid P4Runtime port number", serverPortString);
            return null;
        }
        try {
            p4DeviceId = Long.parseUnsignedLong(p4DeviceIdString);
        } catch (NumberFormatException e) {
            log.error("{} is not a valid P4Runtime-internal device ID", p4DeviceIdString);
            return null;
        }

        if (!controller.createClient(deviceId, serverAddr, serverPort, p4DeviceId)) {
            log.warn("Unable to create client for {}, aborting operation", deviceId);
            return null;
        }

        return controller.getClient(deviceId);
    }

    /**
     * Returns the value of the given driver property, if present, otherwise
     * returns the given default value.
     *
     * @param propName   property name
     * @param defaultVal default value
     * @return boolean
     */
    protected boolean driverBoolProperty(String propName, boolean defaultVal) {
        checkNotNull(propName);
        if (handler().driver().getProperty(propName) == null) {
            return defaultVal;
        } else {
            return Boolean.parseBoolean(handler().driver().getProperty(propName));
        }
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
