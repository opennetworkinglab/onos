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
import com.google.common.collect.ImmutableSet;

import java.util.Objects;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;

public final class DefaultKubevirtInstance implements KubevirtInstance {

    private static final String NOT_NULL_MSG = "Instance % cannot be null";

    private final String uid;
    private final String name;
    private final Set<KubevirtPort> ports;

    /**
     * Default constructor.
     *
     * @param uid       UID
     * @param name      name of instance
     * @param ports     set of ports associated with the instance
     */
    public DefaultKubevirtInstance(String uid, String name, Set<KubevirtPort> ports) {
        this.uid = uid;
        this.name = name;
        this.ports = ports;
    }

    @Override
    public String uid() {
        return uid;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Set<KubevirtPort> ports() {
        return ImmutableSet.copyOf(ports);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DefaultKubevirtInstance that = (DefaultKubevirtInstance) o;
        return uid.equals(that.uid) && name.equals(that.name) && ports.equals(that.ports);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uid, name, ports);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("uid", uid)
                .add("name", name)
                .add("ports", ports)
                .toString();
    }

    /**
     * Returns new builder instance.
     *
     * @return kubevirt port builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Default builder implementation.
     */
    public static final class Builder implements KubevirtInstance.Builder {

        private String uid;
        private String name;
        private Set<KubevirtPort> ports;

        @Override
        public KubevirtInstance build() {
            checkArgument(uid != null, NOT_NULL_MSG, "UID");
            checkArgument(name != null, NOT_NULL_MSG, "name");
            checkArgument(ports != null, NOT_NULL_MSG, "ports");

            return new DefaultKubevirtInstance(uid, name, ports);
        }

        @Override
        public Builder uid(String uid) {
            this.uid = uid;
            return this;
        }

        @Override
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        @Override
        public Builder ports(Set<KubevirtPort> ports) {
            this.ports = ports;
            return this;
        }
    }
}
