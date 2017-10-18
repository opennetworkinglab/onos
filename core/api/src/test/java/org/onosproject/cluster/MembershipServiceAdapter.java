/*
 * Copyright 2017-present Open Networking Foundation
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

import java.util.Collection;
import java.util.Set;

import org.onosproject.core.Version;

/**
 * Membership service adapter.
 */
public class MembershipServiceAdapter implements MembershipService {
    @Override
    public Member getLocalMember() {
        return null;
    }

    @Override
    public MembershipGroup getLocalGroup() {
        return null;
    }

    @Override
    public Set<Member> getMembers() {
        return null;
    }

    @Override
    public Collection<MembershipGroup> getGroups() {
        return null;
    }

    @Override
    public MembershipGroup getGroup(Version version) {
        return null;
    }

    @Override
    public Set<Member> getMembers(Version version) {
        return null;
    }

    @Override
    public Member getMember(NodeId nodeId) {
        return null;
    }
}
