/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onlab.packet;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * A class to represent Groups for membership query and reports.
 */
public abstract class IGMPGroup {

    protected int auxInfo;
    protected IpAddress gaddr;
    protected List<IpAddress> sources = new ArrayList<>();

    public IGMPGroup() {
    }

    /**
     * Initialize this object with a multicast group address and additional info.
     *
     * @param gaddr the multicast group address for this message type.
     * @param auxInfo additional info potentially used by IGMPQuery
     */
    public IGMPGroup(IpAddress gaddr, int auxInfo) {
        this.gaddr = gaddr;
        this.auxInfo = auxInfo;
    }

    /**
     * Get the multicast group address.
     *
     * @return the group address
     */
    public IpAddress getGaddr() {
        return this.gaddr;
    }

    /**
     * Get the auxillary info.
     *
     * @return the auxillary info
     */
    public int getAuxInfo() {
        return this.auxInfo;
    }

    /**
     * Add a unicast source address to this message.
     *
     * @param saddr IPv4 unicast source address
     */
    public void addSource(IpAddress saddr) {
        sources.add(saddr);
    }

    /**
     * Return the list of source addresses.
     *
     * @return list of source addresses
     */
    public List<IpAddress> getSources() {
        return sources;
    }

    /**
     * Deserialize an IGMPQuery or IGMPMembership message.
     *
     * @param bb the ByteBuffer wrapping the serialized message.  The position of the
     *           ByteBuffer should be pointing at the head of either message type.
     * @return An object populated with the respective IGMPGroup subclass
     * @throws DeserializationException in case deserialization goes wrong
     */
    public abstract IGMPGroup deserialize(ByteBuffer bb) throws DeserializationException;

    /**
     * Serialize the IGMPGroup subclass.
     *
     * @param bb the ByteBuffer to write into, positioned at the next spot to be written to.
     * @return The serialized message
     */
    public abstract byte[] serialize(ByteBuffer bb);

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        sb.append("auxInfo= ");
        sb.append(auxInfo);
        sb.append("gaddr= ");
        sb.append(gaddr);
        sb.append("sources= ");
        sb.append(sources.toString());
        sb.append("]");
        return sb.toString();
    }
}
