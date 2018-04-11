/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.segmentrouting.cli;


import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.ConnectPoint;
import org.onosproject.segmentrouting.SegmentRoutingService;
import org.onosproject.segmentrouting.pwaas.DefaultL2Tunnel;
import org.onosproject.segmentrouting.pwaas.DefaultL2TunnelDescription;
import org.onosproject.segmentrouting.pwaas.DefaultL2TunnelPolicy;
import org.onosproject.segmentrouting.pwaas.L2Tunnel;
import org.onosproject.segmentrouting.pwaas.L2TunnelDescription;
import org.onosproject.segmentrouting.pwaas.L2TunnelHandler;
import org.onosproject.segmentrouting.pwaas.L2TunnelPolicy;

import static org.onosproject.segmentrouting.pwaas.PwaasUtil.*;


/**
 * Command to add a pseuwodire.
 */
@Command(scope = "onos", name = "sr-pw-add",
        description = "Add a pseudowire to the network configuration, if it already exists update it.")
public class PseudowireAddCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "pwId",
            description = "Pseudowire ID",
            required = true, multiValued = false)
    String pwId;

    @Argument(index = 1, name = "pwLabel",
            description = "Pseudowire Label",
            required = true, multiValued = false)
    String pwLabel;

    @Argument(index = 2, name = "mode",
            description = "Mode used for pseudowire",
            required = true, multiValued = false)
    String mode;

    @Argument(index = 3, name = "sDTag",
            description = "Service delimiting tag",
            required = true, multiValued = false)
    String sDTag;

    @Argument(index = 4, name = "cP1",
            description = "Connection Point 1",
            required = true, multiValued = false)
    String cP1;

    @Argument(index = 5, name = "cP1InnerVlan",
            description = "Inner Vlan of Connection Point 1",
            required = true, multiValued = false)
    String cP1InnerVlan;

    @Argument(index = 6, name = "cP1OuterVlan",
            description = "Outer Vlan of Connection Point 1",
            required = true, multiValued = false)
    String cP1OuterVlan;

    @Argument(index = 7, name = "cP2",
            description = "Connection Point 2",
            required = true, multiValued = false)
    String cP2;

    @Argument(index = 8, name = "cP2InnerVlan",
            description = "Inner Vlan of Connection Point 2",
            required = true, multiValued = false)
    String cP2InnerVlan;

    @Argument(index = 9, name = "cP2OuterVlan",
            description = "Outer Vlan of Connection Point 2",
            required = true, multiValued = false)
    String cP2OuterVlan;

    @Override
    protected void execute() {

        SegmentRoutingService srService =
                AbstractShellCommand.get(SegmentRoutingService.class);

        L2Tunnel tun;
        L2TunnelPolicy policy;

        try {
            tun = new DefaultL2Tunnel(parseMode(mode), parseVlan(sDTag), parsePwId(pwId), parsePWLabel(pwLabel));
        } catch (IllegalArgumentException e) {
            log.error("Exception while parsing L2Tunnel : \n\t %s", e.getMessage());
            print("Exception while parsing L2Tunnel : \n\t %s", e.getMessage());
            return;
        }

        try {
            policy = new DefaultL2TunnelPolicy(parsePwId(pwId),
                                               ConnectPoint.deviceConnectPoint(cP1), parseVlan(cP1InnerVlan),
                                               parseVlan(cP1OuterVlan), ConnectPoint.deviceConnectPoint(cP2),
                                               parseVlan(cP2InnerVlan), parseVlan(cP2OuterVlan));

        } catch (IllegalArgumentException e) {
            log.error("Exception while parsing L2TunnelPolicy : \n\t %s", e.getMessage());
            print("Exception while parsing L2TunnelPolicy : \n\t %s", e.getMessage());
            return;
        }

        L2TunnelDescription pw = new DefaultL2TunnelDescription(tun, policy);
        L2TunnelHandler.Result res = srService.addPseudowire(pw);
        log.info("Deploying pseudowire {} via the command line.", pw);
        switch (res) {
            case WRONG_PARAMETERS:
                print("Pseudowire could not be added , error in the parameters : \n\t%s",
                      res.getSpecificError());
                break;
            case CONFIGURATION_ERROR:
                print("Pseudowire could not be added, configuration error : \n\t%s",
                      res.getSpecificError());
                break;
            case PATH_NOT_FOUND:
                print("Pseudowire path not found : \n\t%s",
                      res.getSpecificError());
                break;
            case INTERNAL_ERROR:
                print("Pseudowire could not be added, internal error : \n\t%s",
                      res.getSpecificError());
                break;
            case SUCCESS:
                break;
            default:
                break;
        }
    }
}