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
package org.onosproject.l2lb.cli;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.core.ApplicationId;
import org.onosproject.l2lb.api.L2Lb;
import org.onosproject.l2lb.api.L2LbId;
import org.onosproject.l2lb.api.L2LbService;

import java.util.Map;

/**
 * Command to show all L2 load balancers.
 */
@Service
@Command(scope = "onos", name = "l2lbs", description = "Lists L2 load balancers")
public class L2LbListCommand extends AbstractShellCommand {

    // Operation constant
    private static final String AVAILABLE = "Available";

    @Override
    public void doExecute() {
        L2LbService service = get(L2LbService.class);
        // Get l2 load balancers and reservations
        Map<L2LbId, L2Lb> l2LbStore = service.getL2Lbs();
        Map<L2LbId, ApplicationId> l2LbResStore = service.getReservations();
        // Print id -> ports, mode, reservation
        l2LbStore.forEach((l2LbId, l2Lb) -> print("%s -> %s, %s, %s", l2LbId, l2Lb.ports(), l2Lb.mode(),
                                                  l2LbResStore.get(l2LbId) == null ?
                                                          AVAILABLE : l2LbResStore.get(l2LbId).name()));
    }
}