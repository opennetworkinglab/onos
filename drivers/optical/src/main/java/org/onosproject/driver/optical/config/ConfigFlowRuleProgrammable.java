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
package org.onosproject.driver.optical.config;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.onosproject.net.DeviceId;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleProgrammable;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableSet;

import org.onosproject.net.flow.FlowEntry.FlowEntryState;
import org.slf4j.Logger;

// TODO consider relocating
/**
 * {@link FlowRuleProgrammable} which pretends it accepted the requests.
 *
 * Can be useful, when you need to send totally different flow rules
 * down to the Device.
 */
@Beta
public class ConfigFlowRuleProgrammable
    extends AbstractHandlerBehaviour
    implements FlowRuleProgrammable {

    private static final Logger log = getLogger(ConfigFlowRuleProgrammable.class);


    @Override
    public Collection<FlowEntry> getFlowEntries() {
        Set<FlowRule> flowtable = getFlowTable(getFlowTableConfig());
        return flowtable.stream()
                .map(fr -> new DefaultFlowEntry(fr, FlowEntryState.ADDED))
                .collect(Collectors.toList());
    }

    @Override
    public Collection<FlowRule> applyFlowRules(Collection<FlowRule> rules) {
        log.trace("applyFlowRules: {}", rules);

        Optional<FlowTableConfig> config = createFlowTableConfig();
        Set<FlowRule> table = new LinkedHashSet<>(getFlowTable(config));
        table.addAll(rules);
        config.map(cfg -> cfg.flowtable(table))
              .ifPresent(cfg -> cfg.apply());
        log.trace("Updated flowtable: {}", table);
        return table;
    }

    @Override
    public Collection<FlowRule> removeFlowRules(Collection<FlowRule> rules) {
        log.trace("removeFlowRules: {}", rules);
        Optional<FlowTableConfig> config = getFlowTableConfig();
        Set<FlowRule> table = new LinkedHashSet<>(getFlowTable(config));
        table.removeAll(rules);
        config.map(cfg -> cfg.flowtable(table))
              .ifPresent(cfg -> cfg.apply());
        log.trace("Updated flowtable: {}", table);
        return table;
    }

    private Set<FlowRule> getFlowTable(Optional<FlowTableConfig> cfg) {
        Set<FlowRule> flowtable = cfg
                                    .map(FlowTableConfig::flowtable)
                                    .orElse(ImmutableSet.of());
        return flowtable;
    }

    private Optional<FlowTableConfig> createFlowTableConfig() {
        NetworkConfigService netcfg = handler().get(NetworkConfigService.class);
        DeviceId did = data().deviceId();
        return Optional.ofNullable(netcfg.addConfig(did, FlowTableConfig.class));
    }

    private Optional<FlowTableConfig> getFlowTableConfig() {
        NetworkConfigService netcfg = handler().get(NetworkConfigService.class);
        DeviceId did = data().deviceId();
        return Optional.ofNullable(netcfg.getConfig(did, FlowTableConfig.class));
    }

}
