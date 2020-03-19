/*
 * Copyright 2020-present Open Networking Foundation
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

package org.onosproject.drivers.odtn.openconfig;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import gnmi.Gnmi;
import org.onosproject.gnmi.api.GnmiClient;
import org.onosproject.gnmi.api.GnmiController;
import org.onosproject.gnmi.api.GnmiUtils.GnmiPathBuilder;
import org.onosproject.grpc.utils.AbstractGrpcHandlerBehaviour;
import org.onosproject.net.ModulationScheme;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.ModulationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Modulation Config behavior for gNMI and OpenConfig model based device.
 */
public class GnmiTerminalDeviceModulationConfig<T>
        extends AbstractGrpcHandlerBehaviour<GnmiClient, GnmiController>
        implements ModulationConfig<T> {

    public static Logger log = LoggerFactory.getLogger(GnmiTerminalDeviceModulationConfig.class);

    private static final BiMap<Long, ModulationScheme> OPERATIONAL_MODE_TO_MODULATION =
            ImmutableBiMap.<Long, ModulationScheme>builder()
                    .put(1L, ModulationScheme.DP_QPSK)
                    .put(2L, ModulationScheme.DP_16QAM)
                    .put(3L, ModulationScheme.DP_8QAM)
                    .build();

    public GnmiTerminalDeviceModulationConfig() {
        super(GnmiController.class);
    }

    @Override
    public Optional<ModulationScheme> getModulationScheme(PortNumber portNumber, T component) {
        if (!setupBehaviour("getModulationScheme")) {
            return Optional.empty();
        }
        // Get value from path
        //  /components/component[name=]/optical-channel/state/operational-mode
        // And convert operational mode (uint64 bit mask) to ModulationScheme enum

        // First we need to find component name (from port annotation)
        String ocName = getOcName(portNumber);

        // Query operational mode from device
        Gnmi.Path path = GnmiPathBuilder.newBuilder()
                .addElem("components")
                .addElem("component").withKeyValue("name", ocName)
                .addElem("optical-channel")
                .addElem("state")
                .addElem("operational-mode")
                .build();

        Gnmi.GetRequest req = Gnmi.GetRequest.newBuilder()
                .addPath(path)
                .setEncoding(Gnmi.Encoding.PROTO)
                .build();

        Gnmi.GetResponse resp;
        try {
            resp = client.get(req).get();
        } catch (ExecutionException | InterruptedException e) {
            log.warn("Unable to get operational mode from device {}, port {}: {}",
                     deviceId, portNumber, e.getMessage());
            return Optional.empty();
        }

        // Get operational mode value from gNMI get response
        // Here we assume we get only one response
        if (resp.getNotificationCount() == 0 || resp.getNotification(0).getUpdateCount() == 0) {
            log.warn("No update message found");
            return Optional.empty();
        }

        Gnmi.Update update = resp.getNotification(0).getUpdate(0);
        Gnmi.TypedValue operationalModeVal = update.getVal();

        if (operationalModeVal == null) {
            log.warn("No operational mode found");
            return Optional.empty();
        }

        return Optional.ofNullable(
                OPERATIONAL_MODE_TO_MODULATION.getOrDefault(operationalModeVal.getUintVal(), null));
    }

    @Override
    public void setModulationScheme(PortNumber portNumber, T component, long bitRate) {
        if (!setupBehaviour("getModulationScheme")) {
            return;
        }
        // Sets value to path
        //  /components/component[name]/optical-channel/config/operational-mode

        // First we convert bit rate to modulation scheme to operational mode
        ModulationScheme modulationScheme = ModulationScheme.DP_16QAM;
        // Use DP_QPSK if bit rate is less or equals to 100 Gbps
        if (bitRate <= 100) {
            modulationScheme = ModulationScheme.DP_QPSK;
        }
        long operationalMode = OPERATIONAL_MODE_TO_MODULATION.inverse().get(modulationScheme);

        // Build gNMI set request
        String ocName = getOcName(portNumber);
        Gnmi.Path path = GnmiPathBuilder.newBuilder()
                .addElem("components")
                .addElem("component").withKeyValue("name", ocName)
                .addElem("optical-channel")
                .addElem("config")
                .addElem("operational-mode")
                .build();

        Gnmi.TypedValue val = Gnmi.TypedValue.newBuilder()
                .setUintVal(operationalMode)
                .build();

        Gnmi.Update update = Gnmi.Update.newBuilder()
                .setPath(path)
                .setVal(val)
                .build();

        Gnmi.SetRequest req = Gnmi.SetRequest.newBuilder()
                .addUpdate(update)
                .build();

        try {
            client.set(req).get();
        } catch (ExecutionException | InterruptedException e) {
            log.warn("Unable to set operational mode to device {}, port {}, mode: {}: {}",
                    deviceId, portNumber, operationalMode, e.getMessage());
        }
    }

    private String getOcName(PortNumber portNumber) {
        return deviceService.getPort(deviceId, portNumber).annotations().value("oc-name");
    }
}
