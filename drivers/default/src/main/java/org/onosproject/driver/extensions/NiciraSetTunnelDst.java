/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.driver.extensions;

import com.google.common.base.MoreObjects;
import org.onlab.packet.Ip4Address;
import org.onlab.util.KryoNamespace;
import org.onosproject.net.flow.AbstractExtension;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatmentType;
import org.onosproject.store.serializers.Ip4AddressSerializer;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Nicira set tunnel destination extension instruction.
 */
public class NiciraSetTunnelDst extends AbstractExtension implements
        ExtensionTreatment {

    private Ip4Address tunnelDst;

    private final KryoNamespace appKryo = new KryoNamespace.Builder()
            .register(new Ip4AddressSerializer(), Ip4Address.class)
            .register(byte[].class)
            .build("NiciraSetTunnelDst");

    /**
     * Creates a new set tunnel destination instruction.
     */
    NiciraSetTunnelDst() {
        tunnelDst = null;
    }

    /**
     * Creates a new set tunnel destination instruction with a particular IPv4
     * address.
     *
     * @param tunnelDst tunnel destination IPv4 address
     */
    public NiciraSetTunnelDst(Ip4Address tunnelDst) {
        checkNotNull(tunnelDst);
        this.tunnelDst = tunnelDst;
    }

    /**
     * Gets the tunnel destination IPv4 address.
     *
     * @return tunnel destination IPv4 address
     */
    public Ip4Address tunnelDst() {
        return tunnelDst;
    }

    @Override
    public ExtensionTreatmentType type() {
        return ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_TUNNEL_DST.type();
    }

    @Override
    public void deserialize(byte[] data) {
        tunnelDst = appKryo.deserialize(data);
    }

    @Override
    public byte[] serialize() {
        return appKryo.serialize(tunnelDst);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tunnelDst);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof NiciraSetTunnelDst) {
            NiciraSetTunnelDst that = (NiciraSetTunnelDst) obj;
            return Objects.equals(tunnelDst, that.tunnelDst);

        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("tunnelDst", tunnelDst)
                .toString();
    }
}
