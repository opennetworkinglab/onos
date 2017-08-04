/*
 * Copyright 2017-present Open Networking Foundation
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
import org.onosproject.net.flow.FlowId;

/**
 * Simple interface to cache flow ID and priority of cross connect flows.
 */
public interface CrossConnectCache {
    /**
     * Returns the flow ID and priority corresponding to the flow hash.
     *
     * @param hash flow hash
     * @return flow ID and priority, null if not in cache
     */
    Pair<FlowId, Integer> get(int hash);

    /**
     * Stores the flow ID and priority corresponding to the flow hash.
     *
     * @param hash flow hash
     * @param flowId flow ID
     * @param priority flow priority
     */
    void set(int hash, FlowId flowId, int priority);

    /**
     * Removes the given hash from the cache.
     *
     * @param hash flow hash
     */
    void remove(int hash);
}
