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

import com.google.common.base.MoreObjects;

import java.util.Map;
import java.util.Objects;

/**
 * Represents statistics associated to a mirroring.
 */
public final class MirroringStatistics {

    private MirroringName mirroringName;
    private int txBytes;
    private int txPackets;

    /**
     * Statistics associated to a named mirroring.
     *
     * @param name the name of the mirroring
     * @param bytes transmitted bytes
     * @param packets transmitted packets
     */
    private MirroringStatistics(String name, int bytes, int packets) {
        this.mirroringName = MirroringName.mirroringName(name);
        this.txBytes = bytes;
        this.txPackets = packets;
    }

    /**
     *
     * Creates a MirroringStatistics using the supplied information.
     *
     * @param name the name of the mirroring
     * @param statistics the associated statistics
     * @return the MirroringStatistics object
     */
    public static MirroringStatistics mirroringStatistics(String name, Map<String, Integer> statistics) {
        return new MirroringStatistics(name, statistics.get("tx_bytes"), statistics.get("tx_packets"));
    }

    /**
     * Returns the mirroring name string.
     *
     * @return name string
     */
    public MirroringName name() {
        return mirroringName;
    }

    /**
     * Returns the transmitted bytes.
     *
     * @return the bytes
     */
    public long bytes() {
        return txBytes;
    }

    /**
     * Returns the transmitted packtes.
     *
     * @return the packets
     */
    public long packtes() {
        return txPackets;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name().name(), txBytes, txPackets);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof MirroringStatistics) {
            final MirroringStatistics that = (MirroringStatistics) obj;
            return this.getClass() == that.getClass() &&
                    Objects.equals(this.mirroringName, that.mirroringName) &&
                    Objects.equals(this.txBytes, that.txBytes) &&
                    Objects.equals(this.txPackets, that.txPackets);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("name", name())
                .add("tx_bytes", bytes())
                .add("tx_packets", packtes())
                .toString();
    }

}
