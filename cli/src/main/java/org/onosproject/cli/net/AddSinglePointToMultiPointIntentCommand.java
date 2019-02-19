/*
 * Copyright 2014-present Open Networking Foundation
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

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.FilteredConnectPoint;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.SinglePointToMultiPointIntent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Installs connectivity intent between a single ingress device and multiple egress devices.
 */
@Service
@Command(scope = "onos", name = "add-single-to-multi-intent",
        description = "Installs connectivity intent between a single ingress device and multiple egress devices")
public class AddSinglePointToMultiPointIntentCommand extends ConnectivityIntentCommand {
    @Argument(index = 0, name = "ingressDevice egressDevices",
            description = "ingressDevice/Port egressDevice/Port...egressDevice/Port",
            required = true, multiValued = true)
    @Completion(ConnectPointCompleter.class)
    String[] deviceStrings = null;

    @Override
    protected void doExecute() {
        IntentService service = get(IntentService.class);

        if (deviceStrings.length < 2) {
            return;
        }

        String ingressDeviceString = deviceStrings[0];
        ConnectPoint ingressPoint = ConnectPoint.deviceConnectPoint(ingressDeviceString);

        Set<FilteredConnectPoint> egressPoints = new HashSet<>();
        for (int index = 1; index < deviceStrings.length; index++) {
            String egressDeviceString = deviceStrings[index];
            ConnectPoint egress = ConnectPoint.deviceConnectPoint(egressDeviceString);
            egressPoints.add(new FilteredConnectPoint(egress));
        }

        TrafficSelector selector = buildTrafficSelector();
        TrafficTreatment treatment = buildTrafficTreatment();
        List<Constraint> constraints = buildConstraints();

        SinglePointToMultiPointIntent intent =
                SinglePointToMultiPointIntent.builder()
                        .appId(appId())
                        .key(key())
                        .selector(selector)
                        .treatment(treatment)
                        .filteredIngressPoint(new FilteredConnectPoint(ingressPoint))
                        .filteredEgressPoints(egressPoints)
                        .constraints(constraints)
                        .priority(priority())
                        .resourceGroup(resourceGroup())
                        .build();
        service.submit(intent);
        print("Single point to multipoint intent submitted:\n%s", intent.toString());
    }

}
