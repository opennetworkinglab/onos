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
 *
 * This work was partially supported by EC H2020 project METRO-HAUL (761727).
 */

package org.onosproject.drivers.odtn.impl;

import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.FlowId;
import org.onosproject.net.flow.FlowRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Stores a set of Rules for given open config based devices in order to properly report them to the store.
 */
public final class OpenConfigConnectionCache {
    private static final Logger log =
            LoggerFactory.getLogger(OpenConfigConnectionCache.class);

    private Map<DeviceId, Set<FlowRule>> mp = new HashMap<>();
    private Map<DeviceId, Set<FlowRule>> smp = Collections.synchronizedMap(mp);

    private static OpenConfigConnectionCache cache = null;

    //banning public contraction
    private OpenConfigConnectionCache() {
    }

    /**
     * Initializes the cache if not already present.
     * If present returns the existing one.
     *
     * @return single instance of cache
     */
    public static OpenConfigConnectionCache init() {
        if (cache == null) {
            cache = new OpenConfigConnectionCache();
        }
        return cache;
    }

    /**
     * Returns the number of rules stored for a given device.
     *
     * @param did the device
     * @return number of flows stored
     */
    public int size(DeviceId did) {
        synchronized (smp) {
            if (!smp.containsKey(did)) {
                return 0;
            }
            return smp.get(did).size();
        }
    }

    /**
     * Returns the flow with given Id for the specific device.
     *
     * @param did    device id
     * @param flowId flow id
     * @return the flow rule
     */
    public FlowRule get(DeviceId did, FlowId flowId) {
        synchronized (smp) {
            if (!smp.containsKey(did)) {
                return null;
            }
            Set<FlowRule> set = smp.get(did);
            return set.stream()
                    .filter(r -> r.id() == flowId)
                    .findFirst()
                    .orElse(null);
        }
    }

    /**
     * Returns all the flows for the specific device.
     *
     * @param did device id
     * @return Set of flow rules
     */
    public Set<FlowRule> get(DeviceId did) {
        synchronized (smp) {
            if (!smp.containsKey(did)) {
                return null;
            }
            return smp.get(did);
        }
    }

    /**
     * Add a flows for the specific device.
     *
     * @param did      device id
     * @param flowRule the flow rule
     */
    public void add(DeviceId did, FlowRule flowRule) {
        synchronized (smp) {
            Set<FlowRule> set;
            if (smp.containsKey(did)) {
                set = smp.get(did);
            } else {
                set = new HashSet<FlowRule>();
                log.warn("OpenConfigConnectionCache created for {}", did);
                smp.put(did, set);
            }
            set.add(flowRule);
        }
    }

    /**
     * Add a flows for the specific device.
     *
     * @param did      device id
     * @param flowRule the flow rule
     */
    public void remove(DeviceId did, FlowRule flowRule) {
        synchronized (smp) {
            if (!smp.containsKey(did)) {
                return;
            }
            Set<FlowRule> set = smp.get(did);
            set.removeIf(r2 -> r2.id() == flowRule.id());
        }
    }
}
