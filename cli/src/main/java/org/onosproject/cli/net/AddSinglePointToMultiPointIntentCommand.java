/*
 * Copyright 2014 Open Networking Laboratory
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
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.SinglePointToMultiPointIntent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.PortNumber.portNumber;


@Command(scope = "onos", name = "add-single-to-multi-intent",
        description = "Installs connectivity intent between multiple egress devices and a single ingress device")
public class AddSinglePointToMultiPointIntentCommand extends ConnectivityIntentCommand {
    @Argument(index = 0, name = "egressDevices ingressDevice",
            description = "egressDevice/Port...egressDevice/Port ingressDevice/port",
            required = true, multiValued = true)
    String[] deviceStrings = null;

    @Override
    protected void execute() {
        IntentService service = get(IntentService.class);

        if (deviceStrings.length < 2) {
            return;
        }

        String ingressDeviceString = deviceStrings[deviceStrings.length - 1];
        DeviceId ingressDeviceId = deviceId(getDeviceId(ingressDeviceString));
        PortNumber ingressPortNumber = portNumber(getPortNumber(ingressDeviceString));
        ConnectPoint ingressPoint = new ConnectPoint(ingressDeviceId,
                                                     ingressPortNumber);

        Set<ConnectPoint> egressPoints = new HashSet<>();
        for (int index = 0; index < deviceStrings.length - 1; index++) {
            String egressDeviceString = deviceStrings[index];
            DeviceId egressDeviceId = deviceId(getDeviceId(egressDeviceString));
            PortNumber egressPortNumber = portNumber(getPortNumber(egressDeviceString));
            ConnectPoint egress = new ConnectPoint(egressDeviceId,
                                                   egressPortNumber);
            egressPoints.add(egress);
        }

        TrafficSelector selector = buildTrafficSelector();
        TrafficTreatment treatment = DefaultTrafficTreatment.builder().build();
        List<Constraint> constraints = buildConstraints();

        SinglePointToMultiPointIntent intent =
                new SinglePointToMultiPointIntent(
                        appId(),
                        key(),
                        selector,
                        treatment,
                        ingressPoint,
                        egressPoints,
                        constraints);
        service.submit(intent);
        print("Single point to multipoint intent submitted:\n%s", intent.toString());
    }

    /**
     * Extracts the port number portion of the ConnectPoint.
     *
     * @param deviceString string representing the device/port
     * @return port number as a string, empty string if the port is not found
     */
    private String getPortNumber(String deviceString) {
        int slash = deviceString.indexOf('/');
        if (slash <= 0) {
            return "";
        }
        return deviceString.substring(slash + 1, deviceString.length());
    }

    /**
     * Extracts the device ID portion of the ConnectPoint.
     *
     * @param deviceString string representing the device/port
     * @return device ID string
     */
    private String getDeviceId(String deviceString) {
        int slash = deviceString.indexOf('/');
        if (slash <= 0) {
            return "";
        }
        return deviceString.substring(0, slash);
    }

}
