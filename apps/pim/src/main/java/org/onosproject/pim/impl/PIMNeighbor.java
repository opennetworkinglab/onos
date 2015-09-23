/*
 * Copyright 2014-2015 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in reliance with the License.
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

import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.TimerTask;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.pim.PIMHello;
import org.onlab.packet.pim.PIMHelloOption;
import org.onosproject.net.ConnectPoint;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * PIMNeighbor represents all the PIM routers that have sent us
 * hello messages, or that possibly have been statically configured.
 */
public class PIMNeighbor {
    private final Logger log = getLogger(getClass());

    // The primary address of this PIM neighbor
    private IpAddress primaryAddr;

    // The MacAddress of this neighbor
    private MacAddress macAddress;

    // The ConnectPoint this PIM neighbor is connected to.
    private ConnectPoint connectPoint;

    // Is this neighbor us?
    private boolean isThisUs = false;

    // The option values this neighbor has sent us.
    private int priority = 0;
    private int genId = 0;
    private short holdtime = 0;

    // Is this pim neighbor the DR?
    private boolean isDr = false;

    // Timeout for this neighbor
    private volatile Timeout timeout;

    private boolean reelect = false;

    // A back pointer the neighbors list this neighbor belongs to.
    private PIMNeighbors neighbors;

    /**
     * Construct this neighbor from the address and connect point.
     *
     * @param ipaddr IP Address of neighbor
     * @param macaddr MAC Address of the neighbor
     * @param cp The ConnectPoint of this neighbor
     */
    public PIMNeighbor(IpAddress ipaddr, MacAddress macaddr, ConnectPoint cp) {
        this.macAddress = macaddr;
        this.primaryAddr = ipaddr;
        this.connectPoint = cp;
        this.resetTimeout();
    }

    /**
     * Get the primary address of this neighbor.
     *
     * @return the primary IP address.
     */
    public IpAddress getPrimaryAddr() {
        return primaryAddr;
    }

    /**
     * Set the primary address of this neighbor.
     *
     * @param primaryAddr the address we'll use when sending hello messages
     */
    public void setPrimaryAddr(IpAddress primaryAddr) {
        this.primaryAddr = primaryAddr;
    }

    /**
     * Get the priority this neighbor has advertised to us.
     *
     * @return the priority
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Set the priority for this neighbor.
     *
     * @param priority This neighbors priority.
     */
    public void setPriority(int priority) {
        this.priority = priority;
    }

    /**
     * Get the generation ID.
     *
     * @return the generation ID.
     */
    public int getGenId() {
        return genId;
    }

    /**
     * Set the generation ID.
     *
     * @param genId the generation ID.
     */
    public void setGenId(int genId) {
        this.genId = genId;
    }

    /**
     * Get the holdtime for this neighbor.
     *
     * @return the holdtime
     */
    public short getHoldtime() {
        return holdtime;
    }

    /**
     * Set the holdtime for this neighbor.
     *
     * @param holdtime the holdtime.
     */
    public void setholdtime(short holdtime) {
        this.holdtime = holdtime;
    }

    /**
     * Is this neighbor the designated router on this connect point?
     *
     * @return true if so, false if not.
     */
    public boolean isDr() {
        return isDr;
    }

    /**
     * Set this router as the designated router on this connect point.
     *
     * @param isDr True is this neighbor is the DR false otherwise
     */
    public void setIsDr(boolean isDr) {
        this.isDr = isDr;
    }

    /**
     * The ConnectPoint this neighbor is connected to.
     *
     * @return the ConnectPoint
     */
    public ConnectPoint getConnectPoint() {
        return connectPoint;
    }

    /**
     * Set the ConnectPoint this router is connected to.
     *
     * @param connectPoint the ConnectPoint this router is connected to.
     */
    public void setConnectPoint(ConnectPoint connectPoint) {
        this.connectPoint = connectPoint;
    }

    /**
     * Set a back pointer to the neighbors list this neighbor is a member of.
     *
     * @param neighbors the neighbor list this neighbor belongs to
     */
    public void setNeighbors(PIMNeighbors neighbors) {
        this.neighbors = neighbors;
    }

    /**
     * We have received a fresh hello from a neighbor, now we need to process it.
     * Depending on the values received in the the hello options may force a
     * re-election process.
     *
     * We will also refresh the timeout for this neighbor.
     *
     * @param hello copy of the hello we'll be able to extract options from.
     */
    public void refresh(PIMHello hello) {
        checkNotNull(hello);

        for (PIMHelloOption opt : hello.getOptions().values()) {

            int len = opt.getOptLength();
            byte [] value = new byte[len];
            ByteBuffer bb = ByteBuffer.wrap(value);

            switch (opt.getOptType()) {
                case PIMHelloOption.OPT_GENID:
                    int newid = bb.getInt();
                    if (this.genId != newid) {
                        // TODO: we have a newly rebooted neighbor.  Send them our joins.
                        this.genId = newid;
                    }
                    break;

                case PIMHelloOption.OPT_PRIORITY:
                    int newpri = bb.getInt();
                    if (this.priority != newpri) {

                        // The priorities have changed.  We may need to re-elect a new DR?
                        if (this.isDr || this.neighbors.getDesignatedRouter().getPriority() < priority) {
                            reelect = true;
                        }
                        this.priority = newpri;
                    }
                    break;

                case PIMHelloOption.OPT_HOLDTIME:
                    short holdtime = bb.getShort();
                    if (this.holdtime != holdtime) {
                        this.holdtime = holdtime;
                        if (holdtime == 0) {
                            // We have a neighbor going down.  We can remove all joins
                            // we have learned from them.
                            // TODO: What else do we need to do when a neighbor goes down?

                            log.debug("PIM Neighbor has timed out: {}", this.primaryAddr.toString());
                            return;
                        }
                    }
                    break;

                case PIMHelloOption.OPT_PRUNEDELAY:
                case PIMHelloOption.OPT_ADDRLIST:
                    // TODO: implement prune delay and addr list.  Fall through for now.

                default:
                    log.debug("PIM Hello option type: {} not yet supported or unknown.", opt.getOptType());
                    break;
            }
        }

        if (reelect) {
            this.neighbors.electDR(this);
        }

        // Reset the next timeout timer
        this.resetTimeout();
    }

    /* --------------------------------------- Timer functions -------------------------- */

    /**
     * Restart the timeout task for this neighbor.
     */
    private void resetTimeout() {

        if (this.holdtime == 0) {

            // Prepare to die.
            log.debug("shutting down timer for nbr {}", this.primaryAddr.toString());
            if (this.timeout != null) {
                this.timeout.cancel();
                this.timeout = null;
            }
            return;
        }

        // Cancel the existing timeout and start a fresh new one.
        if (this.timeout != null) {
            this.timeout.cancel();
        }

        this.timeout = PIMTimer.getTimer().newTimeout(new NeighborTimeoutTask(this), holdtime, TimeUnit.SECONDS);
    }

    /**
     * The task to run when a neighbor timeout expires.
     */
    private final class NeighborTimeoutTask implements TimerTask {
        PIMNeighbor nbr;

        NeighborTimeoutTask(PIMNeighbor nbr) {
            this.nbr = nbr;
        }

        @Override
        public void run(Timeout timeout) throws Exception {

            // TODO: log.debug;
            PIMNeighbors neighbors = nbr.neighbors;
            neighbors.removeNeighbor(nbr.getPrimaryAddr());
        }
    }

    /**
     * Stop the timeout timer.
     *
     * This happens when we remove the neighbor.
     */
    private final void stopTimeout() {
        this.timeout.cancel();
        this.timeout = null;
    }

    @Override
    public String toString() {
        String out = "";
        if (this.isDr) {
            out += "*NBR:";
        } else {
            out += "NBR:";
        }
        out += "\tIP: " + this.primaryAddr.toString();
        out += "\tPr: " + String.valueOf(this.priority);
        out += "\tHoldTime: " + String.valueOf(this.holdtime);
        out += "\tGenID: " + String.valueOf(this.genId) + "\n";
        return out;
    }
}