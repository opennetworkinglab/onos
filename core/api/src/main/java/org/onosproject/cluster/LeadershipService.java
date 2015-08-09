/*
 * Copyright 2014-2015 Open Networking Laboratory
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

import org.onosproject.event.ListenerService;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Service for leader election.
 * Leadership contests are organized around topics. A instance can join the
 * leadership race for a topic or withdraw from a race it has previously joined.
 * Listeners can be added to receive notifications asynchronously for various
 * leadership contests.
 */
public interface LeadershipService
    extends ListenerService<LeadershipEvent, LeadershipEventListener> {

    /**
     * Returns the current leader for the topic.
     *
     * @param path topic
     * @return nodeId of the leader, null if so such topic exists.
     */
    NodeId getLeader(String path);

    /**
     * Returns the current leadership info for the topic.
     *
     * @param path topic
     * @return leadership info or null if so such topic exists.
     */
    Leadership getLeadership(String path);

    /**
     * Returns the set of topics owned by the specified node.
     *
     * @param nodeId node Id.
     * @return set of topics for which this node is the current leader.
     */
    Set<String> ownedTopics(NodeId nodeId);

    /**
     * Joins the leadership contest.
     *
     * @param path topic for which this controller node wishes to be a leader
     * @return {@code Leadership} future
     */
    CompletableFuture<Leadership> runForLeadership(String path);

    /**
     * Withdraws from a leadership contest.
     *
     * @param path topic for which this controller node no longer wishes to be a leader
     * @return future that is successfully completed when withdraw is done
     */
    CompletableFuture<Void> withdraw(String path);

    /**
     * If the local nodeId is the leader for specified topic, this method causes it to
     * step down temporarily from leadership.
     * <p>
     * The node will continue to be in contention for leadership and can
     * potentially become the leader again if and when it becomes the highest
     * priority candidate
     * <p>
     * If the local nodeId is not the leader, this method will make no changes and
     * simply return false.
     *
     * @param path topic for which this controller node should give up leadership
     * @return true if this node stepped down from leadership, false otherwise
     */
    boolean stepdown(String path);

    /**
     * Moves the specified nodeId to the top of the candidates list for the topic.
     * <p>
     * If the node is not a candidate for this topic, this method will be a noop.
     *
     * @param path leadership topic
     * @param nodeId nodeId to make the top candidate
     * @return true if nodeId is now the top candidate, false otherwise
     */
    boolean makeTopCandidate(String path, NodeId nodeId);

    /**
     * Returns the current leader board.
     *
     * @return mapping from topic to leadership info.
     */
    Map<String, Leadership> getLeaderBoard();

    /**
     * Returns the candidates for all known topics.
     *
     * @return A mapping from topics to corresponding list of candidates.
     */
    Map<String, List<NodeId>> getCandidates();

    /**
     * Returns the candidates for a given topic.
     *
     * @param path topic
     * @return A lists of NodeIds, which may be empty.
     */
    List<NodeId> getCandidates(String path);

}
