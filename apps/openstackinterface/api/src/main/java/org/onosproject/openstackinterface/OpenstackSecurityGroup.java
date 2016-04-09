/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.openstackinterface;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * Represents Openstack Security Group information.
 */
public final class OpenstackSecurityGroup {

    private String description;
    private String id;
    private String name;
    private Collection<OpenstackSecurityGroupRule> rules;
    private String tenantId;

    private OpenstackSecurityGroup(String description, String id, String name,
                                   Collection<OpenstackSecurityGroupRule> rules,
                                   String tenantId) {
        this.description = description;
        this.id = id;
        this.name = name;
        this.tenantId = tenantId;
        this.rules = rules;
    }

    /**
     * Returns the description of the security group.
     *
     * @return description
     */
    public String description() {
        return this.description;
    }

    /**
     * Returns ID of the security group.
     *
     * @return ID
     */
    public String id() {
        return this.id;
    }

    /**
     * Returns the name of the security group.
     *
     * @return name
     */
    public String name() {
        return this.name;
    }

    /**
     * Returns the list of the security group rules.
     *
     * @return Collection of OpenstackSecurityGroupRule objects
     */
    public Collection<OpenstackSecurityGroupRule> rules() {
        return Collections.unmodifiableCollection(rules);
    }

    /**
     * Returns the Tenant ID.
     *
     * @return tenant ID
     */
    public String tenantId() {
        return this.tenantId;
    }

    @Override
    public String toString() {
        StringBuilder sbuilder = new StringBuilder("Security Group :")
                .append(description + ",")
                .append(id + ",")
                .append(name + ",");
        rules.forEach(rule -> sbuilder.append(rule.toString()));
        sbuilder.append(tenantId);

        return sbuilder.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof OpenstackSecurityGroup) {
            OpenstackSecurityGroup that = (OpenstackSecurityGroup) o;

            return this.description.equals(that.description) &&
                    this.tenantId.equals(that.tenantId) &&
                    this.id.equals(that.id) &&
                    this.name.equals(that.name) &&
                    this.rules.containsAll(that.rules);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(description, tenantId, id, name, rules);
    }

    /**
     * Returns the SecurityGroupRule builder object.
     *
     * @return builder object
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Represents the builder of the SecurityGroupRule.
     *
     */
    public static final class Builder {
        private String description;
        private String id;
        private String name;
        private Collection<OpenstackSecurityGroupRule> rules;
        private String tenantId;

        /**
         * Sets the description of the security group.
         *
         * @param description description
         * @return builder object
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Sets the ID of the security group.
         *
         * @param id ID
         * @return builder object
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the name of the security group.
         *
         * @param name name
         * @return builder object
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets Security Group rules.
         *
         * @param rules security group rules
         * @return builder object
         */
        public Builder rules(Collection<OpenstackSecurityGroupRule> rules) {
            this.rules = rules;
            return this;
        }

        /**
         * Sets the tenant ID of the security group.
         *
         * @param tenantId tenant ID
         * @return builder object
         */
        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        /**
         * Creates the OpenstackSecurityGroup object.
         *
         * @return OpenstackSecurityGroup object
         */
        public OpenstackSecurityGroup build() {
            return new OpenstackSecurityGroup(description, id, name, rules, tenantId);
        }
    }
}
