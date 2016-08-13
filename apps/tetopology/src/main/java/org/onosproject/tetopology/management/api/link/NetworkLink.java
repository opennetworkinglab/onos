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

import java.util.List;

import org.onosproject.tetopology.management.api.KeyId;
import org.onosproject.tetopology.management.api.TeTopologyEventSubject;
import org.onosproject.tetopology.management.api.node.TerminationPointKey;

/**
 * Abstraction of a base network link.
 */
public interface NetworkLink extends TeTopologyEventSubject {

    /**
     * Returns the link id.
     *
     * @return link identifier
     */
    KeyId linkId();

    /**
     * Returns the link source termination point.
     *
     * @return source link termination point id
     */
    TerminationPointKey getSource();

    /**
     * Returns the link destination termination point.
     *
     * @return destination link termination point id
     */
    TerminationPointKey getDestination();

    /**
     * Returns the supporting link ids.
     *
     * @return list of the ids of the supporting links
     */
    List<NetworkLinkKey> getSupportingLinkIds();

    /**
     * Returns the link te extension.
     *
     * @return TE link attributes
     */
    TeLink getTe();

}
