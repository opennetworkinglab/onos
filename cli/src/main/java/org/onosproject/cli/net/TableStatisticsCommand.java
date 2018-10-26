/*
 * Copyright 2015-present Open Networking Foundation
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

import static com.google.common.collect.Lists.newArrayList;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.action.Option;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.utils.Comparators;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TableStatisticsEntry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Lists port statistic of all ports in the system.
 */
@Service
@Command(scope = "onos", name = "tablestats",
        description = "Lists statistics of all tables in the device")
public class TableStatisticsCommand extends AbstractShellCommand {

    @Option(name = "-t", aliases = "--table", description = "Show human readable table format for statistics",
            required = false, multiValued = false)
    private boolean table = false;

    @Argument(index = 0, name = "uri", description = "Device ID",
            required = false, multiValued = false)
    String uri = null;

    private static final String FORMAT =
            "   table=%s, active=%s, lookedup=%s, matched=%s, maxsize=%s";
    private static final String NA = "N/A";

    @Override
    protected void doExecute() {
        FlowRuleService flowService = get(FlowRuleService.class);
        DeviceService deviceService = get(DeviceService.class);
        SortedMap<Device, List<TableStatisticsEntry>> deviceTableStats =
                getSortedTableStats(deviceService, flowService);

        if (outputJson()) {
            print("%s", json(deviceTableStats.keySet(), deviceTableStats));
        } else {
            deviceTableStats.forEach((device, tableStats) -> printTableStats(device, tableStats));
        }
    }

    /**
     * Produces a JSON array of table statistics grouped by the each device.
     *
     * @param devices     collection of devices
     * @param deviceTableStats collection of table statistics per each device
     * @return JSON array
     */
    private JsonNode json(Iterable<Device> devices,
                          Map<Device, List<TableStatisticsEntry>> deviceTableStats) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();
        for (Device device : devices) {
            result.add(json(mapper, device, deviceTableStats.get(device)));
        }
        return result;
    }

    // Produces JSON object with the table statistics of the given device.
    private ObjectNode json(ObjectMapper mapper,
                            Device device, List<TableStatisticsEntry> tableStats) {
        ObjectNode result = mapper.createObjectNode();
        ArrayNode array = mapper.createArrayNode();

        tableStats.forEach(tableStat -> array.add(jsonForEntity(tableStat, TableStatisticsEntry.class)));

        result.put("device", device.id().toString())
                .put("tableCount", tableStats.size())
                .set("tables", array);
        return result;
    }

    /**
     * Prints flow table statistics.
     *
     * @param d     the device
     * @param tableStats the set of flow table statistics for that device
     */
    protected void printTableStats(Device d,
                                   List<TableStatisticsEntry> tableStats) {
        boolean empty = tableStats == null || tableStats.isEmpty();
        print("deviceId=%s, tableCount=%d", d.id(), empty ? 0 : tableStats.size());
        if (!empty) {
            for (TableStatisticsEntry t : tableStats) {
                print(FORMAT, t.table(), t.activeFlowEntries(),
                        t.hasPacketsLookedup() ? t.packetsLookedup() : NA, t.packetsMatched(),
                        t.hasMaxSize() ? t.maxSize() : NA);
            }
        }
    }

    /**
     * Returns the list of table statistics sorted using the device ID URIs and table IDs.
     *
     * @param deviceService device service
     * @param flowService flow rule service
     * @return sorted table statistics list
     */
    protected SortedMap<Device, List<TableStatisticsEntry>> getSortedTableStats(DeviceService deviceService,
                                                                                FlowRuleService flowService) {
        SortedMap<Device, List<TableStatisticsEntry>> deviceTableStats = new TreeMap<>(Comparators.ELEMENT_COMPARATOR);
        List<TableStatisticsEntry> tableStatsList;
        Iterable<Device> devices = uri == null ? deviceService.getDevices() :
                Collections.singletonList(deviceService.getDevice(DeviceId.deviceId(uri)));
        for (Device d : devices) {
            tableStatsList = newArrayList(flowService.getFlowTableStatistics(d.id()));
            tableStatsList.sort((p1, p2) -> Integer.valueOf(p1.tableId()).compareTo(Integer.valueOf(p2.tableId())));
            deviceTableStats.put(d, tableStatsList);
        }
        return deviceTableStats;
    }

}