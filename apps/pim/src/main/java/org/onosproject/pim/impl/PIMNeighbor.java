/*
 * Copyright 2015 Open Networking Laboratory
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

import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.pim.PIMHelloOption;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

public class PIMNeighbor {

    private final Logger log = getLogger(getClass());

    // IP Address of this neighbor
    private IpAddress ipAddr;

    // MAC Address of the neighbor (Need for sending J/P)
    private MacAddress macAddr;

    // Hello Options
    // Our hello opt holdTime
    private short holdTime;

    // Our hello opt prune delay
    private int pruneDelay;

    // Neighbor priority
    private int priority;

    // Our current genId
    private int genId;

    // Our timestamp for this neighbor
    private Date lastRefresh;

    /**
     * Construct a new PIM Neighbor.
     *
     * @param ipAddr the IP Address of our new neighbor
     * @param opts option map
     */
    public PIMNeighbor(IpAddress ipAddr, Map<Short, PIMHelloOption> opts) {
        this.ipAddr = ipAddr;
        this.addOptions(opts);
    }

    /**
     * Construct a new PIM neighbor.
     *
     * @param ipAddr the neighbors IP addr
     * @param macAddr MAC address
     */
    public PIMNeighbor(IpAddress ipAddr, MacAddress macAddr) {
        this.ipAddr = ipAddr;
        this.macAddr = macAddr;
    }

    /**
     * Get the MAC address of this neighbor.
     *
     * @return the mac address
     */
    public MacAddress getMacaddr() {
        return macAddr;
    }

    /**
     * Get the IP Address of our neighbor.
     *
     * @return the IP address of our neighbor
     */
    public IpAddress getIpaddr() {
        return ipAddr;
    }

    /**
     * Set the IP address of our neighbor.
     *
     * @param ipAddr our neighbors IP address
     */
    public void setIpaddr(IpAddress ipAddr) {
        this.ipAddr = ipAddr;
    }

    /**
     * Get our neighbors holdTime.
     *
     * @return the holdTime
     */
    public short getHoldtime() {
        return holdTime;
    }

    /**
     * Set our neighbors holdTime.
     *
     * @param holdTime the holdTime
     */
    public void setHoldtime(short holdTime) {
        this.holdTime = holdTime;
    }

    /**
     * Get our neighbors prune delay.
     *
     * @return our neighbors prune delay
     */
    public int getPruneDelay() {
        return pruneDelay;
    }

    /**
     * Set our neighbors prune delay.
     *
     * @param pruneDelay the prune delay
     */
    public void setPruneDelay(int pruneDelay) {
        this.pruneDelay = pruneDelay;
    }

    /**
     * Get our neighbors priority.
     *
     * @return our neighbors priority
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Set our neighbors priority.
     *
     * @param priority our neighbors priority
     */
    public void setPriority(int priority) {
        this.priority = priority;
    }

    /**
     * Get our neighbors Genid.
     *
     * @return our neighbor Genid
     */
    public int getGenid() {
        return genId;
    }

    /**
     * Set our neighbors GenId.
     *
     * @param genId our neighbors GenId
     */
    public void setGenid(int genId) {
        this.genId = genId;
    }

    /**
     * Add the options for this neighbor if needed.
     *
     * @param opts the options to be added/modified
     * @return true if options changed, false if no option has changed
     */
    public boolean addOptions(Map<Short, PIMHelloOption> opts) {

        boolean changed = false;

        for (PIMHelloOption opt : opts.values()) {
            Short otype = opt.getOptType();
            ByteBuffer val = ByteBuffer.wrap(opt.getValue());

            if (otype == PIMHelloOption.OPT_ADDRLIST) {
                // TODO: Will implement someday
            } else if (otype == PIMHelloOption.OPT_GENID) {
                int newval = val.getInt();
                if (newval != genId) {
                    genId = newval;
                    changed = true;
                }
            } else if (otype == PIMHelloOption.OPT_HOLDTIME) {
                short newval = val.getShort();
                if (newval != holdTime) {
                    holdTime = newval;
                    changed = true;
                }
            } else if (otype == PIMHelloOption.OPT_PRIORITY) {
                int newval = val.getInt();
                if (newval != priority) {
                    priority = newval;
                    changed = true;
                }
            } else if (otype == PIMHelloOption.OPT_PRUNEDELAY) {
                int newval = val.getInt();
                if (newval != pruneDelay) {
                    pruneDelay = newval;
                    changed = true;
                }
            } else {
                log.warn("received unknown pim hello options" + otype);
            }
        }
        return changed;
    }

    /**
     * Refresh this neighbors timestamp.
     */
    public void refreshTimestamp() {
        lastRefresh = Calendar.getInstance().getTime();
    }
}
