/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.cluster;

import java.util.Map;

/**
 * Interface for administratively manipulating leadership assignments.
 */
public interface LeadershipAdminService {

    /**
     * Attempts to assign leadership for a topic to a specified node.
     * @param topic leadership topic
     * @param nodeId identifier of the node to be made leader
     * @return true is the transfer was successfully executed. This method returns {@code false}
     * if {@code nodeId} is not one of the candidates for for the topic.
     */
    boolean transferLeadership(String topic, NodeId nodeId);

    /**
     * Make a node to be the next leader by promoting it to top of candidate list.
     * @param topic leadership topic
     * @param nodeId identifier of node to be next leader
     * @return {@code true} if nodeId is now the top candidate. This method returns {@code false}
     * if {@code nodeId} is not one of the candidates for for the topic.
     */
    boolean promoteToTopOfCandidateList(String topic, NodeId nodeId);

    /**
     * Removes all active leadership registrations for a given node.
     * <p>
     * This method will also evict the node from leaderships that it currently owns.
     * @param nodeId node identifier
     */
    void unregister(NodeId nodeId);

    /**
     * Returns the current leader board.
     *
     * @return mapping from topic to leadership info.
     */
    Map<String, Leadership> getLeaderBoard();

    /**
     * Attempts to demote a node to the bottom of the candidate list. It is not allowed
     * to demote the current leader
     *
     * @param topic leadership topic
     * @param nodeId identifier of node to be demoted
     * @return {@code true} if nodeId is now the bottom candidate. This method returns {@code false}
     * if {@code nodeId} is not one of the candidates for the topic or if it is the leader.
     */
    boolean demote(String topic, NodeId nodeId);
}
