/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.packetstats.cli;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricFilter;
import org.apache.karaf.shell.api.action.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onlab.metrics.MetricsService;
import java.util.Map;
/**
 * Displays the entries.
 */
@Command(scope = "onos", name = "pkt-stats-show", description = "Displays the packet statistics values")
public class PacketStatisticsShowCommand extends AbstractShellCommand {

    private static final String METRIC_NAME = null;
    private static final String FORMAT = "PacketType = %s, Count = %s";
    private static final String FORMAT_LLDP = "Device = %s, Count = %s";
    MetricFilter filter = METRIC_NAME != null ? (name, metric) -> name.equals(METRIC_NAME) : MetricFilter.ALL;


    @Override
    protected void doExecute() {
            MetricsService service = get(MetricsService.class);
            Map<String, Counter> counters = service.getCounters(filter);

            Counter arpCounter = counters.get("packetStatisticsComponent.arpFeature.arpPC");
            Counter lldpCounter = counters.get("packetStatisticsComponent.lldpFeature.lldpPC");
            Counter nsCounter = counters.get("packetStatisticsComponent.nbrSolicitFeature.nbrSolicitPC");
            Counter naCounter = counters.get("packetStatisticsComponent.nbrAdvertFeature.nbrAdvertPC");

            print(FORMAT, "ARP ", arpCounter.getCount());
            print(FORMAT, "LLDP ", lldpCounter.getCount());
            print(FORMAT, "Neighbor Solicitation ", nsCounter.getCount());
            print(FORMAT, "Neighbor Advertisement ", naCounter.getCount());

    }

}
