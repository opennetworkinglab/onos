/*
 * Copyright 2021-present Open Networking Foundation
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

package org.onosproject.pipelines.fabric.impl.behaviour.upf;

import org.onlab.util.ImmutableByteSequence;

import java.util.Map;

/**
 * Stores state required for translation of UPF entities to pipeline-specific ones.
 */
public interface FabricUpfStore {
    /**
     * Clear all state associated with translation.
     */
    void reset();

    /**
     * Returns the farIdMap.
     *
     * @return the farIdMap.
     */
    Map<UpfRuleIdentifier, Integer> getFarIdMap();

    /**
     * Get a globally unique integer identifier for the FAR identified by the given (Session ID, Far
     * ID) pair.
     *
     * @param farIdPair a RuleIdentifier instance uniquely identifying the FAR
     * @return A globally unique integer identifier
     */
    int globalFarIdOf(UpfRuleIdentifier farIdPair);

    /**
     * Get a globally unique integer identifier for the FAR identified by the given (Session ID, Far
     * ID) pair.
     *
     * @param pfcpSessionId     The ID of the PFCP session that produced the FAR ID.
     * @param sessionLocalFarId The FAR ID.
     * @return A globally unique integer identifier
     */
    int globalFarIdOf(ImmutableByteSequence pfcpSessionId, int sessionLocalFarId);

    /**
     * Get the corresponding PFCP session ID and session-local FAR ID from a globally unique FAR ID,
     * or return null if no such mapping is found.
     *
     * @param globalFarId globally unique FAR ID
     * @return the corresponding PFCP session ID and session-local FAR ID, as a RuleIdentifier
     */
    UpfRuleIdentifier localFarIdOf(int globalFarId);

    /**
     * Get the corresponding queue Id from scheduling priority.
     *
     * @param schedulingPriority QCI scheduling priority
     * @return the corresponding queue ID
     */
    String queueIdOf(int schedulingPriority);

    /**
     * Get the corresponding queue Id from scheduling priority.
     *
     * @param queueId Tofino queue Id
     * @return the corresponding scheduling priroity
     */
    String schedulingPriorityOf(int queueId);
}
