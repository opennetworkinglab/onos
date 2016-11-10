/*
 * Copyright 2016 Open Networking Laboratory
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
package org.onosproject.tetopology.management.api.link;

import org.onosproject.tetopology.management.api.KeyId;
import org.onosproject.tetopology.management.api.node.NodeTpKey;

import java.util.List;

/**
 * Abstraction of a base network link.
 */
public interface NetworkLink {

    /**
     * Returns the link identifier.
     *
     * @return link identifier
     */
    KeyId linkId();

    /**
     * Returns the link source termination point.
     *
     * @return source link termination point identifier
     */
    NodeTpKey source();

    /**
     * Returns the link destination termination point.
     *
     * @return destination link termination point id
     */
    NodeTpKey destination();

    /**
     * Returns the supporting link identifiers.
     *
     * @return list of the ids of the supporting links
     */
    List<NetworkLinkKey> supportingLinkIds();

    /**
     * Returns the link TE extension.
     *
     * @return TE link attributes
     */
    TeLink teLink();
}
