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
 * Service for obtaining information about the individual members of the controller cluster.
 */
public interface MembershipService {

    /**
     * Returns the local member.
     *
     * @return local member
     */
    Member getLocalMember();

    /**
     * Returns the group associated with the local member.
     *
     * @return the group associated with the local member
     */
    MembershipGroup getLocalGroup();

    /**
     * Returns the set of current cluster members in the local group.
     *
     * @return set of cluster members in the local group
     */
    Set<Member> getMembers();

    /**
     * Returns the set of membership groups in the cluster.
     *
     * @return the set of membership groups in the cluster
     */
    Collection<MembershipGroup> getGroups();

    /**
     * Returns the membership group for the given version.
     *
     * @param version the version for which to return the membership group
     * @return the membership group for the given version
     */
    MembershipGroup getGroup(Version version);

    /**
     * Returns the set of members in the given version.
     *
     * @param version the version for which to return the set of members
     * @return the set of members for the given version
     */
    Set<Member> getMembers(Version version);

    /**
     * Returns the specified controller node.
     *
     * @param nodeId controller node identifier
     * @return controller node
     */
    Member getMember(NodeId nodeId);

}
