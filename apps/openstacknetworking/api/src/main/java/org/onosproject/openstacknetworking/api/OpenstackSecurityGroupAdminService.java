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
package org.onosproject.openstacknetworking.api;

import org.openstack4j.model.network.SecurityGroup;
import org.openstack4j.model.network.SecurityGroupRule;

/**
 * Service for administering the inventory of Openstack security group.
 */
public interface OpenstackSecurityGroupAdminService
        extends OpenstackSecurityGroupService {
    /**
     * Creates a security group.
     *
     * @param sg security group
     */
    void createSecurityGroup(SecurityGroup sg);

    /**
     * Updates the security group.
     *
     * @param sg security group
     */
    void updateSecurityGroup(SecurityGroup sg);

    /**
     * Removes the security group.
     *
     * @param sgId security group ID
     */
    void removeSecurityGroup(String sgId);

    /**
     * Creates a security group rule.
     *
     * @param sgRule security group rule
     */
    void createSecurityGroupRule(SecurityGroupRule sgRule);

    /**
     * Removes the security group rule.
     *
     * @param sgRuleId security group rule ID
     */
    void removeSecurityGroupRule(String sgRuleId);

    /**
     * Removes the existing security groups.
     */
    void clear();
}
