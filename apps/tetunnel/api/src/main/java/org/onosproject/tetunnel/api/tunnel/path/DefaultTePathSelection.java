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

package org.onosproject.tetunnel.api.tunnel.path;

import org.onosproject.tetopology.management.api.TeTopologyKey;

/**
 * Default implementation of TE path selection.
 */
public class DefaultTePathSelection implements TePathSelection {

    private final TeTopologyKey teTopologyKey;
    private final long costLimit;
    private final short hopLimit;

    /**
     * Creates a default implementation of TE path selection with supplied
     * information.
     *
     * @param teTopologyKey key of corresponding TE topology
     * @param costLimit cost limit of the TE path
     * @param hopLimit hot limit of the TE path
     */
    public DefaultTePathSelection(TeTopologyKey teTopologyKey,
                                  long costLimit, short hopLimit) {
        this.teTopologyKey = teTopologyKey;
        this.costLimit = costLimit;
        this.hopLimit = hopLimit;
    }

    @Override
    public TeTopologyKey teTopologyKey() {
        return teTopologyKey;
    }

    @Override
    public long costLimit() {
        return costLimit;
    }

    @Override
    public short hopLimit() {
        return hopLimit;
    }
}
