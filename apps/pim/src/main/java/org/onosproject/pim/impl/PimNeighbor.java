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
package org.onosproject.pim.impl;

import com.google.common.base.MoreObjects;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.pim.PIMHelloOption;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents a PIM neighbor.
 */
public class PimNeighbor {

    // IP Address of this neighbor
    private final IpAddress ipAddr;

    // MAC Address of the neighbor (Need for sending J/P)
    private final MacAddress macAddr;

    // Hello Options
    // Our hello opt holdTime
    private final short holdTime;

    // Our hello opt prune delay
    private final int pruneDelay;

    // Neighbor priority
    private final int priority;

    // Our current genId
    private final int genId;

    private final long upTime;

    // Our timestamp for this neighbor
    private long lastRefresh;

    /**
     * Class constructor.
     *
     * @param ipAddress neighbor IP address
     * @param macAddress neighbor MAC address
     * @param holdTime hold time
     * @param pruneDelay prune delay
     * @param priority priority
     * @param genId generation ID
     */
    public PimNeighbor(IpAddress ipAddress, MacAddress macAddress,
                       short holdTime, int pruneDelay, int priority, int genId) {
        this.ipAddr = checkNotNull(ipAddress);
        this.macAddr = checkNotNull(macAddress);
        this.holdTime = holdTime;
        this.pruneDelay = pruneDelay;
        this.priority = priority;
        this.genId = genId;

        this.upTime = System.currentTimeMillis();
    }

    /**
     * Gets the IP address of our neighbor.
     *
     * @return the IP address of our neighbor
     */
    public IpAddress ipAddress() {
        return ipAddr;
    }

    /**
     * Gets the MAC address of this neighbor.
     *
     * @return the mac address
     */
    public MacAddress macAddress() {
        return macAddr;
    }

    /**
     * Gets our neighbor's hold time.
     *
     * @return the hold time
     */
    public short holdtime() {
        return holdTime;
    }

    /**
     * Gets our neighbor's prune delay.
     *
     * @return our neighbor's prune delay
     */
    public int pruneDelay() {
        return pruneDelay;
    }

    /**
     * Gets our neighbor's priority.
     *
     * @return our neighbor's priority
     */
    public int priority() {
        return priority;
    }

    /**
     * Gets our neighbor's generation ID.
     *
     * @return our neighbor's generation ID
     */
    public int generationId() {
        return genId;
    }

    /**
     * Gets the last time we heard a HELLO from this neighbor.
     *
     * @return last refresh time
     */
    public long lastRefresh() {
        return lastRefresh;
    }

    /**
     * Gets the time that we first learnt of this neighbor.
     *
     * @return up time
     */
    public long upTime() {
        return upTime;
    }

    /**
     * Refreshes this neighbor's last seen timestamp.
     */
    public void refreshTimestamp() {
        lastRefresh = System.currentTimeMillis();
    }

    /**
     * Returns whether this neighbor is expired or not.
     *
     * @return true if the neighbor is expired, otherwise false
     */
    public boolean isExpired() {
        return lastRefresh + TimeUnit.SECONDS.toMillis(holdTime)
                < System.currentTimeMillis();
    }

    /**
     * Creates a PIM neighbor based on an IP, MAC, and collection of PIM HELLO
     * options.
     *
     * @param ipAddress neighbor IP address
     * @param macAddress neighbor MAC address
     * @param opts options from the PIM HELLO packet
     * @return new PIM neighbor
     */
    public static PimNeighbor createPimNeighbor(IpAddress ipAddress,
                                                MacAddress macAddress,
                                                Collection<PIMHelloOption> opts) {

        int generationID = PIMHelloOption.DEFAULT_GENID;
        short holdTime = PIMHelloOption.DEFAULT_HOLDTIME;
        int priority = PIMHelloOption.DEFAULT_PRIORITY;
        int pruneDelay = PIMHelloOption.DEFAULT_PRUNEDELAY;

        for (PIMHelloOption opt : opts) {
            short type = opt.getOptType();
            ByteBuffer value = ByteBuffer.wrap(opt.getValue());

            if (type == PIMHelloOption.OPT_GENID) {
                generationID = value.getInt();
            } else if (type == PIMHelloOption.OPT_HOLDTIME) {
                holdTime = value.getShort();
            } else if (type == PIMHelloOption.OPT_PRIORITY) {
                priority = value.getInt();
            } else if (type == PIMHelloOption.OPT_PRUNEDELAY) {
                pruneDelay = value.getInt();
            } else if (type == PIMHelloOption.OPT_ADDRLIST) {
                // TODO: Will implement someday
            }
        }

        return new PimNeighbor(ipAddress, macAddress, holdTime, pruneDelay, priority, generationID);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof PimNeighbor)) {
            return false;
        }

        PimNeighbor that = (PimNeighbor) other;

        return this.ipAddr.equals(that.ipAddress()) &&
                this.macAddr.equals(that.macAddress()) &&
                this.genId == that.genId &&
                this.holdTime == that.holdTime &&
                this.priority == that.priority;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ipAddr, macAddr, genId, holdTime, priority);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("ipAddress", ipAddr)
                .add("macAddress", macAddr)
                .add("generationId", genId)
                .add("holdTime", holdTime)
                .add("priority", priority)
                .add("pruneDelay", pruneDelay)
                .toString();
    }

}
