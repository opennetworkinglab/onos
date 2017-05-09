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
package org.onosproject.net.optical.cli;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onosproject.cli.app.AllApplicationNamesCompleter;
import org.onosproject.cli.net.ConnectPointCompleter;
import org.onosproject.cli.net.ConnectivityIntentCommand;
import org.onosproject.net.CltSignalType;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.OduSignalType;
import org.onosproject.net.Port;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.OpticalCircuitIntent;
import org.onosproject.net.intent.OpticalConnectivityIntent;
import org.onosproject.net.intent.OpticalOduIntent;
import org.onosproject.net.optical.OchPort;
import org.onosproject.net.optical.OduCltPort;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static org.onosproject.net.optical.device.OpticalDeviceServiceView.opticalView;

/**
 * Installs optical connectivity or circuit intents, depending on given port types.
 */
@Command(scope = "onos", name = "add-optical-intent",
         description = "Installs optical connectivity intent")
public class AddOpticalIntentCommand extends ConnectivityIntentCommand {

    // OSGi workaround
    @SuppressWarnings("unused")
    private ConnectPointCompleter cpCompleter;

    // OSGi workaround
    @SuppressWarnings("unused")
    private AllApplicationNamesCompleter appCompleter;

    @Argument(index = 0, name = "ingress",
              description = "Ingress Device/Port Description",
              required = true, multiValued = false)
    String ingressString = "";

    @Argument(index = 1, name = "egress",
              description = "Egress Device/Port Description",
              required = true, multiValued = false)
    String egressString = "";

    @Option(name = "-b", aliases = "--bidirectional",
            description = "If this argument is passed the optical link created will be bidirectional, " +
            "else the link will be unidirectional.",
            required = false, multiValued = false)
    private boolean bidirectional = false;


    private ConnectPoint createConnectPoint(String devicePortString) {
        String[] splitted = devicePortString.split("/");

        checkArgument(splitted.length == 2,
                "Connect point must be in \"deviceUri/portNumber\" format");

        DeviceId deviceId = DeviceId.deviceId(splitted[0]);
        DeviceService deviceService = get(DeviceService.class);

        List<Port> ports = deviceService.getPorts(deviceId);

        for (Port port : ports) {
            if (splitted[1].equals(port.number().name())) {
                return new ConnectPoint(deviceId, port.number());
            }
        }

        return null;
    }

    @Override
    protected void execute() {
        IntentService service = get(IntentService.class);

        ConnectPoint ingress = createConnectPoint(ingressString);
        ConnectPoint egress = createConnectPoint(egressString);

        if (ingress == null || egress == null) {
            print("Invalid endpoint(s); could not create optical intent");
            return;
        }

        DeviceService deviceService = opticalView(get(DeviceService.class));

        Port srcPort = deviceService.getPort(ingress.deviceId(), ingress.port());
        Port dstPort = deviceService.getPort(egress.deviceId(), egress.port());

        Intent intent;

        if (srcPort instanceof OduCltPort && dstPort instanceof OduCltPort) {
            Device srcDevice = deviceService.getDevice(ingress.deviceId());
            Device dstDevice = deviceService.getDevice(egress.deviceId());

            // continue only if both OduClt port's Devices are of the same type
            if (!(srcDevice.type().equals(dstDevice.type()))) {
                print("Devices without same deviceType: SRC=%s and DST=%s", srcDevice.type(), dstDevice.type());
                return;
            }

            CltSignalType signalType = ((OduCltPort) srcPort).signalType();
            if (Device.Type.ROADM.equals(srcDevice.type()) ||
                    Device.Type.ROADM_OTN.equals(srcDevice.type())) {
                intent = OpticalCircuitIntent.builder()
                        .appId(appId())
                        .key(key())
                        .src(ingress)
                        .dst(egress)
                        .signalType(signalType)
                        .bidirectional(bidirectional)
                        .build();
            } else if (Device.Type.OTN.equals(srcDevice.type())) {
                intent = OpticalOduIntent.builder()
                        .appId(appId())
                        .key(key())
                        .src(ingress)
                        .dst(egress)
                        .signalType(signalType)
                        .bidirectional(bidirectional)
                        .build();
            } else {
                print("Wrong Device Type for connect points %s and %s", ingress, egress);
                return;
            }
        } else if (srcPort instanceof OchPort && dstPort instanceof OchPort) {
            OduSignalType signalType = ((OchPort) srcPort).signalType();
            intent = OpticalConnectivityIntent.builder()
                    .appId(appId())
                    .key(key())
                    .src(ingress)
                    .dst(egress)
                    .signalType(signalType)
                    .bidirectional(bidirectional)
                    .build();
        } else {
            print("Unable to create optical intent between connect points %s and %s", ingress, egress);
            return;
        }

        service.submit(intent);
        print("Optical intent submitted:\n%s", intent.toString());
    }
}
