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
package org.onosproject.net.config.basics;

import org.onosproject.net.config.Config;

/**
 * Base abstraction for network entities for which admission into control
 * domain can be selectively configured, e.g. devices, end-stations, links
 */
public abstract class AllowedEntityConfig<S> extends Config<S> {

    protected static final String ALLOWED = "allowed";

    /**
     * Indicates whether the element is allowed for admission into the control
     * domain.
     *
     * @return true if element is allowed
     */
    public boolean isAllowed() {
        return get(ALLOWED, true);
    }

    /**
     * Specifies whether the element is to be allowed for admission into the
     * control domain.
     *
     * @param isAllowed true to allow; false to forbid; null to clear
     * @return self
     */
    public AllowedEntityConfig isAllowed(Boolean isAllowed) {
        return (AllowedEntityConfig) setOrClear(ALLOWED, isAllowed);
    }

}
