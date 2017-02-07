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

package org.onosproject.driver.optical.power;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.Optional;

import com.google.common.collect.Range;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.PowerConfig;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.optical.OpticalAnnotations;
import org.onosproject.openflow.controller.Dpid;
import org.onosproject.openflow.controller.OpenFlowController;
import org.onosproject.openflow.controller.OpenFlowSwitch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Port Power (Gain and attenuation) implementation for Oplink EDFA device.
 *
 * An Oplink EDFA port exposes Direction resources.
 * Set gain(attenuation) at AGC mode or set output level at APC mode.
 *
 */

public class OplinkEdfaPowerConfig extends AbstractHandlerBehaviour
                                    implements PowerConfig<Object> {

    /**
     * Input and ouput port number
     * Note:
     * These port number configurations are just in use for a short time.
     * In the future, the port number and direction type would be obtained from physical device.
     */
    private static final int LINE_IN_WEST = 1;
    private static final int LINE_OUT_WEST = 2;
    private static final int LINE_IN_EAST = 3;
    private static final int LINE_OUT_EAST = 4;

    /**
     * Power threshold of each port, magnified 100 times
     * Note:
     * These threshold configurations are just in use for a short time.
     * In the future, the power threshold would be obtained from physical device.
     */
    private static final long POWER_IN_WEST_LOW_THRES = -1900L;
    private static final long POWER_IN_WEST_HIGH_THRES = 0L;
    private static final long POWER_IN_EAST_LOW_THRES = -3100L;
    private static final long POWER_IN_EAST_HIGH_THRES = 700L;
    private static final long POWER_OUT_LOW_THRES = 0L;
    private static final long POWER_OUT_HIGH_THRES = 1900L;

    // Transaction id to use.
    private final AtomicInteger xidCounter = new AtomicInteger(0);
    // Log
    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public Optional<Long> getTargetPower(PortNumber portNum, Object component) {
        Long returnVal = getTargetPortPower(portNum);
        return Optional.ofNullable(returnVal);
    }

    @Override
    public Optional<Long> currentPower(PortNumber portNum, Object component) {
        Long returnVal = getCurrentPortPower(portNum);
        return Optional.ofNullable(returnVal);
    }

    @Override
    public void setTargetPower(PortNumber portNum, Object component, long power) {
        setTargetPortPower(portNum, power);
    }

    @Override
    public Optional<Range<Long>> getTargetPowerRange(PortNumber port, Object component) {
        Range<Long> range = getTargetPortPowerRange(port);
        return Optional.ofNullable(range);
    }

    @Override
    public Optional<Range<Long>> getInputPowerRange(PortNumber port, Object component) {
        Range<Long> range = getInputPortPowerRange(port);
        return Optional.ofNullable(range);
    }

    private OpenFlowSwitch getOpenFlowDevice() {
        final OpenFlowController controller = handler().get(OpenFlowController.class);
        final Dpid dpid = Dpid.dpid(data().deviceId().uri());
        OpenFlowSwitch sw = controller.getSwitch(dpid);
        if (sw == null || !sw.isConnected()) {
            log.warn("OpenFlow handshaker driver not found or device is not connected");
            return null;
        }
        return sw;
    }

    private Long getPowerFromPort(PortNumber portNum, String annotation) {
        // Check if switch is connected, otherwise do not return value in store, which is obsolete.
        if (getOpenFlowDevice() == null) {
            // Warning already exists in method getOpenFlowDevice()
            return null;
        }
        DeviceService deviceService = handler().get(DeviceService.class);
        Port port = deviceService.getPort(data().deviceId(), portNum);
        if (port == null) {
            log.warn("Unexpected port: {}", portNum);
            return null;
        }
        String power = port.annotations().value(annotation);
        if (power == null) {
            log.warn("Cannot get {} from port {}.", annotation, portNum);
            return null;
        }
        return Long.valueOf(power);
    }

    private Long getTargetPortPower(PortNumber portNum) {
        return getPowerFromPort(portNum, OpticalAnnotations.TARGET_POWER);
    }

    private Long getCurrentPortPower(PortNumber portNum) {
        return getPowerFromPort(portNum, OpticalAnnotations.CURRENT_POWER);
    }

    private void setTargetPortPower(PortNumber portNum, long power) {
        OpenFlowSwitch device = getOpenFlowDevice();
        // Check if switch is connected, otherwise do not return value in store, which is obsolete.
        if (device == null) {
            // Warning already exists in method getOpenFlowDevice()
            return;
        }
        device.sendMsg(device.factory().buildOplinkPortPowerSet()
                .setXid(xidCounter.getAndIncrement())
                .setPort((int) portNum.toLong())
                .setPowerValue((int) power)
                .build());
    }

    // Returns the acceptable target range for an output Port, null otherwise
    private Range<Long> getTargetPortPowerRange(PortNumber port) {
        long portNum = port.toLong();
        // FIXME
        // Short time hard code, we will use port direction type instead in the future.
        // And more, the power range will be also obtained from device configuration.
        switch ((int) portNum) {
            case LINE_OUT_EAST:
            case LINE_OUT_WEST:
                return Range.closed(POWER_OUT_LOW_THRES, POWER_OUT_HIGH_THRES);
            default:
                // Unexpected port. Do not need warning here for port polling.
                return null;
        }
    }

    // Returns the working input power range for an input port, null if the port
    // is not an input port.
    private Range<Long> getInputPortPowerRange(PortNumber port) {
        long portNum = port.toLong();
        // FIXME
        // Short time hard code, we will use port direction type instead in the future.
        // And more, the power range will be also obtained from device configuration.
        switch ((int) portNum) {
            case LINE_IN_EAST:
                return Range.closed(POWER_IN_EAST_LOW_THRES, POWER_IN_EAST_HIGH_THRES);
            case LINE_IN_WEST:
                return Range.closed(POWER_IN_WEST_LOW_THRES, POWER_IN_WEST_HIGH_THRES);
            default:
                // Unexpected port. Do not need warning here for port polling.
                return null;
        }
    }
}
