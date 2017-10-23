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
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.segmentrouting.SegmentRoutingService;
import org.onosproject.segmentrouting.pwaas.DefaultL2Tunnel;
import org.onosproject.segmentrouting.pwaas.DefaultL2TunnelDescription;
import org.onosproject.segmentrouting.pwaas.DefaultL2TunnelPolicy;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Command to show the pseudowires.
 */
@Command(scope = "onos", name = "pseudowires",
        description = "Lists all pseudowires")
public class PseudowireListCommand extends AbstractShellCommand {

    private static final String FORMAT_PSEUDOWIRE =
            "Pseudowire id = %s \n" +
                    "   mode : %s, sdTag : %s, pwLabel : %s \n" +
                    "   cP1 : %s , cP1OuterTag : %s, cP1InnerTag : %s \n" +
                    "   cP2 : %s , cP2OuterTag : %s, cP2InnerTag : %s \n" /* +
                    "   Path used : (%s - %s) <-> (%s - %s) \n" */;

                    // TODO:  uncomment string when path failures are fixed also for the links
                    // TODO:  used in spine for pw traffic.
    @Override
    protected void execute() {

        SegmentRoutingService srService =
                AbstractShellCommand.get(SegmentRoutingService.class);

        List<DefaultL2Tunnel> tunnels = srService.getL2Tunnels();
        List<DefaultL2TunnelPolicy> policies = srService.getL2Policies();

        // combine polices and tunnels to pseudowires
        List<DefaultL2TunnelDescription> pseudowires = tunnels.stream()
                                    .map(l2Tunnel -> {
                                            DefaultL2TunnelPolicy policy = null;
                                            for (DefaultL2TunnelPolicy l2Policy : policies) {
                                                if (l2Policy.tunnelId() == l2Tunnel.tunnelId()) {
                                                    policy = l2Policy;
                                                    break;
                                                }
                                            }

                                            return new DefaultL2TunnelDescription(l2Tunnel, policy);
                                    })
                                    .collect(Collectors.toList());

        pseudowires.forEach(pw -> printPseudowire(pw));
    }

    private void printPseudowire(DefaultL2TunnelDescription pseudowire) {


        print(FORMAT_PSEUDOWIRE, pseudowire.l2Tunnel().tunnelId(), pseudowire.l2Tunnel().pwMode(),
              pseudowire.l2Tunnel().sdTag(), pseudowire.l2Tunnel().pwLabel(),
              pseudowire.l2TunnelPolicy().cP1(), pseudowire.l2TunnelPolicy().cP1OuterTag(),
              pseudowire.l2TunnelPolicy().cP1InnerTag(), pseudowire.l2TunnelPolicy().cP2(),
              pseudowire.l2TunnelPolicy().cP2OuterTag(), pseudowire.l2TunnelPolicy().cP2InnerTag()/*,
              pseudowire.l2Tunnel().pathUsed().get(0).src(), pseudowire.l2Tunnel().pathUsed().get(0).dst(),
              pseudowire.l2Tunnel().pathUsed().get(1).src(), pseudowire.l2Tunnel().pathUsed().get(1).dst()*/);

        // TODO: uncomment arguments when path issue is fixed for spine switches
    }
}