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

import org.onlab.util.Identifier;

import static com.google.common.base.Preconditions.checkNotNull;

public final class BindingHostId extends Identifier<String> {
    // Public construction is prohibited
    private BindingHostId(String bindingHostId) {
        super(checkNotNull(bindingHostId, "BindingHosttId cannot be null"));
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
        return identifier;
    }
}
