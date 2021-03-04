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

import java.util.Set;

/**
 * Representation of security group.
 */
public interface KubevirtSecurityGroup {

    /**
     * Returns the security group identifier.
     *
     * @return security group identifier
     */
    String id();

    /**
     * Returns the security group name.
     *
     * @return security group name
     */
    String name();

    /**
     * Returns the description.
     *
     * @return description
     */
    String description();

    /**
     * Returns rules associated with this security group.
     *
     * @return security group rules
     */
    Set<KubevirtSecurityGroupRule> rules();

    /**
     * Returns new kubevirt security group instance with given rules.
     *
     * @param updatedRules set of updated security group rules
     * @return updated kubevirt security group
     */
    KubevirtSecurityGroup updateRules(Set<KubevirtSecurityGroupRule> updatedRules);

    /**
     * A default builder interface.
     */
    interface Builder {
        /**
         * Builds an immutable security group instance.
         *
         * @return kubevirt security group
         */
        KubevirtSecurityGroup build();

        /**
         * Returns kubevirt security group builder with supplied identifier.
         *
         * @param id security group identifier
         * @return security group builder
         */
        Builder id(String id);

        /**
         * Returns kubevirt security group builder with supplied name.
         *
         * @param name security group name
         * @return security group builder
         */
        Builder name(String name);

        /**
         * Returns kubevirt security group builder with supplied description.
         *
         * @param description security group description
         * @return security group builder
         */
        Builder description(String description);

        /**
         * Returns kubevirt security group builder with supplied security group rules.
         *
         * @param rules security group rules
         * @return security group builder
         */
        Builder rules(Set<KubevirtSecurityGroupRule> rules);
    }
}
