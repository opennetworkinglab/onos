/*
 * Copyright 2015-present Open Networking Foundation
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

package org.onosproject.ui.topo;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Highlighting modification.
 * <p>
 * Note that (for link highlights) this translates to a CSS class name
 * that is applied to the link in the Topology UI.
 */
public final class Mod implements Comparable<Mod> {
    private final String modId;

    /**
     * Constructs a mod with the given identifier.
     *
     * @param modId modification identifier
     */
    public Mod(String modId) {
        this.modId = checkNotNull(modId);
    }

    @Override
    public String toString() {
        return modId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Mod mod = (Mod) o;
        return modId.equals(mod.modId);
    }

    @Override
    public int hashCode() {
        return modId.hashCode();
    }

    @Override
    public int compareTo(Mod o) {
        return this.modId.compareTo(o.modId);
    }
}
