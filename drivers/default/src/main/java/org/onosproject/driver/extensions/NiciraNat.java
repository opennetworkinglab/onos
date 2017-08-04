/*
 * Copyright 2017-present Open Networking Foundation
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
import com.google.common.collect.Maps;
import org.onlab.packet.IpAddress;
import org.onlab.util.KryoNamespace;
import org.onosproject.net.flow.AbstractExtension;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatmentType;
import org.onosproject.store.serializers.IpAddressSerializer;

import java.util.Map;
import java.util.Objects;

/**
 * Nicira nat extension instruction.
 */
public class NiciraNat extends AbstractExtension implements ExtensionTreatment {
    private int flags;
    private int presentFlags;
    private int portMin;
    private int portMax;
    private IpAddress ipAddressMin;
    private IpAddress ipAddressMax;
    private final KryoNamespace appKryo = new KryoNamespace.Builder()
            .register(new IpAddressSerializer(), IpAddress.class)
            .register(byte[].class)
            .build();

    /**
     * Creates a new nat instruction.
     */
    public NiciraNat() {
        flags = 0;
        presentFlags = 0;
        portMin = 0;
        portMax = 0;
        ipAddressMin = IpAddress.valueOf(0);
        ipAddressMax = IpAddress.valueOf(0);
    }

    /**
     * Creates a new nat instruction.
     * @param flags  nat flags
     * @param presentFlags  nat present flags
     * @param portMin  min port
     * @param portMax  max port
     * @param ipAddressMin  min ip address
     * @param ipAddressMax  max ip address
     */
    public NiciraNat(int flags, int presentFlags, int portMin, int portMax, IpAddress ipAddressMin,
                     IpAddress ipAddressMax) {
        this.flags = flags;
        this.presentFlags = presentFlags;
        this.portMin = portMin;
        this.portMax = portMax;
        this.ipAddressMin = ipAddressMin;
        this.ipAddressMax = ipAddressMax;
    }

    @Override
    public ExtensionTreatmentType type() {
        return ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_NAT.type();
    }

    /**
     * Get Nicira nat flags.
     * @return flags
     */
    public int niciraNatFlags() {
        return flags;
    }

    /**
     * Get Nicira present flags.
     * @return present flags
     */
    public int niciraNatPresentFlags() {
        return presentFlags;
    }

    /**
     * Get Nicira Nat min port.
     * @return min port
     */
    public int niciraNatPortMin() {
        return portMin;
    }

    /**
     * Get Nicira Nat max port.
     * @return max port
     */
    public int niciraNatPortMax() {
        return portMax;
    }

    /**
     * Get Nicira Nat min ip address.
     * @return min ipaddress
     */
    public IpAddress niciraNatIpAddressMin() {
        return ipAddressMin;
    }

    /**
     * Get Nicira Nat max ip address.
     * @return max ipaddress
     */
    public IpAddress niciraNatIpAddressMax() {
        return ipAddressMax;
    }

    @Override
    public void deserialize(byte[] data) {
        Map<String, Object> values = appKryo.deserialize(data);
        flags = (int) values.get("flags");
        presentFlags = (int) values.get("presentFlags");
        portMin = (int) values.get("portMin");
        portMax = (int) values.get("portMax");
        ipAddressMin = (IpAddress) values.get("ipAddressMin");
        ipAddressMax = (IpAddress) values.get("ipAddressMax");
    }

    @Override
    public byte[] serialize() {
        Map<String, Object> values = Maps.newHashMap();
        values.put("flags", flags);
        values.put("presentFlags", presentFlags);
        values.put("portMin", portMin);
        values.put("portMax", portMax);
        values.put("ipAddressMin", ipAddressMin);
        values.put("ipAddressMax", ipAddressMax);
        return appKryo.serialize(values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type(), flags, presentFlags, portMin, portMax, ipAddressMin, ipAddressMax);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof NiciraNat) {
            NiciraNat that = (NiciraNat) obj;
            return Objects.equals(flags, that.flags) &&
                    Objects.equals(presentFlags, that.presentFlags) &&
                    Objects.equals(portMin, that.portMin) &&
                    Objects.equals(portMax, that.portMax) &&
                    Objects.equals(ipAddressMin, that.ipAddressMin) &&
                    Objects.equals(ipAddressMax, that.ipAddressMax) &&
                    Objects.equals(this.type(), that.type());
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("flags", flags)
                .add("present_flags", presentFlags)
                .add("portMin", portMin)
                .add("portMax", portMax)
                .add("ipAddressMin", ipAddressMin)
                .add("ipAddressMax", ipAddressMax)
                .toString();
    }
}
