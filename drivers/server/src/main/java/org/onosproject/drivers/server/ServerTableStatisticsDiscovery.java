/*
 * Copyright 2020-present Open Networking Foundation
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

package org.onosproject.drivers.server;

import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.TableStatisticsDiscovery;
import org.onosproject.net.flow.DefaultTableStatisticsEntry;
import org.onosproject.net.flow.IndexTableId;
import org.onosproject.net.flow.TableStatisticsEntry;
import org.onosproject.protocol.rest.RestSBDevice;

import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.collect.ImmutableList;

import java.io.InputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.ws.rs.ProcessingException;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.drivers.server.Constants.JSON;
import static org.onosproject.drivers.server.Constants.MSG_DEVICE_ID_NULL;
import static org.onosproject.drivers.server.Constants.MSG_DEVICE_NULL;
import static org.onosproject.drivers.server.Constants.MSG_NIC_TABLE_COUNTER_NEGATIVE;
import static org.onosproject.drivers.server.Constants.MSG_NIC_TABLE_INDEX_NEGATIVE;
import static org.onosproject.drivers.server.Constants.MSG_NIC_TABLE_SIZE_NEGATIVE;
import static org.onosproject.drivers.server.Constants.PARAM_ID;
import static org.onosproject.drivers.server.Constants.PARAM_NICS;
import static org.onosproject.drivers.server.Constants.PARAM_NIC_TABLE;
import static org.onosproject.drivers.server.Constants.PARAM_NIC_TABLE_ACTIVE_ENTRIES;
import static org.onosproject.drivers.server.Constants.PARAM_NIC_TABLE_MAX_SIZE;
import static org.onosproject.drivers.server.Constants.PARAM_NIC_TABLE_PKTS_LOOKED_UP;
import static org.onosproject.drivers.server.Constants.PARAM_NIC_TABLE_PKTS_MATCHED;
import static org.onosproject.drivers.server.Constants.URL_RULE_TABLE_STATS;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of table statistics discovery for server devices.
 */
public class ServerTableStatisticsDiscovery
        extends BasicServerDriver
        implements TableStatisticsDiscovery {

    private final Logger log = getLogger(getClass());

    public ServerTableStatisticsDiscovery() {
        super();
        log.debug("Started");
    }

    @Override
    public List<TableStatisticsEntry> getTableStatistics() {
        // Retrieve the device ID from the handler
        DeviceId deviceId = super.getDeviceId();
        checkNotNull(deviceId, MSG_DEVICE_ID_NULL);

        // Get the device
        RestSBDevice device = super.getDevice(deviceId);
        checkNotNull(device, MSG_DEVICE_NULL);

        // Hit the path that provides the server's NIC table statistics
        InputStream response = null;
        try {
            response = getController().get(deviceId, URL_RULE_TABLE_STATS, JSON);
        } catch (ProcessingException pEx) {
            log.error("Failed to get NIC table statistics from device: {}", deviceId);
            return Collections.EMPTY_LIST;
        }

        // Load the JSON into object
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> jsonMap = null;
        JsonNode jsonNode = null;
        ObjectNode objNode = null;
        try {
            jsonMap  = mapper.readValue(response, Map.class);
            jsonNode = mapper.convertValue(jsonMap, JsonNode.class);
            objNode = (ObjectNode) jsonNode;
        } catch (IOException ioEx) {
            log.error("Failed to get NIC table statistics from device: {}", deviceId);
            return Collections.EMPTY_LIST;
        }

        if (jsonNode == null) {
            log.error("Failed to get NIC table statistics from device: {}", deviceId);
            return Collections.EMPTY_LIST;
        }

        List<TableStatisticsEntry> tableStats = Lists.newArrayList();

        JsonNode nicNode = objNode.path(PARAM_NICS);

        for (JsonNode nn : nicNode) {
            ObjectNode nicObjNode = (ObjectNode) nn;

            // The index of the NIC that hosts rules table(s)
            long nicIndex = nicObjNode.path(Constants.PARAM_ID).asLong();

            JsonNode tableNode = nicObjNode.path(PARAM_NIC_TABLE);
            if (tableNode == null) {
                throw new IllegalArgumentException("No tables reported for NIC " + nicIndex);
            }

            for (JsonNode tn : tableNode) {
                ObjectNode tableObjNode = (ObjectNode) tn;

                // NIC table attributes
                int tableIndex = tableObjNode.path(PARAM_ID).asInt();
                checkArgument(tableIndex >= 0, MSG_NIC_TABLE_INDEX_NEGATIVE);

                long tableActiveEntries = tableObjNode.path(PARAM_NIC_TABLE_ACTIVE_ENTRIES).asLong();
                checkArgument(tableActiveEntries >= 0, MSG_NIC_TABLE_COUNTER_NEGATIVE);

                long tablePktsLookedUp = tableObjNode.path(PARAM_NIC_TABLE_PKTS_LOOKED_UP).asLong();
                checkArgument(tablePktsLookedUp >= 0, MSG_NIC_TABLE_COUNTER_NEGATIVE);

                long tablePktsMatched = tableObjNode.path(PARAM_NIC_TABLE_PKTS_MATCHED).asLong();
                checkArgument(tablePktsMatched >= 0, MSG_NIC_TABLE_COUNTER_NEGATIVE);

                long tableMaxsize = tableObjNode.path(PARAM_NIC_TABLE_MAX_SIZE).asLong();
                checkArgument(tableMaxsize >= 0, MSG_NIC_TABLE_SIZE_NEGATIVE);

                // Server's device ID and NIC ID compose a NIC device ID
                DeviceId nicDeviceId = DeviceId.deviceId(
                    deviceId.toString() + ":nic" + String.valueOf(nicIndex));

                TableStatisticsEntry tableStat = DefaultTableStatisticsEntry.builder()
                    .withDeviceId(nicDeviceId)
                    .withTableId(IndexTableId.of(tableIndex))
                    .withActiveFlowEntries(tableActiveEntries)
                    .withPacketsLookedUpCount(tablePktsLookedUp)
                    .withPacketsMatchedCount(tablePktsMatched)
                    .withMaxSize(tableMaxsize > 0 ? tableMaxsize : -1)
                    .build();

                tableStats.add(tableStat);

                log.debug("[Device {}] NIC {} with table statistics: {}",
                    deviceId, nicIndex, tableStat);
            }
        }

        return ImmutableList.copyOf(tableStats);
    }

}
