/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.drivers.bmv2;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.onlab.osgi.ServiceNotFoundException;
import org.onlab.packet.ChassisId;
import org.onosproject.bmv2.api.runtime.Bmv2DeviceAgent;
import org.onosproject.bmv2.api.runtime.Bmv2RuntimeException;
import org.onosproject.bmv2.api.service.Bmv2Controller;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.List;

import static org.onosproject.bmv2.api.runtime.Bmv2Device.*;
import static org.onosproject.net.Device.Type.SWITCH;

/**
 * Implementation of the device description discovery behaviour for BMv2.
 */
public class Bmv2DeviceDescriptionDiscovery extends AbstractHandlerBehaviour implements DeviceDescriptionDiscovery {

    private static final String JSON_CONFIG_MD5 = "bmv2JsonConfigMd5";
    private static final String PROCESS_INSTANCE_ID = "bmv2ProcessInstanceId";

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private Bmv2Controller controller;

    private boolean init() {
        try {
            controller = handler().get(Bmv2Controller.class);
            return true;
        } catch (ServiceNotFoundException e) {
            log.warn(e.getMessage());
            return false;
        }
    }

    @Override
    public DeviceDescription discoverDeviceDetails() {

        if (!init()) {
            return null;
        }

        DeviceId deviceId = handler().data().deviceId();

        Bmv2DeviceAgent deviceAgent;
        try {
            deviceAgent = controller.getAgent(deviceId);
        } catch (Bmv2RuntimeException e) {
            log.error("Failed to connect to Bmv2 device", e);
            return null;
        }

        DefaultAnnotations.Builder annotationsBuilder = DefaultAnnotations.builder();

        try {
            String md5 = deviceAgent.getJsonConfigMd5();
            BigInteger i = new BigInteger(1, md5.getBytes());
            annotationsBuilder.set(JSON_CONFIG_MD5, String.format("%1$032X", i).toLowerCase());
        } catch (Bmv2RuntimeException e) {
            log.warn("Unable to dump JSON configuration from {}: {}", deviceId, e.explain());
        }
        try {
            int instanceId = deviceAgent.getProcessInstanceId();
            annotationsBuilder.set(PROCESS_INSTANCE_ID, String.valueOf(instanceId));
        } catch (Bmv2RuntimeException e) {
            log.warn("Unable to get process instance ID from {}: {}", deviceId, e.explain());
        }

        annotationsBuilder.set(AnnotationKeys.PROTOCOL, PROTOCOL);

        return new DefaultDeviceDescription(deviceId.uri(),
                                            SWITCH,
                                            MANUFACTURER,
                                            HW_VERSION,
                                            SW_VERSION,
                                            SERIAL_NUMBER,
                                            new ChassisId(),
                                            annotationsBuilder.build());
    }

    @Override
    public List<PortDescription> discoverPortDetails() {

        if (!init()) {
            return null;
        }

        DeviceId deviceId = handler().data().deviceId();

        Bmv2DeviceAgent deviceAgent;
        try {
            deviceAgent = controller.getAgent(deviceId);
        } catch (Bmv2RuntimeException e) {
            log.error("Failed to connect to Bmv2 device", e);
            return null;
        }

        List<PortDescription> portDescriptions = Lists.newArrayList();

        try {
            deviceAgent.getPortsInfo().forEach(p -> {
                PortNumber portNumber = PortNumber.portNumber((long) p.number(), p.ifaceName());
                portDescriptions.add(new DefaultPortDescription(portNumber, p.isUp(), DefaultAnnotations.EMPTY));
            });
        } catch (Bmv2RuntimeException e) {
            log.error("Unable to get port descriptions of {}: {}", deviceId, e);
        }

        return ImmutableList.copyOf(portDescriptions);
    }
}
