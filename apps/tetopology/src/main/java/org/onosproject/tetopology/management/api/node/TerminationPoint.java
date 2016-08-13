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
package org.onosproject.tetopology.management.api.node;

import java.util.List;

import org.onosproject.tetopology.management.api.KeyId;
import org.onosproject.tetopology.management.api.TeTopologyEventSubject;

/**
 * Abstraction of a termination point.
 */
public interface TerminationPoint extends TeTopologyEventSubject {

    /**
     * Returns the termination point id.
     *
     * @return termination point id
     */
    KeyId id();

    /**
     * Returns list of supporting termination point ids.
     *
     * @return the supportingTpIds
     */
    List<TerminationPointKey> getSupportingTpIds();

    /**
     * Returns TE attributes for this termination point.
     *
     * @return the te attribute
     */
    TeTerminationPoint getTe();

}
