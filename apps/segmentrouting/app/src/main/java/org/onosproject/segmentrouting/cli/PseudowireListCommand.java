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

import org.apache.karaf.shell.commands.Command;
import org.onlab.packet.VlanId;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.segmentrouting.SegmentRoutingService;
import org.onosproject.segmentrouting.pwaas.L2TunnelDescription;
/**
 * Command to show the pseudowires.
 */
@Command(scope = "onos", name = "sr-pw-list",
        description = "Lists all pseudowires")
public class PseudowireListCommand extends AbstractShellCommand {

    private static final String FORMAT_PSEUDOWIRE =
            "Pseudowire id = %s \n" +
                    "   mode : %s, sdTag : %s, pwLabel : %s \n" +
                    "   cP1 : %s , cP1OuterTag : %s, cP1InnerTag : %s \n" +
                    "   cP2 : %s , cP2OuterTag : %s, cP2InnerTag : %s \n" +
                    "   transportVlan : %s \n" +
                    "   pending = %s";

    @Override
    protected void execute() {

        SegmentRoutingService srService =
                AbstractShellCommand.get(SegmentRoutingService.class);

        srService.getL2TunnelDescriptions(false)
                .forEach(pw -> printPseudowire(pw, false));

        srService.getL2TunnelDescriptions(true)
                .forEach(pw -> printPseudowire(pw, true));
    }

    private void printPseudowire(L2TunnelDescription pseudowire, boolean pending) {
        VlanId vlan = pseudowire.l2Tunnel().transportVlan().equals(VlanId.vlanId((short) 4094)) ?
                VlanId.NONE : pseudowire.l2Tunnel().transportVlan();

        print(FORMAT_PSEUDOWIRE, pseudowire.l2Tunnel().tunnelId(), pseudowire.l2Tunnel().pwMode(),
              pseudowire.l2Tunnel().sdTag(), pseudowire.l2Tunnel().pwLabel(),
              pseudowire.l2TunnelPolicy().cP1(), pseudowire.l2TunnelPolicy().cP1OuterTag(),
              pseudowire.l2TunnelPolicy().cP1InnerTag(), pseudowire.l2TunnelPolicy().cP2(),
              pseudowire.l2TunnelPolicy().cP2OuterTag(), pseudowire.l2TunnelPolicy().cP2InnerTag(),
              vlan, pending);
    }
}