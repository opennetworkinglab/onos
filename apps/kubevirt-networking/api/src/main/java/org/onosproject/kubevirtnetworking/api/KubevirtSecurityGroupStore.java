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
package org.onosproject.kubevirtnetworking.api;

import org.onosproject.store.Store;

import java.util.Set;

/**
 * Manages inventory of kubevirt security group states; not intended for direct use.
 */
public interface KubevirtSecurityGroupStore
        extends Store<KubevirtSecurityGroupEvent, KubevirtSecurityGroupStoreDelegate> {

    /**
     * Creates a security group.
     *
     * @param sg security group
     */
    void createSecurityGroup(KubevirtSecurityGroup sg);

    /**
     * Updates the security group with the security group ID with the security group object.
     *
     * @param sg new SecurityGroup object
     */
    void updateSecurityGroup(KubevirtSecurityGroup sg);

    /**
     * Removes the security group with the security group ID.
     *
     * @param sgId security group Id
     * @return SecurityGroup object removed
     */
    KubevirtSecurityGroup removeSecurityGroup(String sgId);

    /**
     * Returns the security group with the security group ID.
     *
     * @param sgId security group ID
     * @return Security Group
     */
    KubevirtSecurityGroup securityGroup(String sgId);

    /**
     * Returns all security groups.
     *
     * @return set of security groups
     */
    Set<KubevirtSecurityGroup> securityGroups();

    /**
     * Clears the security group store.
     */
    void clear();
}
