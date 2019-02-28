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

import com.google.common.base.Strings;
import io.grpc.StatusRuntimeException;
import org.onosproject.gnmi.api.GnmiClient;
import org.onosproject.gnmi.api.GnmiClientKey;
import org.onosproject.gnmi.api.GnmiController;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.BasicDeviceConfig;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
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

    GnmiClient getClientByKey() {
        final GnmiClientKey clientKey = clientKey();
        if (clientKey == null) {
            return null;
        }
        return handler().get(GnmiController.class).getClient(clientKey);
    }

    protected GnmiClientKey clientKey() {
        deviceId = handler().data().deviceId();

        final BasicDeviceConfig cfg = handler().get(NetworkConfigService.class)
                .getConfig(deviceId, BasicDeviceConfig.class);
        if (cfg == null || Strings.isNullOrEmpty(cfg.managementAddress())) {
            log.error("Missing or invalid config for {}, cannot derive " +
                              "gNMI server endpoints", deviceId);
            return null;
        }

        try {
            return new GnmiClientKey(
                    deviceId, new URI(cfg.managementAddress()));
        } catch (URISyntaxException e) {
            log.error("Management address of {} is not a valid URI: {}",
                      deviceId, cfg.managementAddress());
            return null;
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
