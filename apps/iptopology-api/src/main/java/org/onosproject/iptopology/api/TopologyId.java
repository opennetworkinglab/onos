/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.iptopology.api;

import org.onlab.util.Identifier;

/**
 * Represents Multi-Topology IDs for a network link, node or prefix.
 */
public class TopologyId extends Identifier<Short> {
    /**
     * Constructor to initialize its parameter.
     *
     * @param topologyId topology id for node/link/prefix
     */
    public TopologyId(short topologyId) {
        super(topologyId);
    }

    /**
     * Obtains the topology ID.
     *
     * @return  topology ID
     */
    public short topologyId() {
        return identifier;
    }
}