/*
 * Copyright 2016 Open Networking Laboratory
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

package org.onosproject.driver.optical.power;

import java.util.Optional;

import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.Direction;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.PowerConfig;
import org.onosproject.net.device.DeviceService;
import org.onosproject.openflow.controller.Dpid;
import org.onosproject.openflow.controller.OpenFlowController;
import org.onosproject.openflow.controller.OpenFlowSwitch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Port Power (Gain and attenuation) implementation for Oplink ROADM.
 *
 * An Oplink ROADM port exposes OchSignal resources.
 * Optical Power can be set at port level or channel/wavelength level (attenuation).
 *
 */

public class OplinkRoadmPowerConfig extends AbstractHandlerBehaviour
                                    implements PowerConfig<Direction> {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    private OpenFlowSwitch getOpenFlowDevice() {
        final OpenFlowController controller = this.handler().get(OpenFlowController.class);
        final Dpid dpid = Dpid.dpid(this.data().deviceId().uri());
        OpenFlowSwitch sw = controller.getSwitch(dpid);
        if (sw == null || !sw.isConnected()) {
            return null;
        } else {
            return sw;
        }
    }

    @Override
    public Optional<Long> getTargetPower(PortNumber portNum, Direction component) {
        // Will be implemented in the future.
        return Optional.empty();
    }

    @Override
    public Optional<Long> currentPower(PortNumber portNum, Direction component) {
        Long returnVal = null;
        // Check if switch is connected, otherwise do not return value in store,
        // which is obsolete.
        if (getOpenFlowDevice() != null) {
            DeviceService deviceService = this.handler().get(DeviceService.class);
            Port port = deviceService.getPort(this.data().deviceId(), portNum);
            if (port != null) {
                String currentPower = port.annotations().value(AnnotationKeys.CURRENT_POWER);
                if (currentPower != null) {
                    returnVal = Long.valueOf(currentPower);
                }
            }
        }
        return Optional.ofNullable(returnVal);
    }

    @Override
    public void setTargetPower(PortNumber portNum, Direction component, long power) {
        OpenFlowSwitch device = getOpenFlowDevice();
        if (device != null) {
            device.sendMsg(device.factory().buildOplinkPortPowerSet()
                    .setXid(0)
                    .setPort((int) portNum.toLong())
                    .setPowerValue((int) power)
                    .build());
        } else {
            log.warn("OpenFlow handshaker driver not found or device is not connected");
        }
    }
}
