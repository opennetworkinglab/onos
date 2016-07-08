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

import java.util.Objects;

/**
 * Represent the object for the xml element of bgpVrfAF.
 */
public class NetconfBgpVrfAF {
    private final String afType;
    private final NetconfImportRoutes importRoutes;

    /**
     * NetconfBgpVrfAF constructor.
     *
     * @param afType address family Type
     * @param importRoutes import routes
     */
    public NetconfBgpVrfAF(String afType, NetconfImportRoutes importRoutes) {
        checkNotNull(afType, "afType cannot be null");
        checkNotNull(importRoutes, "importRoutes cannot be null");
        this.afType = afType;
        this.importRoutes = importRoutes;
    }

    /**
     * Returns afType.
     *
     * @return afType
     */
    public String afType() {
        return afType;
    }

    /**
     * Returns importRoutes.
     *
     * @return importRoutes
     */
    public NetconfImportRoutes importRoutes() {
        return importRoutes;
    }

    @Override
    public int hashCode() {
        return Objects.hash(afType, importRoutes);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof NetconfBgpVrfAF) {
            final NetconfBgpVrfAF other = (NetconfBgpVrfAF) obj;
            return Objects.equals(this.afType, other.afType)
                    && Objects.equals(this.importRoutes, other.importRoutes);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("afType", afType)
                .add("importRoutes", importRoutes).toString();
    }
}
