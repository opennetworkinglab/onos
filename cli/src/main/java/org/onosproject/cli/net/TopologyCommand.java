/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onosproject.cli.net;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyProvider;
import org.onosproject.net.topology.TopologyService;

/**
 * Lists summary of the current topology.
 */
@Command(scope = "onos", name = "topology",
description = "Lists summary of the current topology")
public class TopologyCommand extends AbstractShellCommand {

    private static final String FMT = "created=%s, uptime=%s, devices=%d, links=%d, clusters=%d";

    @Option(name = "-r", aliases = "--recompute",
            description = "Trigger topology re-computation", required = false,
            multiValued = false)
    private boolean recompute = false;

    protected TopologyService service;
    protected Topology topology;

    /**
     * Initializes the context for all cluster commands.
     */
    protected void init() {
        service = get(TopologyService.class);
        topology = service.currentTopology();
    }

    @Override
    protected void execute() {
        init();
        long topologyUptime =
                Math.max(0, (System.currentTimeMillis() - topology.creationTime()));
        if (recompute) {
            get(TopologyProvider.class).triggerRecompute();

        } else if (outputJson()) {
            print("%s",
                    jsonForEntity(topology, Topology.class));
        } else {
            print(FMT, formatCreationTime(topology.creationTime()),
                    formatElapsedTime(topologyUptime),
                    topology.deviceCount(), topology.linkCount(),
                    topology.clusterCount());
        }
    }

    /**
     * Converts millis to a formatted elapsed time string.
     *
     * @param millis Duration in millis to convert to a string
     *
     * @return Formatted string: "D days, H hrs, M mins, S secs".
     */
    private static String formatElapsedTime(long millis) {
        if (millis < 0) {
            throw new IllegalArgumentException("Interval less than zero. "
                    + "Possible unsynchronized timestamps");
        }

        final long days = TimeUnit.MILLISECONDS.toDays(millis);
        millis -= TimeUnit.DAYS.toMillis(days);
        final long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        final long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        final long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);

        final StringBuilder topologyUptimeString = new StringBuilder(64);
        topologyUptimeString.append(days);
        topologyUptimeString.append(" days, ");
        topologyUptimeString.append(hours);
        topologyUptimeString.append(" hrs, ");
        topologyUptimeString.append(minutes);
        topologyUptimeString.append(" mins, ");
        topologyUptimeString.append(seconds);
        topologyUptimeString.append(" secs");

        return (topologyUptimeString.toString());
    }

    /**
     * Converts millis to a formatted Date String.
     *
     * @param millis Duration in millis to convert to a string
     *
     * @return Formatted string: yyyy-MM-dd HH:mm:ss.
     */
    private static String formatCreationTime(long millis) {
        final DateFormat dateFormatter =
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        return (dateFormatter.format(calendar.getTime()));
    }
}
