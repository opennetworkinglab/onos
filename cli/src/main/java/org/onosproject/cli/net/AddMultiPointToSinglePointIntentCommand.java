/*
 * Copyright 2014-present Open Networking Laboratory
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
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.MultiPointToSinglePointIntent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Installs connectivity intent between multiple ingress devices and a single egress device.
 */
@Command(scope = "onos", name = "add-multi-to-single-intent",
         description = "Installs connectivity intent between multiple ingress devices and a single egress device")
public class AddMultiPointToSinglePointIntentCommand extends ConnectivityIntentCommand {

    @Argument(index = 0, name = "ingressDevices egressDevice",
              description = "ingressDevice/Port..ingressDevice/Port egressDevice/Port",
              required = true, multiValued = true)
    String[] deviceStrings = null;

    @Override
    protected void execute() {
        IntentService service = get(IntentService.class);

        if (deviceStrings.length < 2) {
            return;
        }

        String egressDeviceString = deviceStrings[deviceStrings.length - 1];
        ConnectPoint egress = ConnectPoint.deviceConnectPoint(egressDeviceString);

        Set<ConnectPoint> ingressPoints = new HashSet<>();
        for (int index = 0; index < deviceStrings.length - 1; index++) {
            String ingressDeviceString = deviceStrings[index];
            ConnectPoint ingress = ConnectPoint.deviceConnectPoint(ingressDeviceString);
            ingressPoints.add(ingress);
        }

        TrafficSelector selector = buildTrafficSelector();
        TrafficTreatment treatment = buildTrafficTreatment();
        List<Constraint> constraints = buildConstraints();

        Intent intent = MultiPointToSinglePointIntent.builder()
                .appId(appId())
                .key(key())
                .selector(selector)
                .treatment(treatment)
                .ingressPoints(ingressPoints)
                .egressPoint(egress)
                .constraints(constraints)
                .priority(priority())
                .resourceGroup(resourceGroup())
                .build();
        service.submit(intent);
        print("Multipoint to single point intent submitted:\n%s", intent.toString());
    }
}
