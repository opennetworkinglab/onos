/*
 * Copyright 2014-2015 Open Networking Laboratory
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

package org.onosproject.sdnip.cli;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onlab.packet.IpAddress;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.routing.RoutingService;
import org.onosproject.routing.config.BgpConfig;

import java.util.HashSet;
import java.util.Optional;

/**
 * Command to add a new internal BGP speaker.
 */
@Command(scope = "onos", name = "add-bgp-speaker",
        description = "Adds an internal BGP speaker")
public class AddSpeakerCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "name",
            description = "Name of the internal BGP speaker",
            required = true, multiValued = false)
    String name = null;

    @Argument(index = 1, name = "connectionPoint",
            description = "Interface to the BGP speaker",
            required = true, multiValued = false)
    String connectionPoint = null;

    private static final String SPEAKER_ADD_SUCCESS = "Speaker Successfully Added.";

    @Override
    protected void execute() {
        NetworkConfigService configService = get(NetworkConfigService.class);
        CoreService coreService = get(CoreService.class);
        ApplicationId appId = coreService.getAppId(RoutingService.ROUTER_APP_ID);

        BgpConfig config = configService.addConfig(appId, BgpConfig.class);

        BgpConfig.BgpSpeakerConfig speaker = config.getSpeakerWithName(name);
        if (speaker != null) {
            log.debug("Speaker already exists: {}", name);
            return;
        }

        addSpeakerToConf(config);
        configService.applyConfig(appId, BgpConfig.class, config.node());

        print(SPEAKER_ADD_SUCCESS);
    }

    /**
     * Adds the speaker to the BgpConfig service.
     *
     * @param config the BGP configuration
     */
    private void addSpeakerToConf(BgpConfig config) {
        log.debug("Adding new speaker to configuration: {}", name);
        BgpConfig.BgpSpeakerConfig speaker = getSpeaker();

        config.addSpeaker(speaker);
    }

    private BgpConfig.BgpSpeakerConfig getSpeaker() {
        ConnectPoint connectPoint = ConnectPoint.
                deviceConnectPoint(connectionPoint);
        return new BgpConfig.BgpSpeakerConfig(Optional.ofNullable(name),
                connectPoint, new HashSet<IpAddress>());
    }
}
