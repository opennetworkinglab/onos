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

package org.onosproject.net.optical.util;

import org.onosproject.net.optical.OduCltPort;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.CltSignalType;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.OchSignal;
import org.onosproject.net.OduSignalType;
import org.onosproject.net.Port;
import org.onosproject.net.Path;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.OpticalCircuitIntent;
import org.onosproject.net.intent.OpticalConnectivityIntent;
import org.onosproject.net.intent.OpticalOduIntent;
import org.onosproject.net.optical.OchPort;

import static org.onosproject.net.Device.Type;
import static org.onosproject.net.optical.device.OpticalDeviceServiceView.opticalView;

import org.slf4j.Logger;

import java.util.Optional;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Utility class for optical intents.
 */
public final class OpticalIntentUtility {

    private static final Logger log = getLogger(OpticalIntentUtility.class);

    private OpticalIntentUtility() {
    }

    /**
     * Returns a new optical intent created from the method parameters.
     *
     * @param ingress ingress description (device/port)
     * @param egress egress description (device/port)
     * @param deviceService device service
     * @param key intent key
     * @param appId application id
     * @param bidirectional if this argument is true, the optical link created
     * will be bidirectional, otherwise the link will be unidirectional.
     * @param signal optical signal
     * @param suggestedPath suggested path
     *
     * @return created intent
     */
    public static Intent createOpticalIntent(ConnectPoint ingress, ConnectPoint
            egress, DeviceService deviceService, Key key, ApplicationId appId, boolean
            bidirectional, OchSignal signal, Path suggestedPath) {

        Intent intent = null;

        if (ingress == null || egress == null) {
            log.debug("Invalid endpoint(s); could not create optical intent");
            return intent;
        }

        DeviceService ds = opticalView(deviceService);

        Port srcPort = ds.getPort(ingress.deviceId(), ingress.port());
        Port dstPort = ds.getPort(egress.deviceId(), egress.port());

        if (srcPort instanceof OduCltPort && dstPort instanceof OduCltPort) {
            Device srcDevice = ds.getDevice(ingress.deviceId());
            Device dstDevice = ds.getDevice(egress.deviceId());

            // continue only if both OduClt port's Devices are of the same type
            if (!(srcDevice.type().equals(dstDevice.type()))) {
                log.debug("Devices without same deviceType: SRC {} and DST={}", srcDevice.type(), dstDevice.type());
                return intent;
            }

            CltSignalType signalType = ((OduCltPort) srcPort).signalType();
            if (Type.ROADM.equals(srcDevice.type()) ||
                    Type.ROADM_OTN.equals(srcDevice.type()) ||
                    Type.OLS.equals(srcDevice.type()) ||
                    Type.TERMINAL_DEVICE.equals(srcDevice.type())) {
                intent = OpticalCircuitIntent.builder()
                        .appId(appId)
                        .key(key)
                        .src(ingress)
                        .dst(egress)
                        .signalType(signalType)
                        .bidirectional(bidirectional)
                        .ochSignal(Optional.ofNullable(signal))
                        .suggestedPath(Optional.ofNullable(suggestedPath))
                        .build();
            } else if (Type.OTN.equals(srcDevice.type())) {
                intent = OpticalOduIntent.builder()
                        .appId(appId)
                        .key(key)
                        .src(ingress)
                        .dst(egress)
                        .signalType(signalType)
                        .bidirectional(bidirectional)
                        .build();
            } else {
                log.debug("Wrong Device Type for connect points {} and {}", ingress, egress);
            }
        } else if (srcPort instanceof OchPort && dstPort instanceof OchPort) {
            OduSignalType signalType = ((OchPort) srcPort).signalType();
            intent = OpticalConnectivityIntent.builder()
                    .appId(appId)
                    .key(key)
                    .src(ingress)
                    .dst(egress)
                    .signalType(signalType)
                    .bidirectional(bidirectional)
                    .ochSignal(Optional.ofNullable(signal))
                    .suggestedPath(Optional.ofNullable(suggestedPath))
                    .build();
        } else {
            log.debug("Unable to create optical intent between connect points {} and {}", ingress, egress);
        }

        return intent;
    }

    /**
     * Returns a new optical intent created from the method parameters, strict suggestedPath is specified.
     *
     * @param ingress ingress description (device/port)
     * @param egress egress description (device/port)
     * @param deviceService device service
     * @param key intent key
     * @param appId application id
     * @param bidirectional if this argument is true, the optical link created
     * will be bidirectional, otherwise the link will be unidirectional.
     * @param signal optical signal
     * @param suggestedPath suggested path for the intent
     *
     * @return created intent
     */
    public static Intent createExplicitOpticalIntent(ConnectPoint ingress, ConnectPoint
            egress, DeviceService deviceService, Key key, ApplicationId appId, boolean
                                                     bidirectional, OchSignal signal, Path suggestedPath) {

        Intent intent = null;

        if (ingress == null || egress == null) {
            log.error("Invalid endpoint(s); could not create optical intent");
            return intent;
        }

        DeviceService ds = opticalView(deviceService);

        Port srcPort = ds.getPort(ingress.deviceId(), ingress.port());
        Port dstPort = ds.getPort(egress.deviceId(), egress.port());

        if (srcPort instanceof OduCltPort && dstPort instanceof OduCltPort) {
            Device srcDevice = ds.getDevice(ingress.deviceId());
            Device dstDevice = ds.getDevice(egress.deviceId());

            // continue only if both OduClt port's Devices are of the same type
            if (!(srcDevice.type().equals(dstDevice.type()))) {
                log.debug("Devices without same deviceType: SRC={} and DST={}", srcDevice.type(), dstDevice.type());
                return intent;
            }

            CltSignalType signalType = ((OduCltPort) srcPort).signalType();
            if (Type.ROADM.equals(srcDevice.type()) ||
                    Type.ROADM_OTN.equals(srcDevice.type()) ||
                    Type.OLS.equals(srcDevice.type()) ||
                    Type.TERMINAL_DEVICE.equals(srcDevice.type())) {
                intent = OpticalCircuitIntent.builder()
                        .appId(appId)
                        .key(key)
                        .src(ingress)
                        .dst(egress)
                        .signalType(signalType)
                        .bidirectional(bidirectional)
                        .ochSignal(Optional.ofNullable(signal))
                        .suggestedPath(Optional.ofNullable(suggestedPath))
                        .build();
            } else if (Type.OTN.equals(srcDevice.type())) {
                intent = OpticalOduIntent.builder()
                        .appId(appId)
                        .key(key)
                        .src(ingress)
                        .dst(egress)
                        .signalType(signalType)
                        .bidirectional(bidirectional)
                        .build();
            } else {
                log.error("Wrong Device Type for connect points: " +
                        "ingress {} of type {}; egress {} of type {}",
                        ingress, srcDevice.type(), egress, dstDevice.type());
            }
        } else if (srcPort instanceof OchPort && dstPort instanceof OchPort) {
            OduSignalType signalType = ((OchPort) srcPort).signalType();
            intent = OpticalConnectivityIntent.builder()
                    .appId(appId)
                    .key(key)
                    .src(ingress)
                    .dst(egress)
                    .signalType(signalType)
                    .bidirectional(bidirectional)
                    .ochSignal(Optional.ofNullable(signal))
                    .suggestedPath(Optional.ofNullable(suggestedPath))
                    .build();
        } else {
            log.error("Unable to create explicit optical intent between connect points {} and {}", ingress, egress);
        }

        return intent;
    }
}
