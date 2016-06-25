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

package org.onosproject.net.statistic;

import org.onosproject.core.ApplicationId;
import org.onosproject.core.GroupId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.flow.FlowRule;

import java.util.Optional;

/**
 * Test adapter for statistics service.
 */
public class StatisticServiceAdapter implements StatisticService {
    @Override
    public Load load(Link link) {
        return null;
    }

    @Override
    public Load load(ConnectPoint connectPoint) {
        return null;
    }

    @Override
    public Link max(Path path) {
        return null;
    }

    @Override
    public Link min(Path path) {
        return null;
    }

    @Override
    public FlowRule highestHitter(ConnectPoint connectPoint) {
        return null;
    }

    @Override
    public Load load(Link link, ApplicationId appId, Optional<GroupId> groupId) {
        return null;
    }
}
