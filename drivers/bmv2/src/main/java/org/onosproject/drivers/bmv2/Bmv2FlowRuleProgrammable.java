/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.drivers.bmv2;

import com.eclipsesource.json.Json;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.onosproject.bmv2.api.model.Bmv2Model;
import org.onosproject.bmv2.api.runtime.Bmv2Client;
import org.onosproject.bmv2.api.runtime.Bmv2MatchKey;
import org.onosproject.bmv2.api.runtime.Bmv2RuntimeException;
import org.onosproject.bmv2.api.runtime.Bmv2TableEntry;
import org.onosproject.bmv2.ctl.Bmv2ThriftClient;
import org.onosproject.drivers.bmv2.translators.Bmv2DefaultFlowRuleTranslator;
import org.onosproject.drivers.bmv2.translators.Bmv2FlowRuleTranslator;
import org.onosproject.drivers.bmv2.translators.Bmv2FlowRuleTranslatorException;
import org.onosproject.drivers.bmv2.translators.Bmv2SimpleTranslatorConfig;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleProgrammable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.onosproject.net.flow.FlowEntry.FlowEntryState.ADDED;

/**
 * Flow rule programmable device behaviour implementation for BMv2.
 */
public class Bmv2FlowRuleProgrammable extends AbstractHandlerBehaviour
        implements FlowRuleProgrammable {

    private static final Logger LOG =
            LoggerFactory.getLogger(Bmv2FlowRuleProgrammable.class);

    // There's no Bmv2 client method to poll flow entries from the device. Use a local store.
    // FIXME: this information should be distributed across instances of the cluster.
    private static final ConcurrentMap<Triple<DeviceId, String, Bmv2MatchKey>, Pair<Long, TimestampedFlowRule>>
            ENTRIES_MAP = Maps.newConcurrentMap();

    // Cache model objects instead of parsing the JSON each time.
    private static final LoadingCache<String, Bmv2Model> MODEL_CACHE = CacheBuilder.newBuilder()
            .expireAfterAccess(60, TimeUnit.SECONDS)
            .build(new CacheLoader<String, Bmv2Model>() {
                @Override
                public Bmv2Model load(String jsonString) throws Exception {
                    // Expensive call.
                    return Bmv2Model.parse(Json.parse(jsonString).asObject());
                }
            });

    @Override
    public Collection<FlowEntry> getFlowEntries() {

        DeviceId deviceId = handler().data().deviceId();

        Bmv2Client deviceClient;
        try {
            deviceClient = Bmv2ThriftClient.of(deviceId);
        } catch (Bmv2RuntimeException e) {
            LOG.error("Failed to connect to Bmv2 device", e);
            return Collections.emptyList();
        }

        Bmv2Model model = getTranslator(deviceId).config().model();

        List<FlowEntry> entryList = Lists.newArrayList();

        model.tables().forEach(table -> {
            // For each table declared in the model for this device, do:
            try {
                // Bmv2 doesn't support proper polling for table entries, but only a string based table dump.
                // The trick here is to first dump the entry ids currently installed in the device for a given table,
                // and then filter ENTRIES_MAP based on the retrieved values.
                Set<Long> installedEntryIds = Sets.newHashSet(deviceClient.getInstalledEntryIds(table.name()));
                ENTRIES_MAP.forEach((key, value) -> {
                    if (key.getLeft() == deviceId && key.getMiddle() == table.name()
                            && value != null) {
                        long entryId = value.getKey();
                        // Filter entries_map for this device and table.
                        if (installedEntryIds.contains(entryId)) {
                            // Entry is installed.
                            long bytes = 0L;
                            long packets = 0L;
                            if (table.hasCounters()) {
                                // Read counter values from device.
                                try {
                                    Pair<Long, Long> counterValue = deviceClient.readTableEntryCounter(table.name(),
                                                                                                       entryId);
                                    bytes = counterValue.getLeft();
                                    packets = counterValue.getRight();
                                } catch (Bmv2RuntimeException e) {
                                    LOG.warn("Unable to get counter values for entry {} of table {} of device {}: {}",
                                             entryId, table.name(), deviceId, e.toString());
                                }
                            }
                            TimestampedFlowRule tsRule = value.getRight();
                            FlowEntry entry = new DefaultFlowEntry(tsRule.rule(), ADDED,
                                                                   tsRule.lifeInSeconds(), packets, bytes);
                            entryList.add(entry);
                        } else {
                            // No such entry on device, can remove from local store.
                            ENTRIES_MAP.remove(key);
                        }
                    }
                });
            } catch (Bmv2RuntimeException e) {
                LOG.error("Unable to get flow entries for table {} of device {}: {}",
                          table.name(), deviceId, e.toString());
            }
        });

        return Collections.unmodifiableCollection(entryList);
    }

    @Override
    public Collection<FlowRule> applyFlowRules(Collection<FlowRule> rules) {

        return processFlowRules(rules, Operation.APPLY);
    }

    @Override
    public Collection<FlowRule> removeFlowRules(Collection<FlowRule> rules) {

        return processFlowRules(rules, Operation.REMOVE);
    }

    private Collection<FlowRule> processFlowRules(Collection<FlowRule> rules, Operation operation) {

        DeviceId deviceId = handler().data().deviceId();

        Bmv2Client deviceClient;
        try {
            deviceClient = Bmv2ThriftClient.of(deviceId);
        } catch (Bmv2RuntimeException e) {
            LOG.error("Failed to connect to Bmv2 device", e);
            return Collections.emptyList();
        }

        Bmv2FlowRuleTranslator translator = getTranslator(deviceId);

        List<FlowRule> processedFlowRules = Lists.newArrayList();

        for (FlowRule rule : rules) {

            Bmv2TableEntry bmv2Entry;

            try {
                bmv2Entry = translator.translate(rule);
            } catch (Bmv2FlowRuleTranslatorException e) {
                LOG.error("Unable to translate flow rule: {}", e.getMessage());
                continue;
            }

            String tableName = bmv2Entry.tableName();
            Triple<DeviceId, String, Bmv2MatchKey> entryKey = Triple.of(deviceId, tableName, bmv2Entry.matchKey());

            /*
            From here on threads are synchronized over entryKey, i.e. serialize operations
            over the same matchKey of a specific table and device.
             */
            ENTRIES_MAP.compute(entryKey, (key, value) -> {
                try {
                    if (operation == Operation.APPLY) {
                        // Apply entry
                        long entryId;
                        if (value != null) {
                            // Existing entry.
                            entryId = value.getKey();
                            try {
                                // Tentatively delete entry before re-adding.
                                // It might not exist on device due to inconsistencies.
                                deviceClient.deleteTableEntry(bmv2Entry.tableName(), entryId);
                                value = null;
                            } catch (Bmv2RuntimeException e) {
                                // Silently drop exception as we can probably fix this by re-adding the entry.
                            }
                        }
                        // Add entry.
                        entryId = deviceClient.addTableEntry(bmv2Entry);
                        value = Pair.of(entryId, new TimestampedFlowRule(rule));
                    } else {
                        // Remove entry
                        if (value == null) {
                            // Entry not found in map, how come?
                            LOG.debug("Trying to remove entry, but entry ID not found: " + entryKey);
                        } else {
                            deviceClient.deleteTableEntry(tableName, value.getKey());
                            value = null;
                        }
                    }
                    // If here, no exceptions... things went well :)
                    processedFlowRules.add(rule);
                } catch (Bmv2RuntimeException e) {
                    LOG.warn("Unable to {} flow rule: {}", operation.name().toLowerCase(), e.toString());
                }
                return value;
            });
        }

        return processedFlowRules;
    }

    /**
     * Gets the appropriate flow rule translator based on the device running configuration.
     *
     * @param deviceId a device id
     * @return a flow rule translator
     */
    private Bmv2FlowRuleTranslator getTranslator(DeviceId deviceId) {

        DeviceService deviceService = handler().get(DeviceService.class);
        if (deviceService == null) {
            LOG.error("Unable to get device service");
            return null;
        }

        Device device = deviceService.getDevice(deviceId);
        if (device == null) {
            LOG.error("Unable to get device {}", deviceId);
            return null;
        }

        String jsonString = device.annotations().value("bmv2JsonConfigValue");
        if (jsonString == null) {
            LOG.error("Unable to read bmv2 JSON config from device {}", deviceId);
            return null;
        }

        Bmv2Model model;
        try {
            model = MODEL_CACHE.get(jsonString);
        } catch (ExecutionException e) {
            LOG.error("Unable to parse bmv2 JSON config for device {}:", deviceId, e.getCause());
            return null;
        }

        // TODO: get translator config dynamically.
        // Now it's hardcoded, selection should be based on the device bmv2 model.
        Bmv2FlowRuleTranslator.TranslatorConfig translatorConfig = new Bmv2SimpleTranslatorConfig(model);
        return new Bmv2DefaultFlowRuleTranslator(translatorConfig);
    }

    private enum Operation {
        APPLY, REMOVE
    }

    private class TimestampedFlowRule {
        private final FlowRule rule;
        private final Date addedDate;

        public TimestampedFlowRule(FlowRule rule) {
            this.rule = rule;
            this.addedDate = new Date();
        }

        public FlowRule rule() {
            return rule;
        }

        public long lifeInSeconds() {
            return (new Date().getTime() - addedDate.getTime()) / 1000;
        }
    }
}