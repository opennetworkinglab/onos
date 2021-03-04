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

import org.onosproject.event.AbstractEvent;

/**
 * Describes kubevirt security group event.
 */
public class KubevirtSecurityGroupEvent
        extends AbstractEvent<KubevirtSecurityGroupEvent.Type, KubevirtSecurityGroup> {

    private KubevirtSecurityGroupRule sgRule;

    /**
     * SecurityGroupEvent constructor.
     *
     * @param type SecurityGroupEvent type
     * @param sg SecurityGroup object
     */
    public KubevirtSecurityGroupEvent(Type type, KubevirtSecurityGroup sg) {
        super(type, sg);
    }

    /**
     * SecurityGroupEvent constructor.
     *
     * @param type SecurityGroupEvent type
     * @param sg SecurityGroup object
     * @param sgRule SecurityGroupRule object
     */
    public KubevirtSecurityGroupEvent(Type type, KubevirtSecurityGroup sg,
                                      KubevirtSecurityGroupRule sgRule) {
        super(type, sg);
        this.sgRule = sgRule;
    }

    /**
     * Returns security group rule.
     *
     * @return SecurityGroupRule
     */
    public KubevirtSecurityGroupRule rule() {
        return this.sgRule;
    }

    public enum Type {
        /**
         * Signifies that a new kubevirt security group is created.
         */
        KUBEVIRT_SECURITY_GROUP_CREATED,

        /**
         * Signifies that the kubevirt security group is removed.
         */
        KUBEVIRT_SECURITY_GROUP_REMOVED,

        /**
         * Signifies that a new kubevirt security group rule is created.
         */
        KUBEVIRT_SECURITY_GROUP_RULE_CREATED,

        /**
         * Signifies that the kubevirt security group rule is removed.
         */
        KUBEVIRT_SECURITY_GROUP_RULE_REMOVED,
    }
}
