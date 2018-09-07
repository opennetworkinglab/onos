/*
 * Copyright 2015-present Open Networking Foundation
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

import java.util.List;
import java.util.Set;

/**
 * Test adapter for leadership service.
 */
public class LeadershipServiceAdapter implements LeadershipService {

    @Override
    public NodeId getLeader(String path) {
        return null;
    }

    @Override
    public Leadership getLeadership(String path) {
        return null;
    }

    @Override
    public Set<String> ownedTopics(NodeId nodeId) {
        return null;
    }

    @Override
    public Leadership runForLeadership(String path) {
        return null;
    }

    @Override
    public void withdraw(String path) {
    }

    @Override
    public void addListener(LeadershipEventListener listener) {

    }

    @Override
    public void removeListener(LeadershipEventListener listener) {

    }

    @Override
    public List<NodeId> getCandidates(String path) {
        return null;
    }
}