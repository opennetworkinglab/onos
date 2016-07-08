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
 * Represent the object for the xml element of vpnInstAFs.
 */
public class NetconfVpnInstAFs {
    private final List<NetconfVpnInstAF> vpnInstAFs;

    /**
     * NetconfVpnInstAFs constructor.
     * 
     * @param vpnInstAFs List of NetconfVpnInstAF
     */
    public NetconfVpnInstAFs(List<NetconfVpnInstAF> vpnInstAFs) {
        checkNotNull(vpnInstAFs, "vpnInstAFs cannot be null");
        this.vpnInstAFs = vpnInstAFs;
    }

    /**
     * Returns vpnInstAFs.
     * 
     * @return vpnInstAFs
     */
    public List<NetconfVpnInstAF> vpnInstAFs() {
        return vpnInstAFs;
    }

    @Override
    public int hashCode() {
        return Objects.hash(vpnInstAFs);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof NetconfVpnInstAFs) {
            final NetconfVpnInstAFs other = (NetconfVpnInstAFs) obj;
            return Objects.equals(this.vpnInstAFs, other.vpnInstAFs);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("vpnInstAFs", vpnInstAFs).toString();
    }
}
