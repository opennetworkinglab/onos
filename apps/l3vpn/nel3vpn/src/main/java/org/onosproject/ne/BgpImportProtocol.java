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

import java.util.Objects;

/**
 * BgpImportProtocol is Bgp Import Protocol.
 */
public final class BgpImportProtocol {
    private final ProtocolType protocolType;

    /**
     * BgpImportProtocol constructor.
     * 
     * @param protocolType the bgp protocol type
     */
    public BgpImportProtocol(ProtocolType protocolType) {
        checkNotNull(protocolType, "protocolType cannot be null");
        this.protocolType = protocolType;
    }

    /**
     * Returns netVpnId.
     * 
     * @return netVpnId
     */
    public ProtocolType protocolType() {
        return protocolType;
    }

    /**
     * The enumeration of bgp protocol type.
     */
    public enum ProtocolType {
        Direct(0),
        Bgp(1),
        Ospf(2),
        Isis(3);

        int value;

        private ProtocolType(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(protocolType);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof BgpImportProtocol) {
            final BgpImportProtocol other = (BgpImportProtocol) obj;
            return Objects.equals(this.protocolType, other.protocolType);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("protocolType", protocolType).toString();
    }
}
