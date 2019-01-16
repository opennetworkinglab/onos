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
package org.onosproject.portloadbalancer.cli;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.core.ApplicationId;
import org.onosproject.portloadbalancer.api.PortLoadBalancer;
import org.onosproject.portloadbalancer.api.PortLoadBalancerId;
import org.onosproject.portloadbalancer.api.PortLoadBalancerService;

import java.util.Map;

/**
 * Command to show all port load balancers.
 */
@Service
@Command(scope = "onos", name = "plbs", description = "Lists port load balancers")
public class PortLoadBalancerListCommand extends AbstractShellCommand {

    // Operation constant
    private static final String AVAILABLE = "Available";

    @Override
    public void doExecute() {
        PortLoadBalancerService service = get(PortLoadBalancerService.class);
        // Get port load balancers and reservations
        Map<PortLoadBalancerId, PortLoadBalancer> portLoadBalancerStore = service.getPortLoadBalancers();
        Map<PortLoadBalancerId, ApplicationId> portLoadBalancerResStore = service.getReservations();
        // Print id -> ports, mode, reservation
        portLoadBalancerStore.forEach((portLoadBalancerId, portLoadBalancer) ->
                print("%s -> %s, %s, %s", portLoadBalancerId, portLoadBalancer.ports(), portLoadBalancer.mode(),
                        portLoadBalancerResStore.get(portLoadBalancerId) == null ? AVAILABLE :
                                portLoadBalancerResStore.get(portLoadBalancerId).name()));
    }
}