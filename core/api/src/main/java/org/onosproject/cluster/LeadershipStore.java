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

import org.onosproject.store.Store;

/**
 * Store interface for managing {@link LeadershipService} state.
 */
public interface LeadershipStore extends Store<LeadershipEvent, LeadershipStoreDelegate> {

    /**
     * Adds registration for the local instance to be part of the leadership contest for topic.
     *
     * @param topic leadership topic
     * @return Updated leadership after operation is completed
     */
    Leadership addRegistration(String topic);

    /**
     * Unregisters the local instance from leadership contest for topic.
     *
     * @param topic leadership topic
     */
    void removeRegistration(String topic);

    /**
     * Unregisters an instance from all leadership contests.
     *
     * @param nodeId node identifier
     */
    void removeRegistration(NodeId nodeId);

    /**
     * Updates state so that given node is leader for a topic.
     *
     * @param topic leadership topic
     * @param toNodeId identifier of the desired leader
     * @return {@code true} if the transfer succeeded; {@code false} otherwise.
     * This method can return {@code false} if the node is not registered for the topic
     */
    boolean moveLeadership(String topic, NodeId toNodeId);

    /**
     * Attempts to make a node the top candidate.
     *
     * @param topic leadership topic
     * @param nodeId node identifier
     * @return {@code true} if the specified node is now the top candidate.
     * This method will return {@code false} if the node is not registered for the topic
     */
    boolean makeTopCandidate(String topic, NodeId nodeId);

    /**
     * Returns the current leadership for topic.
     *
     * @param topic leadership topic
     * @return current leadership
     */
    Leadership getLeadership(String topic);

    /**
     * Return current leadership for all topics.
     *
     * @return topic to leadership mapping
     */
    Map<String, Leadership> getLeaderships();

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