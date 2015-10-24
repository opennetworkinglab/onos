/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.cli.net;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.OchPort;
import org.onosproject.net.OduCltPort;
import org.onosproject.net.DeviceId;
import org.onosproject.net.OduSignalType;
import org.onosproject.net.Port;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.OpticalCircuitIntent;
import org.onosproject.net.intent.OpticalConnectivityIntent;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Installs optical connectivity or circuit intents, depending on given port types.
 */
@Command(scope = "onos", name = "add-optical-intent",
         description = "Installs optical connectivity intent")
public class AddOpticalIntentCommand extends ConnectivityIntentCommand {

    @Argument(index = 0, name = "ingressDevice",
              description = "Ingress Device/Port Description",
              required = true, multiValued = false)
    String ingressDeviceString = "";

    @Argument(index = 1, name = "egressDevice",
              description = "Egress Device/Port Description",
              required = true, multiValued = false)
    String egressDeviceString = "";

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

        ConnectPoint ingress = createConnectPoint(ingressDeviceString);
        ConnectPoint egress = createConnectPoint(egressDeviceString);

        if (ingress == null || egress == null) {
            print("Invalid endpoint(s); could not create optical intent");
            return;
        }

        DeviceService deviceService = get(DeviceService.class);
        Port srcPort = deviceService.getPort(ingress.deviceId(), ingress.port());
        Port dstPort = deviceService.getPort(egress.deviceId(), egress.port());

        Intent intent;
        // FIXME: Hardcoded signal types
        if (srcPort instanceof OduCltPort && dstPort instanceof OduCltPort) {
            intent = OpticalCircuitIntent.builder()
                    .appId(appId())
                    .key(key())
                    .src(ingress)
                    .dst(egress)
                    .signalType(OduCltPort.SignalType.CLT_10GBE)
                    .bidirectional(bidirectional)
                    .build();
        } else if (srcPort instanceof OchPort && dstPort instanceof OchPort) {
            intent = OpticalConnectivityIntent.builder()
                    .appId(appId())
                    .key(key())
                    .src(ingress)
                    .dst(egress)
                    .signalType(OduSignalType.ODU4)
                    .bidirectional(bidirectional)
                    .build();
        } else {
            print("Unable to create optical intent between connect points {} and {}", ingress, egress);
            return;
        }

        service.submit(intent);
        print("Optical intent submitted:\n%s", intent.toString());
    }
}
