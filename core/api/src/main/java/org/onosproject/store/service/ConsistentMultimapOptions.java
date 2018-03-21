/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.store.service;

import org.onosproject.store.primitives.DistributedPrimitiveOptions;

/**
 * A builder class for {@code AsyncConsistentMultimap}.
 */
public abstract class ConsistentMultimapOptions<O extends ConsistentMultimapOptions<O, K, V>, K, V>
        extends DistributedPrimitiveOptions<O> {

    private boolean purgeOnUninstall = false;

    public ConsistentMultimapOptions() {
        super(DistributedPrimitive.Type.CONSISTENT_MULTIMAP);
    }

    /**
     * Clears multimap contents when the owning application is uninstalled.
     *
     * @return this builder
     */
    public O withPurgeOnUninstall() {
        purgeOnUninstall = true;
        return (O) this;
    }

    /**
     * Returns if multimap entries need to be cleared when owning application
     * is uninstalled.
     *
     * @return true if items are to be cleared on uninstall
     */
    public boolean purgeOnUninstall() {
        return purgeOnUninstall;
    }

}
