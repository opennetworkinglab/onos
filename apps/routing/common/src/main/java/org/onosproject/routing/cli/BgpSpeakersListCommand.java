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

package org.onosproject.routing.cli;

import com.google.common.collect.Lists;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.utils.Comparators;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.routing.RoutingService;
import org.onosproject.routing.config.BgpConfig;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Lists the BGP speakers configured in the system.
 */
@Service
@Command(scope = "onos", name = "bgp-speakers",
        description = "Lists all BGP speakers")
public class BgpSpeakersListCommand extends AbstractShellCommand {

    private static final String FORMAT = "port=%s/%s, vlan=%s, peers=%s";
    private static final String NAME_FORMAT = "%s: " + FORMAT;

    private static final Comparator<BgpConfig.BgpSpeakerConfig> SPEAKERS_COMPARATOR = (s1, s2) ->
            Comparators.CONNECT_POINT_COMPARATOR.compare(s1.connectPoint(), s2.connectPoint());

    @Override
    protected void doExecute() {
        NetworkConfigService configService = get(NetworkConfigService.class);
        CoreService coreService = get(CoreService.class);
        ApplicationId appId = coreService.getAppId(RoutingService.ROUTER_APP_ID);

        BgpConfig config = configService.getConfig(appId, BgpConfig.class);
        if (config == null) {
            print("No speakers configured");
            return;
        }

        List<BgpConfig.BgpSpeakerConfig> bgpSpeakers =
                Lists.newArrayList(config.bgpSpeakers());

        Collections.sort(bgpSpeakers, SPEAKERS_COMPARATOR);

        if (config.bgpSpeakers().isEmpty()) {
            print("No speakers configured");
        } else {
            bgpSpeakers.forEach(
                s -> {
                    if (s.name().isPresent()) {
                        print(NAME_FORMAT, s.name().get(), s.connectPoint().deviceId(),
                                s.connectPoint().port(), s.vlan(), s.peers());
                    } else {
                        print(FORMAT, s.connectPoint().deviceId(),
                                s.connectPoint().port(), s.vlan(), s.peers());
                    }
                });
        }
    }
}
