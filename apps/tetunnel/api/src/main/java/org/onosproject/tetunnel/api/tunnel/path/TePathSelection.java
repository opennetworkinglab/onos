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
 * Representation of a TE tunnel path selection attributes.
 */
public interface TePathSelection {

    /**
     * Returns key of corresponding TE topology of the TE path.
     *
     * @return key of corresponding TE topology
     */
    TeTopologyKey teTopologyKey();

    /**
     * Returns cost limit of the TE path.
     *
     * @return cost limit
     */
    long costLimit();

    /**
     * Returns hop limit of the TE path.
     *
     * @return hop limit
     */
    short hopLimit();

    //TODO add more attributes here.
}
