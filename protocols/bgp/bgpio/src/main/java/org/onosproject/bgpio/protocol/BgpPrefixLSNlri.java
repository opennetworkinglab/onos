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
package org.onosproject.bgpio.protocol;

import java.util.List;

import org.onosproject.bgpio.types.BgpValueType;
import org.onosproject.bgpio.protocol.linkstate.NodeDescriptors;

/**
 * Abstraction of an entity providing BGP-LS Prefix NLRI.
 */
public interface BgpPrefixLSNlri extends BgpLSNlri {
    /**
     * Returns local node descriptors.
     *
     * @return local node descriptors
     */
    NodeDescriptors getLocalNodeDescriptors();

    /**
     * Returns list of Prefix descriptor.
     *
     * @return list of Prefix descriptor
     */
    List<BgpValueType> getPrefixdescriptor();
}
