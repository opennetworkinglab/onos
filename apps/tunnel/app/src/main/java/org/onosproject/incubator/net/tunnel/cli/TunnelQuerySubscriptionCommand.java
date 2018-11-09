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
package org.onosproject.incubator.net.tunnel.cli;

import java.util.Collection;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.incubator.net.tunnel.TunnelService;
import org.onosproject.incubator.net.tunnel.TunnelSubscription;

/**
 * Query all tunnel subscriptions of consumer by consumer id.
 * It's used by consumers.
 */
@Service
@Command(scope = "onos", name = "tunnel-subscriptions",
      description = "Query all request orders of consumer by consumer id. It's used by consumers.")
public class TunnelQuerySubscriptionCommand extends AbstractShellCommand {
    @Argument(index = 0, name = "consumerId",
            description = "consumer id means provider id",
            required = true, multiValued = false)
    String consumerId = null;
    private static final String FMT = "appId=%s, src=%s, dst=%s,"
            + "type=%s, tunnelId=%s";

    @Override
    protected void doExecute() {
        TunnelService service = get(TunnelService.class);
        ApplicationId applicationId = new DefaultApplicationId(1, consumerId);
        Collection<TunnelSubscription> tunnelSet = service.queryTunnelSubscription(applicationId);
        for (TunnelSubscription order : tunnelSet) {
            print(FMT, order.consumerId(), order.src(), order.dst(),
                  order.type(), order.tunnelId());
        }
    }

}
