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

import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.NettyChannelBuilder;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.service.PiPipeconfService;
import org.onosproject.net.pi.service.PiTranslationService;
import org.onosproject.p4runtime.api.P4RuntimeClient;
import org.onosproject.p4runtime.api.P4RuntimeController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Abstract implementation of a behaviour handler for a P4Runtime device.
 */
public class AbstractP4RuntimeHandlerBehaviour extends AbstractHandlerBehaviour {

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
        if (!controller.hasClient(deviceId)) {
            log.warn("Unable to find client for {}, aborting operation", deviceId);
            return false;
        }
        client = controller.getClient(deviceId);

        PiPipeconfService piPipeconfService = handler().get(PiPipeconfService.class);
        if (!piPipeconfService.ofDevice(deviceId).isPresent() ||
                !piPipeconfService.getPipeconf(piPipeconfService.ofDevice(deviceId).get()).isPresent()) {
            log.warn("Unable to get the pipeconf of {}, aborting operation", deviceId);
            return false;
        }
        pipeconf = piPipeconfService.getPipeconf(piPipeconfService.ofDevice(deviceId).get()).get();

        piTranslationService = handler().get(PiTranslationService.class);

        return true;
    }

    /**
     * Create a P4Runtime client for this device. Returns true if the operation
     * was successful, false otherwise.
     *
     * @return true if successful, false otherwise
     */
    protected boolean createClient() {
        deviceId = handler().data().deviceId();
        controller = handler().get(P4RuntimeController.class);

        String serverAddr = this.data().value(P4RUNTIME_SERVER_ADDR_KEY);
        String serverPortString = this.data().value(P4RUNTIME_SERVER_PORT_KEY);
        String p4DeviceIdString = this.data().value(P4RUNTIME_DEVICE_ID_KEY);

        if (serverAddr == null || serverPortString == null || p4DeviceIdString == null) {
            log.warn("Unable to create client for {}, missing driver data key (required is {}, {}, and {})",
                     deviceId, P4RUNTIME_SERVER_ADDR_KEY, P4RUNTIME_SERVER_PORT_KEY, P4RUNTIME_DEVICE_ID_KEY);
            return false;
        }

        ManagedChannelBuilder channelBuilder = NettyChannelBuilder
                .forAddress(serverAddr, Integer.valueOf(serverPortString))
                .usePlaintext(true);

        if (!controller.createClient(deviceId, Long.parseUnsignedLong(p4DeviceIdString), channelBuilder)) {
            log.warn("Unable to create client for {}, aborting operation", deviceId);
            return false;
        }

        return true;
    }

    /**
     * Returns the value of the given driver property, if present,
     * otherwise returns the given default value.
     *
     * @param propName property name
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
}
