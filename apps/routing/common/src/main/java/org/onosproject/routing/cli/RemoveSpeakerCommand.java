/*
 * Copyright 2017-present Open Networking Laboratory
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

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.routing.RoutingService;
import org.onosproject.routing.config.BgpConfig;

/**
 * Command to remove a internal BGP speaker.
 */
@Command(scope = "onos", name = "bgp-speaker-remove",
        description = "Removes an internal BGP speaker")
public class RemoveSpeakerCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "name",
            description = "Name of the internal BGP speaker",
            required = true, multiValued = false)
    String name = null;

    private static final String SPEAKER_REMOVE_SUCCESS = "Speaker Successfully Removed.";
    private static final String NO_CONFIGURATION = "No speakers configured";
    private static final String PEERS_EXIST =
            "Speaker with name \'%s\' has peer connections";
    private static final String SPEAKER_NOT_FOUND =
            "Speaker with name \'%s\' not found";

    @Override
    protected void execute() {
        NetworkConfigService configService = get(NetworkConfigService.class);
        CoreService coreService = get(CoreService.class);
        ApplicationId appId = coreService.getAppId(RoutingService.ROUTER_APP_ID);

        BgpConfig config = configService.getConfig(appId, BgpConfig.class);
        if (config == null || config.bgpSpeakers().isEmpty()) {
            print(NO_CONFIGURATION);
            return;
        }

        BgpConfig.BgpSpeakerConfig speaker = config.getSpeakerWithName(name);
        if (speaker == null) {
            print(SPEAKER_NOT_FOUND, name);
            return;
        } else {
            if (!speaker.peers().isEmpty()) {
                // Removal not allowed when peer connections exist.
                print(PEERS_EXIST, name);
                return;
            }
        }

        removeSpeakerFromConf(config);
        configService.applyConfig(appId, BgpConfig.class, config.node());

        print(SPEAKER_REMOVE_SUCCESS);
    }

    /**
     * Removes the speaker from the BgpConfig service.
     *
     * @param bgpConfig the BGP configuration
     */
    private void removeSpeakerFromConf(BgpConfig bgpConfig) {
        log.debug("Removing speaker from configuration: {}", name);

        bgpConfig.removeSpeaker(name);
    }
}
