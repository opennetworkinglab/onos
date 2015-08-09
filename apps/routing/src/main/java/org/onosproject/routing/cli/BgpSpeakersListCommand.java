/*
 * Copyright 2015 Open Networking Laboratory
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

package org.onosproject.routing.cli;

import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.incubator.net.config.NetworkConfigService;
import org.onosproject.routing.RoutingService;
import org.onosproject.routing.config.impl.BgpConfig;

/**
 * Lists the BGP speakers configured in the system.
 */
@Command(scope = "onos", name = "bgp-speakers",
        description = "Lists all BGP speakers")
public class BgpSpeakersListCommand extends AbstractShellCommand {

    private static final String FORMAT = "%s : %s";

    @Override
    protected void execute() {
        NetworkConfigService configService = get(NetworkConfigService.class);
        CoreService coreService = get(CoreService.class);
        ApplicationId appId = coreService.getAppId(RoutingService.ROUTER_APP_ID);

        print(appId.toString());

        BgpConfig config = configService.getConfig(appId, BgpConfig.class);

        if (config == null || config.bgpSpeakers().isEmpty()) {
            print("No speakers configured");
        } else {
            config.bgpSpeakers().forEach(
                    s -> print(FORMAT, s.connectPoint(), s.listenAddresses()));
        }
    }
}
