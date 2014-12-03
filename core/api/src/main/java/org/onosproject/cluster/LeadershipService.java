/*
 * Copyright 2014 Open Networking Laboratory
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
 * Service for leader election.
 * Leadership contests are organized around topics. A instance can join the
 * leadership race for a topic or withdraw from a race it has previously joined.
 * Listeners can be added to receive notifications asynchronously for various
 * leadership contests.
 */
public interface LeadershipService {

    /**
     * Gets the most recent leader for the topic.
     * @param path topic
     * @return nodeId of the leader, null if so such topic exists.
     */
    NodeId getLeader(String path);

    /**
     * Joins the leadership contest.
     * @param path topic for which this controller node wishes to be a leader.
     */
    void runForLeadership(String path);

    /**
     * Withdraws from a leadership contest.
     * @param path topic for which this controller node no longer wishes to be a leader.
     */
    void withdraw(String path);

    Map<String, Leadership> getLeaderBoard();

    /**
     * Registers a event listener to be notified of leadership events.
     * @param listener listener that will asynchronously notified of leadership events.
     */
    void addListener(LeadershipEventListener listener);

    /**
     * Unregisters a event listener for leadership events.
     * @param listener listener to be removed.
     */
    void removeListener(LeadershipEventListener listener);
}
