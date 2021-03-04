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

import org.onosproject.event.ListenerService;

import java.util.Set;

/**
 * Service for interfacing kubevirt SecurityGroup events and SecurityGroup store.
 */
public interface KubevirtSecurityGroupService
        extends ListenerService<KubevirtSecurityGroupEvent, KubevirtSecurityGroupListener> {

    /**
     * Returns all security groups.
     *
     * @return set of security group
     */
    Set<KubevirtSecurityGroup> securityGroups();

    /**
     * Returns the security group for the sgId.
     *
     * @param sgId security group Id
     * @return security group
     */
    KubevirtSecurityGroup securityGroup(String sgId);

    /**
     * Returns the security group rule for the sgId.
     *
     * @param sgrId security group rule Id
     * @return security group rule
     */
    KubevirtSecurityGroupRule securityGroupRule(String sgrId);

    /**
     * Returns whether security group is enabled or not.
     *
     * @return true security group is enabled, false otherwise
     */
    boolean isSecurityGroupEnabled();

    /**
     * Sets security group enable option.
     *
     * @param option security group enable option
     */
    void setSecurityGroupEnabled(boolean option);
}
