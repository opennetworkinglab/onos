/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.packetthrottle.cli;

import org.apache.karaf.shell.api.action.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.net.packet.PacketInFilter;
import org.onosproject.packetthrottle.api.PacketThrottleService;
import java.util.Map;


/**
 * Displays the statistics of the packets dropped due to throttle.
 */
@Service
@Command(scope = "onos", name = "pkt-stats-overflow-show",
        description = "Displays the packet overflow statistics values")
public class PacketOverFlowStatsShowCommand extends AbstractShellCommand {

    private static final String FORMAT = "PacketType = %s, Count = %s";



    @Override
    protected void doExecute() {
        PacketInFilter filter;
        PacketThrottleService packetThrottleService = get(PacketThrottleService.class);
        Map<String, PacketInFilter> filterMap = packetThrottleService.filterMap();
        for (Map.Entry<String, PacketInFilter> entry: filterMap.entrySet()) {
            filter = entry.getValue();
            print(FORMAT, filter.name(), filter.droppedPackets());
        }
    }
}
