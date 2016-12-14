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

package org.onosproject.incubator.net.virtual.provider;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

public abstract class AbstractVirtualProvider implements VirtualProvider {
    private final String scheme;
    private final String id;

    /**
     * Creates a provider with the supplied identifier.
     *
     * @param scheme provider scheme
     * @param id provider id
     */
    protected AbstractVirtualProvider(String id, String scheme) {
        this.scheme = scheme;
        this.id = id;
    }

    /**
     * Returns the device URI scheme to which this provider is bound.
     *
     * @return device URI scheme
     */
    @Override
    public String scheme() {
        return this.scheme;
    }

    /**
     * Returns the device URI scheme specific id portion.
     *
     * @return id
     */
    @Override
    public String id() {
        return this.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(scheme, id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof AbstractVirtualProvider) {
            final AbstractVirtualProvider other = (AbstractVirtualProvider) obj;
            return Objects.equals(this.scheme, other.scheme) &&
                    Objects.equals(this.id, other.id);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("scheme", scheme).add("id", id)
                .toString();
    }
}
