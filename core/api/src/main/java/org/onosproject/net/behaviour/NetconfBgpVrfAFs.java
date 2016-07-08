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
 * Represent the object for the xml element of bgpVrfAFs.
 */
public class NetconfBgpVrfAFs {
    private final List<NetconfBgpVrfAF> bgpVrfAFs;

    /**
     * NetconfBgpVrfAFs constructor.
     * 
     * @param bgpVrfAFs List of NetconfBgpVrfAF
     */
    public NetconfBgpVrfAFs(List<NetconfBgpVrfAF> bgpVrfAFs) {
        checkNotNull(bgpVrfAFs, "bgpVrfAFs cannot be null");
        this.bgpVrfAFs = bgpVrfAFs;
    }

    /**
     * Returns bgpVrfAFs.
     * 
     * @return bgpVrfAFs
     */
    public List<NetconfBgpVrfAF> bgpVrfAFs() {
        return bgpVrfAFs;
    }

    @Override
    public int hashCode() {
        return Objects.hash(bgpVrfAFs);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof NetconfBgpVrfAFs) {
            final NetconfBgpVrfAFs other = (NetconfBgpVrfAFs) obj;
            return Objects.equals(this.bgpVrfAFs, other.bgpVrfAFs);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("bgpVrfAFs", bgpVrfAFs).toString();
    }
}
