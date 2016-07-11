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
package org.onosproject.net.behaviour;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Objects;

/**
 * Represent the object for the xml element of importRoutes.
 */
public class NetconfImportRoutes {
    private final List<NetconfImportRoute> importRoutes;

    /**
     * NetconfImportRoutes constructor.
     *
     * @param importRoutes List of NetconfImportRoute
     */
    public NetconfImportRoutes(List<NetconfImportRoute> importRoutes) {
        checkNotNull(importRoutes, "importRoutes cannot be null");
        this.importRoutes = importRoutes;
    }

    /**
     * Returns importRoutes.
     *
     * @return importRoutes
     */
    public List<NetconfImportRoute> importRoutes() {
        return importRoutes;
    }

    @Override
    public int hashCode() {
        return Objects.hash(importRoutes);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof NetconfImportRoutes) {
            final NetconfImportRoutes other = (NetconfImportRoutes) obj;
            return Objects.equals(this.importRoutes, other.importRoutes);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("importRoutes", importRoutes)
                .toString();
    }
}
