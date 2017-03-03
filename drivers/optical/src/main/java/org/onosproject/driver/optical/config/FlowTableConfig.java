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

import java.util.Set;

import org.onosproject.codec.JsonCodec;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.BaseConfig;
import org.onosproject.net.flow.FlowRule;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;

/**
 * Config to store FlowTable.
 *
 * Used by ConfigFlowRuleProgrammable
 */
public class FlowTableConfig extends BaseConfig<DeviceId> {

    /**
     * Configuration key for {@link FlowTableConfig}.
     */
    public static final String CONFIG_KEY = "flowtable";

    public static final String ENTRIES = "entries";


    @Override
    public boolean isValid() {
        return hasField(ENTRIES);
    }


    public Set<FlowRule> flowtable() {
        JsonNode ents = object.path(ENTRIES);
        if (!ents.isArray()) {

            return ImmutableSet.of();
        }
        ArrayNode entries = (ArrayNode) ents;

        Builder<FlowRule> builder = ImmutableSet.builder();
        entries.forEach(entry -> builder.add(decode(entry, FlowRule.class)));
        return builder.build();
    }

    public FlowTableConfig flowtable(Set<FlowRule> table) {
        JsonCodec<FlowRule> codec = codec(FlowRule.class);
        ArrayNode entries = codec.encode(table, this);
        object.set(ENTRIES, entries);
        return this;
    }

    /**
     * Create a {@link FlowTableConfig}.
     * <p>
     * Note: created instance needs to be initialized by #init(..) before using.
     */
    public FlowTableConfig() {
        super();
    }

    /**
     * Create a {@link FlowTableConfig} for specified Device.
     * <p>
     * Note: created instance is not bound to NetworkConfigService,
     * cannot use {@link #apply()}. Must be passed to the service
     * using NetworkConfigService#applyConfig
     *
     * @param did DeviceId
     */
    public FlowTableConfig(DeviceId did) {
        ObjectMapper mapper = new ObjectMapper();
        init(did, CONFIG_KEY, mapper.createObjectNode(), mapper, null);
    }
}
