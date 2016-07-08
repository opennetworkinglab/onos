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
 * Represent the object for the xml element of bgpVrfs.
 */
public class NetconfBgpVrfs {
    private final List<NetconfBgpVrf> bgpVrfs;

    /**
     * NetconfBgpVrfs constructor.
     *
     * @param bgpVrfs List of NetconfBgpVrf
     */
    public NetconfBgpVrfs(List<NetconfBgpVrf> bgpVrfs) {
        checkNotNull(bgpVrfs, "bgpVrfs cannot be null");
        this.bgpVrfs = bgpVrfs;
    }

    /**
     * Returns bgpVrfs.
     *
     * @return bgpVrfs
     */
    public List<NetconfBgpVrf> bgpVrfs() {
        return bgpVrfs;
    }

    @Override
    public int hashCode() {
        return Objects.hash(bgpVrfs);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof NetconfBgpVrfs) {
            final NetconfBgpVrfs other = (NetconfBgpVrfs) obj;
            return Objects.equals(this.bgpVrfs, other.bgpVrfs);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("bgpVrfs", bgpVrfs)
                .toString();
    }
}

