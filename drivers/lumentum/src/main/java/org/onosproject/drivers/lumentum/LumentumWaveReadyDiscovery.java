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
package org.onosproject.drivers.lumentum;

import org.onlab.packet.ChassisId;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.CltSignalType;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.GridType;
import org.onosproject.net.OchSignal;
import org.onosproject.net.OduSignalType;
import org.onosproject.net.PortNumber;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.tl1.Tl1Command;
import org.onosproject.tl1.Tl1Controller;
import org.onosproject.tl1.Tl1Device;
import org.onosproject.tl1.impl.DefaultTl1Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.optical.device.OchPortHelper.ochPortDescription;
import static org.onosproject.net.optical.device.OduCltPortHelper.oduCltPortDescription;

/**
 * Device description behaviour for Lumentum WaveReady devices.
 *
 * Tested with Lumentum WaveReady 3100 transponder.
 */
public class LumentumWaveReadyDiscovery extends AbstractHandlerBehaviour implements DeviceDescriptionDiscovery {
    private final Logger log = LoggerFactory.getLogger(LumentumWaveReadyDiscovery.class);

    // Time to wait for device response in milliseconds
    private static final int TIMEOUT = 10000;

    private static final String LUMENTUM = "Lumentum";
    private static final String WAVEREADY = "WaveReady";
    private static final String SWVERSION = "1.0";
    private static final String SERIAL = "3100";

    // Some TL1 string constants
    private static final String ACT = "ACT";
    private static final String USER = "USER";
    private static final String RTRV = "RTRV";
    private static final String NETYPE = "NETYPE";
    private static final String PLUGGABLE_INV = "PLUGGABLE-INV";
    private static final String CANC = "CANC";
    private static final String EIGHTFIFTY = "850";


    @Override
    public DeviceDescription discoverDeviceDetails() {
        DeviceId deviceId = handler().data().deviceId();
        Tl1Controller ctrl = checkNotNull(handler().get(Tl1Controller.class));
        // Something reasonable, unavailable by default
        DeviceDescription defaultDescription = new DefaultDeviceDescription(deviceId.uri(), Device.Type.OTN,
                LUMENTUM, WAVEREADY, SWVERSION, SERIAL,
                new ChassisId(), false, DefaultAnnotations.EMPTY);

        Optional<Tl1Device> device = ctrl.getDevice(deviceId);
        if (!device.isPresent()) {
            return defaultDescription;
        }

        // Login
        Tl1Command loginCmd = DefaultTl1Command.builder()
                .withVerb(ACT)
                .withModifier(USER)
                .withAid(device.get().username())
                .withCtag(100)
                .withParameters(device.get().password())
                .build();
        Future<String> login = ctrl.sendMsg(deviceId, loginCmd);

        try {
            String loginResponse = login.get(TIMEOUT, TimeUnit.MILLISECONDS);
            if (loginResponse.contains("Access denied")) {
                log.error("Access denied: {}", loginResponse);
                return defaultDescription;
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error("Login failed", e);
            return defaultDescription;
        }

        // Fetch device description
        Tl1Command ddCmd = DefaultTl1Command.builder()
                .withVerb(RTRV)
                .withModifier(NETYPE)
                .withCtag(101)
                .build();
        Future<String> dd = ctrl.sendMsg(deviceId, ddCmd);

        try {
            String ddResponse = dd.get(TIMEOUT, TimeUnit.MILLISECONDS);

            return new DefaultDeviceDescription(defaultDescription, true, extractAnnotations(ddResponse));
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error("Device description not found", e);
            return defaultDescription;
        }
    }

    @Override
    public List<PortDescription> discoverPortDetails() {
        DeviceId deviceId = handler().data().deviceId();
        Tl1Controller ctrl = checkNotNull(handler().get(Tl1Controller.class));

        // Assume we're successfully logged in
        // Fetch port descriptions
        Tl1Command pdCmd = DefaultTl1Command.builder()
                .withVerb(RTRV)
                .withModifier(PLUGGABLE_INV)
                .withCtag(102)
                .build();
        Future<String> pd = ctrl.sendMsg(deviceId, pdCmd);

        try {
            String pdResponse = pd.get(TIMEOUT, TimeUnit.MILLISECONDS);

            return extractPorts(pdResponse);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error("Port description not found", e);
            return Collections.EMPTY_LIST;
        }
    }

    private SparseAnnotations extractAnnotations(String s) {
        DefaultAnnotations.Builder annot = DefaultAnnotations.builder();

        Arrays.stream(s.split(",")).forEach(w -> {
            String[] pair = w.replaceAll("\\\\\"", "").split("=");
            if (pair.length == 2) {
                annot.set(pair[0], pair[1]);
            } else {
                annot.set(pair[0], "");
            }
        });

        return annot.build();
    }

    // Extract ports from response on pluggable inventory retrieval.
    // Client ports are identified by 850nm, everything else is a network port.
    private List<PortDescription> extractPorts(String s) {
        List<PortDescription> ports = new ArrayList<>();

        if (s.length() == 0) {
            return ports;
        }

        Arrays.stream(s.split("\"\"")).forEach(p -> {
            if (p.contains(EIGHTFIFTY)) {
                PortDescription cltPort = oduCltPortDescription(
                        PortNumber.portNumber(ports.size() + 1),
                        true,
                        CltSignalType.CLT_10GBE,
                        extractAnnotations(p));
                ports.add(cltPort);
            } else {
                PortDescription netPort = ochPortDescription(
                        PortNumber.portNumber(ports.size() + 1),
                        true,
                        OduSignalType.ODU2e,
                        true,
                        new OchSignal(GridType.DWDM, ChannelSpacing.CHL_50GHZ, 0, 4),
                        extractAnnotations(p));
                ports.add(netPort);
            }
        });

        return ports;
    }

    // Unused but provided here for convenience.
    private void logout() {
        DeviceId deviceId = handler().data().deviceId();
        Tl1Controller ctrl = checkNotNull(handler().get(Tl1Controller.class));

        Optional<Tl1Device> device = ctrl.getDevice(deviceId);
        if (!device.isPresent()) {
            return;
        }

        // Logout command
        Tl1Command logoutCmd = DefaultTl1Command.builder()
                .withVerb(CANC)
                .withModifier(USER)
                .withAid(device.get().username())
                .withCtag(103)
                .build();
        Future<String> logout = ctrl.sendMsg(deviceId, logoutCmd);

        try {
            String logoutResponse = logout.get(TIMEOUT, TimeUnit.MILLISECONDS);

        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error("Lougout failed", e);
        }
    }
}
