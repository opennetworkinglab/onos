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

package org.onosproject.incubator.net.dpi;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Protocol statistic information.
 */
public class ProtocolStatInfo {
    String name;
    String breed;
    long packets;
    long bytes;
    int flows;

    /**
     * Constructor for default ProtocolStatInfo class.
     */
    public ProtocolStatInfo() {
        name = "";
        breed = "";
        packets = 0;
        bytes = 0;
        flows = 0;
    }

    /**
     * Constructor for ProtocolStatInfo class specified with protocol statistic parameters.
     *
     * @param name protocol name
     * @param breed protocol breed
     * @param packets protocol packets
     * @param bytes protocol bytes
     * @param flows protocol flows
     */
    public ProtocolStatInfo(String name, String breed, long packets, long bytes, int flows) {
        this.name = name;
        this.breed = breed;
        this.packets = packets;
        this.bytes = bytes;
        this.flows = flows;
    }

    /**
     * Returns DPI protocol name.
     *
     * @return name
     */
    public String name() {
        return name;
    }

    /**
     * Returns DPI protocol breed.
     *
     * @return breed
     */
    public String breed() {
        return breed;
    }

    /**
     * Returns DPI protocol packets.
     *
     * @return packets
     */
    public long packets() {
        return packets;
    }

    /**
     * Returns DPI protocol bytes.
     *
     * @return bytes
     */
    public long bytes() {
        return bytes;
    }

    /**
     * Returns DPI protocol flows.
     *
     * @return flows
     */
    public int flows() {
        return flows;
    }


    public void setName(String name) {
        this.name = name;
    }

    public void setBreed(String breed) {
        this.breed = breed;
    }

    public void setPackets(long packets) {
        this.packets = packets;
    }

    public void setBytes(long bytes) {
        this.bytes = bytes;
    }

    public void setFlows(int flows) {
        this.flows = flows;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("name", name)
                .add("breed", breed)
                .add("packets", packets)
                .add("bytes", bytes)
                .add("flows", flows)
                .toString();
    }
}
