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

import com.google.common.base.MoreObjects;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Default implementation class of kubevirt security group.
 */
public final class DefaultKubevirtSecurityGroup implements KubevirtSecurityGroup {

    private static final String NOT_NULL_MSG = "Security Group % cannot be null";

    private final String id;
    private final String name;
    private final String description;
    private final Set<KubevirtSecurityGroupRule> rules;

    /**
     * A default constructor.
     *
     * @param id            security group identifier
     * @param name          security group name
     * @param description   security group description
     * @param rules         security group rules
     */
    public DefaultKubevirtSecurityGroup(String id, String name, String description,
                                        Set<KubevirtSecurityGroupRule> rules) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.rules = rules;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public Set<KubevirtSecurityGroupRule> rules() {
        return Objects.requireNonNullElseGet(rules, HashSet::new);
    }

    @Override
    public KubevirtSecurityGroup updateRules(Set<KubevirtSecurityGroupRule> updatedRules) {
        return new Builder()
                .id(id)
                .name(name)
                .description(description)
                .rules(updatedRules)
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DefaultKubevirtSecurityGroup that = (DefaultKubevirtSecurityGroup) o;
        return id.equals(that.id) && name.equals(that.name) &&
                Objects.equals(description, that.description) &&
                Objects.equals(rules, that.rules);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, rules);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("name", name)
                .add("description", description)
                .add("rules", rules)
                .toString();
    }

    /**
     * Returns new builder instance.
     *
     * @return kubevirt security group builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder implements KubevirtSecurityGroup.Builder {

        private String id;
        private String name;
        private String description;
        private Set<KubevirtSecurityGroupRule> rules;

        @Override
        public KubevirtSecurityGroup build() {
            checkArgument(id != null, NOT_NULL_MSG, "id");
            checkArgument(name != null, NOT_NULL_MSG, "name");

            return new DefaultKubevirtSecurityGroup(id, name, description, rules);
        }

        @Override
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        @Override
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        @Override
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        @Override
        public Builder rules(Set<KubevirtSecurityGroupRule> rules) {
            this.rules = rules;
            return this;
        }
    }
}
