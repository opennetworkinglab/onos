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
import org.onosproject.segmentrouting.SegmentRoutingService;
import org.onosproject.segmentrouting.pwaas.L2TunnelHandler;


/**
 * Command to add a pseuwodire.
 */
@Command(scope = "onos", name = "pseudowire-add",
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

        L2TunnelHandler.Result res = srService.addPseudowire(pwId, pwLabel,
                                                             cP1, cP1InnerVlan, cP1OuterVlan,
                                                             cP2, cP2InnerVlan, cP2OuterVlan,
                                                             mode, sDTag);
        switch (res) {
            case ADDITION_ERROR:
                print("Pseudowire could not be added, error in configuration, please check logs for more details!");
                break;
            case CONFIG_NOT_FOUND:
                print("Configuration for pwaas was not found! Initialize the configuration first through netcfg.");
                break;
            default:
                break;
        }

    }
}