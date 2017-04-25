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
package org.onosproject.driver.optical.flowrule;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.net.flow.FlowId;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple implementation of a local flow rule cache that stores the flow ID and priority.
 *
 * Use this if you have a device that does not allow you to store these fields.
 *
 * WARNING: Be aware that this implementation makes no attempt to use a distributed store
 * for the cache, so do not rely on it to support fail-over in multi-instance deployments.
 * If the instance which holds the cache goes down, you *will* be in trouble.
 */
@Component(immediate = true, enabled = true)
@Service
public class DefaultCrossConnectCache implements CrossConnectCache {
    private final Map<Integer, Pair<FlowId, Integer>> cache = new HashMap<>();

    @Override
    public Pair<FlowId, Integer> get(int hash) {
        return cache.get(hash);
    }

    @Override
    public void set(int hash, FlowId flowId, int priority) {
        cache.put(hash, Pair.of(flowId, priority));
    }

    @Override
    public void remove(int hash) {
        cache.remove(hash);
    }
}
