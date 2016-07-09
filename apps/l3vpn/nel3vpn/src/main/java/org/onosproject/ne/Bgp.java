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
package org.onosproject.ne;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Objects;

/**
 * Bgp.
 */
public class Bgp {
    private final List<BgpImportProtocol> importProtocols;

    /**
     * Bgp constructor.
     *
     * @param protocolType the bgp protocol type
     */
    public Bgp(List<BgpImportProtocol> importProtocols) {
        checkNotNull(importProtocols, "importProtocols cannot be null");
        this.importProtocols = importProtocols;
    }

    /**
     * Returns list of importProtocol.
     *
     * @return importProtocols
     */
    public List<BgpImportProtocol> importProtocols() {
        return importProtocols;
    }

    @Override
    public int hashCode() {
        return Objects.hash(importProtocols);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Bgp) {
            final Bgp other = (Bgp) obj;
            return Objects.equals(this.importProtocols, other.importProtocols);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("importProtocols", importProtocols).toString();
    }
}
