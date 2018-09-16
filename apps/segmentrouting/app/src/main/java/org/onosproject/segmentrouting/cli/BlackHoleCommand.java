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

package org.onosproject.segmentrouting.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onlab.packet.IpPrefix;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.segmentrouting.SegmentRoutingService;
import org.onosproject.segmentrouting.config.SegmentRoutingAppConfig;

import java.util.Set;

/**
 * CLI command for managing black hole routes.
 */
@Command(scope = "onos", name = "sr-blackhole",
        description = "Manage black hole routes")
public class BlackHoleCommand extends AbstractShellCommand {
    @Argument(index = 0, name = "op",
            description = "list, add or remove",
            required = true, multiValued = false)
    private String op;

    @Argument(index = 1, name = "prefix",
            description = "IP prefix",
            required = false, multiValued = false)
    private String prefix;

    @Override
    protected void execute() {
        SegmentRoutingService srService = AbstractShellCommand.get(SegmentRoutingService.class);
        NetworkConfigService netcfgService = AbstractShellCommand.get(NetworkConfigService.class);

        SegmentRoutingAppConfig appConfig = netcfgService.getConfig(srService.appId(), SegmentRoutingAppConfig.class);
        if (appConfig == null) {
            JsonNode jsonNode = new ObjectMapper().createObjectNode();
            netcfgService.applyConfig(srService.appId(), SegmentRoutingAppConfig.class, jsonNode);
            appConfig = netcfgService.getConfig(srService.appId(), SegmentRoutingAppConfig.class);
        }

        Set<IpPrefix> blackHoleIps;
        switch (op) {
            case "list":
                appConfig.blackholeIPs().forEach(prefix -> print(prefix.toString()));
                break;
            case "add":
                blackHoleIps = Sets.newConcurrentHashSet(appConfig.blackholeIPs());
                blackHoleIps.add(IpPrefix.valueOf(prefix));
                appConfig.setBalckholeIps(blackHoleIps);
                break;
            case "remove":
                blackHoleIps = Sets.newConcurrentHashSet(appConfig.blackholeIPs());
                blackHoleIps.remove(IpPrefix.valueOf(prefix));
                appConfig.setBalckholeIps(blackHoleIps);
                break;
            default:
                throw new UnsupportedOperationException("Unknown operation " + op);
        }
    }
}
