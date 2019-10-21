/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.odtn.cli.impl;

import com.google.common.annotations.Beta;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cli.net.ConnectPointCompleter;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.intent.IntentId;
import org.onosproject.odtn.GnpyService;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkArgument;
import static org.slf4j.LoggerFactory.getLogger;

@Beta
@Service
@Command(scope = "onos", name = "odtn-gnpy-connection-command",
        description = "show tapi context command")
public class GnpyConnectionRequestCommand extends AbstractShellCommand {

    private static final Logger log = getLogger(GnpyConnectionRequestCommand.class);

    @Argument(index = 0, name = "ingress",
            description = "Ingress Device/Port Description",
            required = true, multiValued = false)
    @Completion(ConnectPointCompleter.class)
    String ingressString = "";

    @Argument(index = 1, name = "egress",
            description = "Egress Device/Port Description",
            required = true, multiValued = false)
    @Completion(ConnectPointCompleter.class)
    String egressString = "";

    @Option(name = "-b", aliases = "--bidirectional",
            description = "If this argument is passed the optical link created will be bidirectional, " +
                    "else the link will be unidirectional.",
            required = false, multiValued = false)
    private boolean bidirectional = false;

    @Override
    protected void doExecute() {
        GnpyService service = get(GnpyService.class);

        if (!service.isConnected()) {
            print("gNPY is not connected, please issue `odtn-connect-gnpy-command` first");
            return;
        }

        ConnectPoint ingress = createConnectPoint(ingressString);
        ConnectPoint egress = createConnectPoint(egressString);

        Pair<IntentId, Double> intentIdAndOsnr =
                service.obtainConnectivity(ingress, egress, bidirectional);

        if (intentIdAndOsnr != null) {
            print("Optical Connectivity from %s to %s submitted through GNPy. \n", ingress, egress);
            print("Expected GSNR %.2f dB", intentIdAndOsnr.getRight().doubleValue());
            print("Intent: %s", intentIdAndOsnr.getLeft());
        } else {
            print("Error in submitting Optical Connectivity submitted through GNPy, please see logs");
        }
    }

    private ConnectPoint createConnectPoint(String devicePortString) {
        String[] splitted = devicePortString.split("/");

        checkArgument(splitted.length == 2,
                      "Connect point must be in \"deviceUri/portNumber\" format");

        return ConnectPoint.deviceConnectPoint(devicePortString);
    }

}
