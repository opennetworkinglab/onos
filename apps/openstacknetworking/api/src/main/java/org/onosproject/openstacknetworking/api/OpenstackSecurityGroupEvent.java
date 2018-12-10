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

import org.onosproject.event.AbstractEvent;
import org.openstack4j.model.network.SecurityGroup;
import org.openstack4j.model.network.SecurityGroupRule;

/**
 * Describes OpenStack security group event.
 */
public class OpenstackSecurityGroupEvent
        extends AbstractEvent<OpenstackSecurityGroupEvent.Type, SecurityGroup> {

    private SecurityGroupRule sgRule;

    public enum Type {
        /**
         * Signifies that a new OpenStack security group is created.
         */
        OPENSTACK_SECURITY_GROUP_CREATED,

        /**
         * Signifies that the OpenStack security group is removed.
         */
        OPENSTACK_SECURITY_GROUP_REMOVED,

        /**
         * Signifies that a new OpenStack security group rule is created.
         */
        OPENSTACK_SECURITY_GROUP_RULE_CREATED,

        /**
         * Signifies that the OpenStack security group rule is removed.
         */
        OPENSTACK_SECURITY_GROUP_RULE_REMOVED,
    }

    /**
     * SecurityGroupEvent constructor.
     *
     * @param type SecurityGroupEvent type
     * @param sg SecurityGroup object
     */
    public OpenstackSecurityGroupEvent(OpenstackSecurityGroupEvent.Type type,
                                       SecurityGroup sg) {
        super(type, sg);
    }

    /**
     * SecurityGroupEvent constructor.
     *
     * @param type SecurityGroupEvent type
     * @param sg security group
     * @param sgRule SecurityGroup object
     */
    public OpenstackSecurityGroupEvent(OpenstackSecurityGroupEvent.Type type,
                                       SecurityGroup sg,
                                       SecurityGroupRule sgRule) {
        super(type, sg);
        this.sgRule = sgRule;
    }

    /**
     * Returns security group rule.
     *
     * @return SecurityGroupRule
     */
    public SecurityGroupRule securityGroupRule() {
        return this.sgRule;
    }
}
