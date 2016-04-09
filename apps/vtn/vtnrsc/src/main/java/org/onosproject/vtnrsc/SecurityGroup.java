/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.vtnrsc;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Immutable representation of a security group.
 */
public final class SecurityGroup {
    private final String securityGroup;

    /**
     * Returns the securityGroup.
     *
     * @return securityGroup
     */
    public String securityGroup() {
        return securityGroup;
    }
    // Public construction is prohibited
    private SecurityGroup(String securityGroup) {
        checkNotNull(securityGroup, "SecurityGroup cannot be null");
        this.securityGroup = securityGroup;
    }

    /**
     * Creates a securityGroup using the supplied securityGroup.
     *
     * @param securityGroup security group
     * @return securityGroup
     */
    public static SecurityGroup securityGroup(String securityGroup) {
        return new SecurityGroup(securityGroup);
    }

    @Override
    public int hashCode() {
        return securityGroup.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof SecurityGroup) {
            final SecurityGroup that = (SecurityGroup) obj;
            return this.getClass() == that.getClass()
                    && Objects.equals(this.securityGroup, that.securityGroup);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("securityGroup", securityGroup)
                .toString();
    }

}
