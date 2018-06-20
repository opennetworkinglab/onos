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
package org.onosproject.segmentrouting.xconnect.api;

import com.google.common.base.MoreObjects;
import org.onosproject.net.PortNumber;

import java.util.Objects;
import java.util.Set;

/**
 * Xconnect description.
 */
public class XconnectDesc {
    private XconnectKey key;
    private Set<PortNumber> ports;

    /**
     * Constructs new Xconnect description with given device ID and VLAN ID.
     *
     * @param key Xconnect key
     * @param ports set of ports
     */
    public XconnectDesc(XconnectKey key, Set<PortNumber> ports) {
        this.key = key;
        this.ports = ports;
    }

    /**
     * Gets Xconnect key.
     *
     * @return Xconnect key
     */
    public XconnectKey key() {
        return key;
    }

    /**
     * Gets ports.
     *
     * @return set of ports
     */
    public Set<PortNumber> ports() {
        return ports;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof XconnectDesc)) {
            return false;
        }
        final XconnectDesc other = (XconnectDesc) obj;
        return Objects.equals(this.key, other.key) &&
                Objects.equals(this.ports, other.ports);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, ports);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("key", key)
                .add("ports", ports)
                .toString();
    }
}
