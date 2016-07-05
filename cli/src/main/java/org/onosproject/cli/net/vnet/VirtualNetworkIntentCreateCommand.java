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

package org.onosproject.cli.net.vnet;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.net.ConnectivityIntentCommand;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualNetworkIntent;
import org.onosproject.incubator.net.virtual.VirtualNetworkService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;

import java.util.List;

/**
 * Installs virtual network intents.
 */
@Command(scope = "onos", name = "add-vnet-intent",
        description = "Installs virtual network connectivity intent")
public class VirtualNetworkIntentCreateCommand extends ConnectivityIntentCommand {

    @Argument(index = 0, name = "networkId", description = "Network ID",
            required = true, multiValued = false)
    Long networkId = null;

    @Argument(index = 1, name = "ingressDevice",
            description = "Ingress Device/Port Description",
            required = true, multiValued = false)
    String ingressDeviceString = null;

    @Argument(index = 2, name = "egressDevice",
            description = "Egress Device/Port Description",
            required = true, multiValued = false)
    String egressDeviceString = null;

    @Override
    protected void execute() {
        VirtualNetworkService service = get(VirtualNetworkService.class);
        IntentService virtualNetworkIntentService = service.get(NetworkId.networkId(networkId), IntentService.class);

        ConnectPoint ingress = ConnectPoint.deviceConnectPoint(ingressDeviceString);
        ConnectPoint egress = ConnectPoint.deviceConnectPoint(egressDeviceString);

        TrafficSelector selector = buildTrafficSelector();
        TrafficTreatment treatment = buildTrafficTreatment();

        List<Constraint> constraints = buildConstraints();

        Intent intent = VirtualNetworkIntent.builder()
                .networkId(NetworkId.networkId(networkId))
                .appId(appId())
                .key(key())
                .selector(selector)
                .treatment(treatment)
                .ingressPoint(ingress)
                .egressPoint(egress)
                .constraints(constraints)
                .priority(priority())
                .build();
        virtualNetworkIntentService.submit(intent);
        print("Virtual intent submitted:\n%s", intent.toString());
    }
}
