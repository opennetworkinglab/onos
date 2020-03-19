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
 *
 * This work was partially supported by EC H2020 project METRO-HAUL (761727).
 */

package org.onosproject.drivers.odtn.openconfig;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import gnmi.Gnmi;
import org.onosproject.gnmi.api.GnmiClient;
import org.onosproject.gnmi.api.GnmiController;
import org.onosproject.gnmi.api.GnmiUtils.GnmiPathBuilder;
import org.onosproject.grpc.utils.AbstractGrpcHandlerBehaviour;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.PowerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * PowerConfig behaviour for gNMI and OpenConfig model based device.
 */
public class GnmiTerminalDevicePowerConfig<T>
        extends AbstractGrpcHandlerBehaviour<GnmiClient, GnmiController>
        implements PowerConfig<T> {

    private static final Logger log = LoggerFactory.getLogger(GnmiTerminalDevicePowerConfig.class);
    private static final int DEFAULT_OC_POWER_PRECISION = 2;
    private static final Collection<Port.Type> OPTICAL_TYPES = ImmutableSet.of(Port.Type.FIBER,
                    Port.Type.PACKET,
                    Port.Type.ODUCLT,
                    Port.Type.OCH,
                    Port.Type.OMS,
                    Port.Type.OTU);

    public GnmiTerminalDevicePowerConfig() {
        super(GnmiController.class);
    }

    @Override
    public Optional<Double> getTargetPower(PortNumber port, T component) {
        if (!setupBehaviour("getTargetPower")) {
            return Optional.empty();
        }
        if (!isOpticalPort(port)) {
            return Optional.empty();
        }
        // path: /components/component[name=<name>]/optical-channel/config/target-output-power
        return getValueFromPath(getOcName(port), "config/target-output-power");
    }

    @Override
    public void setTargetPower(PortNumber port, T component, double power) {
        if (!setupBehaviour("setTargetPower")) {
            return;
        }
        if (!isOpticalPort(port)) {
            return;
        }
        setValueToPath(getOcName(port), "config/target-output-power", power);
    }

    @Override
    public Optional<Double> currentPower(PortNumber port, T component) {
        if (!setupBehaviour("currentPower")) {
            return Optional.empty();
        }
        if (!isOpticalPort(port)) {
            return Optional.empty();
        }
        // path: /components/component[name=<name>]/optical-channel/state/output-power/instant
        return getValueFromPath(getOcName(port), "state/output-power/instant");
    }

    @Override
    public Optional<Double> currentInputPower(PortNumber port, T component) {
        if (!setupBehaviour("currentInputPower")) {
            return Optional.empty();
        }
        if (!isOpticalPort(port)) {
            return Optional.empty();
        }
        // path: /components/component[name=<name>]/optical-channel/state/input-power/instant
        return getValueFromPath(getOcName(port), "state/input-power/instant");
    }

    @Override
    public Optional<Range<Double>> getTargetPowerRange(PortNumber port, Object component) {
        if (!isOpticalPort(port)) {
            return Optional.empty();
        }

        // From CassiniTerminalDevicePowerConfig
        double targetMin = -30;
        double targetMax = 1;
        return Optional.of(Range.open(targetMin, targetMax));
    }

    @Override
    public Optional<Range<Double>> getInputPowerRange(PortNumber port, Object component) {
        if (!isOpticalPort(port)) {
            return Optional.empty();
        }

        // From CassiniTerminalDevicePowerConfig
        double targetMin = -30;
        double targetMax = 1;
        return Optional.of(Range.open(targetMin, targetMax));
    }

    private String getOcName(PortNumber portNumber) {
        if (!setupBehaviour("getOcName")) {
            return null;
        }
        return deviceService.getPort(deviceId, portNumber).annotations().value("oc-name");
    }

    private boolean isOpticalPort(PortNumber portNumber) {
        if (!setupBehaviour("isOpticalPort")) {
            return false;
        }
        return OPTICAL_TYPES.contains(deviceService.getPort(deviceId, portNumber).type());
    }

    private Optional<Double> getValueFromPath(String ocName, String subPath) {
        Gnmi.GetRequest req = Gnmi.GetRequest.newBuilder()
                .addPath(buildPathWithSubPath(ocName, subPath))
                .setEncoding(Gnmi.Encoding.PROTO)
                .build();
        try {
            Gnmi.GetResponse resp = client.get(req).get();
            // Here we assume we have only one response
            if (resp.getNotificationCount() == 0 || resp.getNotification(0).getUpdateCount() == 0) {
                log.warn("Empty response for sub-path {}, component {}", subPath, ocName);
                return Optional.empty();
            }
            Gnmi.Update update = resp.getNotification(0).getUpdate(0);
            Gnmi.Decimal64 value = update.getVal().getDecimalVal();
            return Optional.of(decimal64ToDouble(value));
        } catch (ExecutionException | InterruptedException e) {
            log.warn("Unable to get value from optical sub-path {} for component {}: {}",
                     subPath, ocName, e.getMessage());
            return Optional.empty();
        }
    }

    private void setValueToPath(String ocName, String subPath, Double value) {
        Gnmi.TypedValue val = Gnmi.TypedValue.newBuilder()
                .setDecimalVal(doubleToDecimal64(value, DEFAULT_OC_POWER_PRECISION))
                .build();
        Gnmi.Update update = Gnmi.Update.newBuilder()
                .setPath(buildPathWithSubPath(ocName, subPath))
                .setVal(val)
                .build();
        Gnmi.SetRequest req = Gnmi.SetRequest.newBuilder()
                .addUpdate(update)
                .build();
        try {
            client.set(req).get();
        } catch (ExecutionException | InterruptedException e) {
            log.warn("Unable to set optical sub-path {}, component {}, value {}: {}",
                     subPath, ocName, value, e.getMessage());
        }
    }

    private Gnmi.Path buildPathWithSubPath(String ocName, String subPath) {
        String[] elems = subPath.split("/");
        GnmiPathBuilder pathBuilder = GnmiPathBuilder.newBuilder()
                .addElem("components")
                .addElem("component").withKeyValue("name", ocName)
                .addElem("optical-channel");
        for (String elem : elems) {
            pathBuilder.addElem(elem);
        }
        return pathBuilder.build();
    }

    private Double decimal64ToDouble(Gnmi.Decimal64 value) {
        double result = value.getDigits();
        if (value.getPrecision() != 0) {
            result = result / Math.pow(10, value.getPrecision());
        }
        return result;
    }

    private Gnmi.Decimal64 doubleToDecimal64(Double value, int precision) {
        return Gnmi.Decimal64.newBuilder()
                .setDigits((long) (value * Math.pow(10, precision)))
                .setPrecision(precision)
                .build();
    }
}