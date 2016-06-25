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
import org.apache.karaf.shell.commands.Option;
import org.onlab.packet.MplsLabel;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.MplsIntent;

import java.util.List;
import java.util.Optional;

/**
 * Installs MPLS intents.
 *
 * @deprecated in Goldeneye Release, in favour of encapsulation
 * constraint {@link org.onosproject.net.intent.constraint.EncapsulationConstraint}
 */
@Deprecated
@Command(scope = "onos", name = "add-mpls-intent", description = "Installs mpls connectivity intent")
public class AddMplsIntent extends ConnectivityIntentCommand {

    @Argument(index = 0, name = "ingressDevice",
            description = "Ingress Device/Port Description",
            required = true,
            multiValued = false)
    private String ingressDeviceString = null;

    @Option(name = "--ingressLabel",
            description = "Ingress Mpls label",
            required = false,
            multiValued = false)
    private String ingressLabelString = "";

    @Argument(index = 1, name = "egressDevice",
            description = "Egress Device/Port Description",
            required = true,
            multiValued = false)
    private String egressDeviceString = null;

    @Option(name = "--egressLabel",
            description = "Egress Mpls label",
            required = false,
            multiValued = false)
    private String egressLabelString = "";

    @Override
    protected void execute() {
        IntentService service = get(IntentService.class);

        ConnectPoint ingress = ConnectPoint.deviceConnectPoint(ingressDeviceString);
        Optional<MplsLabel> ingressLabel = Optional.empty();
        if (!ingressLabelString.isEmpty()) {
            ingressLabel = Optional
                    .ofNullable(MplsLabel.mplsLabel(parseInt(ingressLabelString)));
        }

        ConnectPoint egress = ConnectPoint.deviceConnectPoint(egressDeviceString);
        Optional<MplsLabel> egressLabel = Optional.empty();
        if (!egressLabelString.isEmpty()) {
            egressLabel = Optional
                    .ofNullable(MplsLabel.mplsLabel(parseInt(egressLabelString)));
        }

        TrafficSelector selector = buildTrafficSelector();
        TrafficTreatment treatment = buildTrafficTreatment();

        List<Constraint> constraints = buildConstraints();

        MplsIntent intent = MplsIntent.builder()
                .appId(appId())
                .selector(selector)
                .treatment(treatment)
                .ingressPoint(ingress)
                .ingressLabel(ingressLabel)
                .egressPoint(egress)
                .egressLabel(egressLabel)
                .constraints(constraints)
                .priority(priority())
                .build();
        service.submit(intent);
    }

    protected Integer parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException nfe) {
            return null;
        }
    }
}
