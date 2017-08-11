/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onosproject.net.statistic;

import org.onosproject.core.ApplicationId;
import org.onosproject.core.GroupId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.flow.FlowRule;

import java.util.Optional;

/**
 * Service for obtaining statistic information about link in the system.
 * Statistics are obtained from the FlowRuleService in order to minimize the
 * amount of hammering occurring at the dataplane.
 */
public interface StatisticService {

    /**
     * Obtain the load for a the ingress to the given link.
     *
     * @param link the link to query.
     * @return a {@link org.onosproject.net.statistic.Load Load}
     */
    Load load(Link link);

    /**
     * Obtain the load for the given port.
     *
     * @param connectPoint the port to query
     * @return a {@link org.onosproject.net.statistic.Load}
     */
    Load load(ConnectPoint connectPoint);

    /**
     * Find the most loaded link along a path.
     *
     * @param path the path to search in
     * @return the most loaded {@link org.onosproject.net.Link}.
     */
    Link max(Path path);

    /**
     * Find the least loaded link along a path.
     *
     * @param path the path to search in
     * @return the least loaded {@link org.onosproject.net.Link}.
     */
    Link min(Path path);

    /**
     * Returns the highest hitter (a flow rule) for a given port, ie. the
     * flow rule which is generating the most load.
     *
     * @param connectPoint the port
     * @return the flow rule
     */
    FlowRule highestHitter(ConnectPoint connectPoint);

    /**
     * Obtain the load for a the ingress to the given link used by
     * the specified application ID and group ID.
     *
     * @param link    link to query
     * @param appId   application ID to filter with
     * @param groupId group ID to filter with
     * @return {@link Load Load}
     */
    Load load(Link link, ApplicationId appId, Optional<GroupId> groupId);
}
