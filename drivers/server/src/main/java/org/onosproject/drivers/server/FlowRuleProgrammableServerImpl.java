/*
 * Copyright 2018 Open Networking Foundation
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

import org.onosproject.drivers.server.devices.nic.NicFlowRule;
import org.onosproject.drivers.server.devices.nic.NicRxFilter.RxFilter;
import org.onosproject.net.DeviceId;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleProgrammable;
import org.onosproject.net.flow.FlowRuleService;

import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.ws.rs.ProcessingException;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.drivers.server.Constants.JSON;
import static org.onosproject.drivers.server.Constants.MSG_DEVICE_ID_NULL;
import static org.onosproject.drivers.server.Constants.PARAM_CPUS;
import static org.onosproject.drivers.server.Constants.PARAM_ID;
import static org.onosproject.drivers.server.Constants.PARAM_NAME;
import static org.onosproject.drivers.server.Constants.PARAM_NICS;
import static org.onosproject.drivers.server.Constants.PARAM_NIC_RX_FILTER;
import static org.onosproject.drivers.server.Constants.PARAM_NIC_RX_FILTER_FD;
import static org.onosproject.drivers.server.Constants.PARAM_NIC_RX_METHOD;
import static org.onosproject.drivers.server.Constants.PARAM_RULES;
import static org.onosproject.drivers.server.Constants.PARAM_RULE_CONTENT;
import static org.onosproject.drivers.server.Constants.SLASH;
import static org.onosproject.drivers.server.Constants.URL_RULE_MANAGEMENT;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Manages rules on commodity server devices, by
 * converting ONOS FlowRule objetcs into
 * network interface card (NIC) rules and vice versa.
 */
public class FlowRuleProgrammableServerImpl
        extends BasicServerDriver
        implements FlowRuleProgrammable {

    private final Logger log = getLogger(getClass());

    /**
     * Driver's property to specify how many rules the controller can remove at once.
     */
    private static final String RULE_DELETE_BATCH_SIZE_PROPERTY = "ruleDeleteBatchSize";

    public FlowRuleProgrammableServerImpl() {
        super();
        log.debug("Started");
    }

    @Override
    public Collection<FlowEntry> getFlowEntries() {
        DeviceId deviceId = getDeviceId();
        checkNotNull(deviceId, MSG_DEVICE_ID_NULL);

        // Expected FlowEntries installed through ONOS
        FlowRuleService flowService = getHandler().get(FlowRuleService.class);
        Iterable<FlowEntry> flowEntries = flowService.getFlowEntries(deviceId);

        // Hit the path that provides the server's flow rules
        InputStream response = null;
        try {
            response = getController().get(deviceId, URL_RULE_MANAGEMENT, JSON);
        } catch (ProcessingException pEx) {
            log.error("Failed to get NIC flow entries from device: {}", deviceId);
            return Collections.EMPTY_LIST;
        }

        // Load the JSON into objects
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode objNode = null;
        try {
            Map<String, Object> jsonMap  = mapper.readValue(response, Map.class);
            JsonNode jsonNode = mapper.convertValue(jsonMap, JsonNode.class);
            objNode = (ObjectNode) jsonNode;
        } catch (IOException ioEx) {
            log.error("Failed to get NIC flow entries from device: {}", deviceId);
            return Collections.EMPTY_LIST;
        }

        if (objNode == null) {
            log.error("Failed to get NIC flow entries from device: {}", deviceId);
            return Collections.EMPTY_LIST;
        }

        JsonNode scsNode = objNode.path(PARAM_RULES);

        // Here we store the trully installed rules
        Collection<FlowEntry> actualFlowEntries =
            Sets.<FlowEntry>newConcurrentHashSet();

        for (JsonNode scNode : scsNode) {
            String scId = get(scNode, PARAM_ID);
            String rxFilter = get(
                scNode.path(PARAM_NIC_RX_FILTER), PARAM_NIC_RX_METHOD);

            // Only Flow-based RxFilter is permitted
            if (RxFilter.getByName(rxFilter) != RxFilter.FLOW) {
                log.warn("Device with Rx filter {} is not managed by this driver",
                    rxFilter.toString().toUpperCase());
                continue;
            }

            // Each device might have multiple NICs
            for (JsonNode nicNode : scNode.path(PARAM_NICS)) {
                JsonNode cpusNode = nicNode.path(PARAM_CPUS);

                // Each NIC can dispatch to multiple CPU cores
                for (JsonNode cpuNode : cpusNode) {
                    String cpuId = get(cpuNode, PARAM_ID);
                    JsonNode rulesNode = cpuNode.path(PARAM_RULES);

                    // Multiple rules might correspond to each CPU core
                    for (JsonNode ruleNode : rulesNode) {
                        long ruleId = ruleNode.path(PARAM_ID).asLong();
                        String ruleContent = get(ruleNode, PARAM_RULE_CONTENT);

                        // Search for this rule ID in ONOS's store
                        FlowRule r = findRuleInFlowEntries(flowEntries, ruleId);

                        // Local rule, not present in the controller => Ignore
                        if (r == null) {
                            continue;
                        // Rule trully present in the data plane => Add
                        } else {
                            actualFlowEntries.add(new DefaultFlowEntry(
                                r, FlowEntry.FlowEntryState.ADDED, 0, 0, 0));
                        }
                    }
                }
            }
        }

        return actualFlowEntries;
    }

    @Override
    public Collection<FlowRule> applyFlowRules(Collection<FlowRule> rules) {
        DeviceId deviceId = getDeviceId();
        checkNotNull(deviceId, MSG_DEVICE_ID_NULL);

        // Set of truly-installed rules to be reported
        Set<FlowRule> installedRules = Sets.<FlowRule>newConcurrentHashSet();

        // Splits the rule set into multiple ones, grouped by traffic class ID
        Map<String, Set<FlowRule>> rulesPerTc = groupRules(rules);

        // Install NIC rules on a per-traffic class basis
        for (Map.Entry<String, Set<FlowRule>> entry : rulesPerTc.entrySet()) {
            String tcId = entry.getKey();
            Set<FlowRule> tcRuleSet = entry.getValue();

            installedRules.addAll(
                installNicFlowRules(deviceId, tcId, tcRuleSet)
            );
        }

        return installedRules;
    }

    @Override
    public Collection<FlowRule> removeFlowRules(Collection<FlowRule> rules) {
        DeviceId deviceId = getDeviceId();
        checkNotNull(deviceId, MSG_DEVICE_ID_NULL);

        int ruleDeleteBatchSize = getRuleDeleteBatchSizeProperty(deviceId);

        // Set of truly-removed rules to be reported
        Set<FlowRule> removedRules = Sets.<FlowRule>newConcurrentHashSet();

        List<FlowRule> ruleList = (List) rules;
        int ruleCount = rules.size();
        int ruleStart = 0;
        int processed = 0;
        int batchNb   = 1;
        while (processed < ruleCount) {
            String ruleIds = "";

            for (int i = ruleStart; i < ruleCount; i++) {
                // Batch completed
                if (i >= (batchNb * ruleDeleteBatchSize)) {
                    break;
                }

                // TODO: Turn this string into a list and modify removeNicFlowRuleBatch()
                // Create a comma-separated sequence of rule IDs
                ruleIds += Long.toString(ruleList.get(i).id().value()) + ",";

                processed++;
            }

            // Remove last comma
            ruleIds = ruleIds.substring(0, ruleIds.length() - 1);

            // Remove the entire batch of rules at once
            if (removeNicFlowRuleBatch(deviceId, ruleIds)) {
                removedRules.addAll(ruleList.subList(ruleStart, processed));
            }

            // Prepare for the next batch (if any)
            batchNb++;
            ruleStart += ruleDeleteBatchSize;
        }

        return removedRules;
    }

    /**
     * Groups a set of FlowRules by their traffic class ID.
     *
     * @param rules set of NIC rules to install
     * @return a map of traffic class IDs to their set of NIC rules
     */
    private Map<String, Set<FlowRule>> groupRules(Collection<FlowRule> rules) {
        Map<String, Set<FlowRule>> rulesPerTc =
            new ConcurrentHashMap<String, Set<FlowRule>>();

        rules.forEach(rule -> {
            if (!(rule instanceof FlowEntry)) {
                NicFlowRule nicRule = null;

                // Only NicFlowRules are accepted
                try {
                    nicRule = (NicFlowRule) rule;
                } catch (ClassCastException cEx) {
                    log.warn("Skipping flow rule not crafted for NIC: {}", rule);
                }

                if (nicRule != null) {
                    String tcId = nicRule.trafficClassId();

                    // Create a bucket of flow rules for this traffic class
                    if (!rulesPerTc.containsKey(tcId)) {
                        rulesPerTc.put(tcId, Sets.<FlowRule>newConcurrentHashSet());
                    }

                    Set<FlowRule> tcRuleSet = rulesPerTc.get(tcId);
                    tcRuleSet.add(nicRule);
                }
            }
        });

        return rulesPerTc;
    }

    /**
     * Searches for a flow rule with certain ID.
     *
     * @param flowEntries a list of FlowEntries
     * @param ruleId a desired rule ID
     * @return a FlowRule that corresponds to the desired ID or null
     */
    private FlowRule findRuleInFlowEntries(
            Iterable<FlowEntry> flowEntries, long ruleId) {
        for (FlowEntry fe : flowEntries) {
            if (fe.id().value() == ruleId) {
                return (FlowRule) fe;
            }
        }

        return null;
    }

    /**
     * Installs a set of FlowRules of the same traffic class ID
     * on a server device.
     *
     * @param deviceId target server device ID
     * @param trafficClassId traffic class ID of the NIC rules
     * @param rules set of NIC rules to install
     * @return a set of successfully installed NIC rules
     */
    private Collection<FlowRule> installNicFlowRules(
            DeviceId deviceId, String trafficClassId,
            Collection<FlowRule> rules) {
        int rulesToInstall = rules.size();
        if (rulesToInstall == 0) {
            return Collections.EMPTY_LIST;
        }

        ObjectMapper mapper = new ObjectMapper();

        // Create the object node to host the list of rules
        ObjectNode scsObjNode = mapper.createObjectNode();

        // Add the service chain's traffic class ID that requested these rules
        scsObjNode.put(PARAM_ID, trafficClassId);

        // Create the object node to host the Rx filter method
        ObjectNode methodObjNode = mapper.createObjectNode();
        methodObjNode.put(PARAM_NIC_RX_METHOD, PARAM_NIC_RX_FILTER_FD);
        scsObjNode.put(PARAM_NIC_RX_FILTER, methodObjNode);

        // Map each core to an array of rule IDs and rules
        Map<Long, ArrayNode> cpuObjSet =
            new ConcurrentHashMap<Long, ArrayNode>();

        String nic = null;
        Iterator<FlowRule> it = rules.iterator();

        while (it.hasNext()) {
            NicFlowRule nicRule = (NicFlowRule) it.next();
            if (nicRule.isFullWildcard() && (rulesToInstall > 1)) {
                log.warn("Skipping wildcard flow rule: {}", nicRule);
                it.remove();
                continue;
            }

            long coreIndex = nicRule.cpuCoreIndex();

            // Keep the ID of the target NIC
            if (nic == null) {
                nic = findNicInterfaceWithPort(deviceId, nicRule.interfaceNumber());
                checkArgument(!Strings.isNullOrEmpty(nic),
                    "Attempted to install flow rules in an invalid NIC");
            }

            // Create a JSON array for this CPU core
            if (!cpuObjSet.containsKey(coreIndex)) {
                cpuObjSet.put(coreIndex, mapper.createArrayNode());
            }

            // The array of rules that corresponds to this CPU core
            ArrayNode ruleArrayNode = cpuObjSet.get(coreIndex);

            // Each rule has an ID and a content
            ObjectNode ruleNode = mapper.createObjectNode();
            ruleNode.put(PARAM_ID, nicRule.id().value());
            ruleNode.put(PARAM_RULE_CONTENT, nicRule.ruleBody());

            ruleArrayNode.add(ruleNode);
        }

        if (rules.size() == 0) {
            log.error("Failed to install {} NIC flow rules in device {}", rulesToInstall, deviceId);
            return Collections.EMPTY_LIST;
        }

        ObjectNode nicObjNode = mapper.createObjectNode();
        nicObjNode.put(PARAM_NAME, nic);

        ArrayNode cpusArrayNode = nicObjNode.putArray(PARAM_CPUS);

        // Convert the map of CPU cores to arrays of rules to JSON
        for (Map.Entry<Long, ArrayNode> entry : cpuObjSet.entrySet()) {
            long coreIndex = entry.getKey();
            ArrayNode ruleArrayNode = entry.getValue();

            ObjectNode cpuObjNode = mapper.createObjectNode();
            cpuObjNode.put(PARAM_ID, coreIndex);
            cpuObjNode.putArray(PARAM_RULES).addAll(ruleArrayNode);

            cpusArrayNode.add(cpuObjNode);
        }

        scsObjNode.putArray(PARAM_NICS).add(nicObjNode);

        // Create the object node to host all the data
        ObjectNode sendObjNode = mapper.createObjectNode();
        sendObjNode.putArray(PARAM_RULES).add(scsObjNode);

        // Post the NIC rules to the server
        int response = getController().post(
            deviceId, URL_RULE_MANAGEMENT,
            new ByteArrayInputStream(sendObjNode.toString().getBytes()), JSON);

        // Upon an error, return an empty set of rules
        if (!checkStatusCode(response)) {
            log.error("Failed to install {} NIC flow rules in device {}", rules.size(), deviceId);
            return Collections.EMPTY_LIST;
        }

        log.info("Successfully installed {}/{} NIC flow rules in device {}",
            rules.size(), rulesToInstall, deviceId);

        // .. or all of them
        return rules;
    }

    /**
     * Removes a batch of FlowRules from a server device
     * using a single REST command.
     *
     * @param deviceId target server device ID
     * @param ruleIds a batch of comma-separated NIC rule IDs to be removed
     * @return boolean removal status
     */
    private boolean removeNicFlowRuleBatch(DeviceId deviceId, String ruleIds) {
        int response = -1;
        long ruleCount = ruleIds.chars().filter(ch -> ch == ',').count() + 1;

        // Try to remove the rules, although server might be unreachable
        try {
            response = getController().delete(deviceId,
                URL_RULE_MANAGEMENT + SLASH + ruleIds, null, JSON);
        } catch (Exception ex) {
            log.error("Failed to remove NIC flow rule batch with {} rules from device {}", ruleCount, deviceId);
            return false;
        }

        if (!checkStatusCode(response)) {
            log.error("Failed to remove NIC flow rule batch with {} rules from device {}", ruleCount, deviceId);
            return false;
        }

        log.info("Successfully removed NIC flow rule batch with {} rules from device {}", ruleCount, deviceId);
        return true;
    }

    /**
     * Returns how many rules this driver can delete at once.
     *
     * @param deviceId the device's ID to delete rules from
     * @return rule deletion batch size
     */
    private int getRuleDeleteBatchSizeProperty(DeviceId deviceId) {
        Driver driver = getHandler().get(DriverService.class).getDriver(deviceId);
        return Integer.parseInt(driver.getProperty(RULE_DELETE_BATCH_SIZE_PROPERTY));
    }

}
