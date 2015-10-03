/*
 * Copyright 2015 Open Networking Laboratory
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

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

public final class BindingHostId {
    private final String bindingHostId;

    // Public construction is prohibited
    private BindingHostId(String bindingHostId) {
        checkNotNull(bindingHostId, "BindingHosttId cannot be null");
        this.bindingHostId = bindingHostId;
    }

    /**
     * Creates a BindingHostId identifier.
     *
     * @param bindingHostId the bindingHostId identifier
     * @return the bindingHostId identifier
     */
    public static BindingHostId bindingHostId(String bindingHostId) {
        return new BindingHostId(bindingHostId);
    }

    /**
     * Returns the bindingHostId identifier.
     *
     * @return the bindingHostId identifier
     */
    public String bindingHostId() {
        return bindingHostId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(bindingHostId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof BindingHostId) {
            final BindingHostId that = (BindingHostId) obj;
            return this.getClass() == that.getClass()
                    && Objects.equals(this.bindingHostId, that.bindingHostId);
        }
        return false;
    }

    @Override
    public String toString() {
        return bindingHostId;
    }
}
