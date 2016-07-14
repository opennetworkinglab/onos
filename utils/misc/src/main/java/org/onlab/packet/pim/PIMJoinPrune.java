/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onlab.packet.pim;

import org.onlab.packet.BasePacket;
import org.onlab.packet.Deserializer;
import org.onlab.packet.IPacket;
import org.onlab.packet.IpPrefix;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.onlab.packet.PacketUtils.checkInput;

public class PIMJoinPrune extends BasePacket {

    private PIMAddrUnicast upstreamAddr = new PIMAddrUnicast();
    private short holdTime = (short) 0xffff;

    private HashMap<IpPrefix, PIMJoinPruneGroup> joinPrunes = new HashMap<>();

    /**
     * Get the J/P hold time.
     *
     * @return specified in seconds.
     */
    public short getHoldTime() {
        return holdTime;
    }

    /**
     * Set the J/P holdtime in seconds.
     *
     * @param holdTime return the holdtime.
     */
    public void setHoldTime(short holdTime) {
        this.holdTime = holdTime;
    }

    /**
     * Get the upstreamAddr for this J/P request.
     *
     * @return the upstream address.
     */
    public PIMAddrUnicast getUpstreamAddr() {
        return upstreamAddr;
    }

    /**
     * Set the upstream address of this PIM J/P request.
     *
     * @param upstr the PIM Upstream unicast address
     */
    public void setUpstreamAddr(PIMAddrUnicast upstr) {
        this.upstreamAddr = upstr;
    }

    /**
     * Get the JoinPrune Group with all the joins and prunes.
     *
     * @return the joinPruneGroup collection
     */
    public Collection<PIMJoinPruneGroup> getJoinPrunes() {
        return joinPrunes.values();
    }

    /**
     * Add the specified s,g to join field.
     *
     * @param saddr the source address of the route
     * @param gaddr the group address of the route
     * @param join true for a join, false for a prune.
     */
    public void addJoinPrune(String saddr, String gaddr, boolean join) {
        IpPrefix gpfx = IpPrefix.valueOf(gaddr);
        IpPrefix spfx = IpPrefix.valueOf(saddr);
        addJoinPrune(spfx, gpfx, join);
    }

    /**
     * Add the specified S, G to the join field.
     *
     * @param spfx the source prefix of the route
     * @param gpfx the group prefix of the route
     * @param join true for join, false for prune
     */
    public void addJoinPrune(IpPrefix spfx, IpPrefix gpfx, boolean join) {
        PIMJoinPruneGroup jpg = joinPrunes.get(gpfx);
        if (jpg == null) {
                jpg = new PIMJoinPruneGroup(gpfx);
            joinPrunes.put(gpfx, jpg);
        }

        HashMap<IpPrefix, IpPrefix> members = (join) ? jpg.getJoins() : jpg.getPrunes();
        if (members.get(spfx) == null) {
            members.put(spfx, spfx);
        }
    }

    /**
     * Add a join given strings represending the source and group addresses.
     *
     * @param saddr source address
     * @param gaddr group address
     */
    public void addJoin(String saddr, String gaddr) {
        this.addJoinPrune(saddr, gaddr, true);
    }

    /**
     * Add a prune given strings represending the source and group addresses.
     *
     * @param saddr source address
     * @param gaddr group address
     */
    public void addPrune(String saddr, String gaddr) {
        this.addJoinPrune(saddr, gaddr, false);
    }

    /**
     * Sets all payloads parent packet if applicable, then serializes this
     * packet and all payloads.
     *
     * @return a byte[] containing this packet and payloads
     */
    @Override
    public byte[] serialize() {

        byte[] data = new byte[8096];      // Come up with something better
        ByteBuffer bb = ByteBuffer.wrap(data);

        bb.put(upstreamAddr.serialize());
        bb.put((byte) 0);    // reserved

        int ngrps = joinPrunes.size();
        bb.put((byte) ngrps);
        bb.putShort(this.holdTime);

        // Walk the group list and input all groups
        for (PIMJoinPruneGroup jpg : joinPrunes.values()) {
            PIMAddrGroup grp = new PIMAddrGroup(jpg.getGroup());
            bb.put(grp.serialize());

            // put the number of joins and prunes
            bb.putShort((short) jpg.getJoins().size());
            bb.putShort((short) jpg.getPrunes().size());

            // Set all of the joins
            for (IpPrefix spfx : jpg.getJoins().values()) {
                PIMAddrSource src = new PIMAddrSource(spfx);
                bb.put(src.serialize());
            }

            // Set all of the prunes
            for (IpPrefix spfx : jpg.getPrunes().values()) {
                PIMAddrSource src = new PIMAddrSource(spfx);
                bb.put(src.serialize());
            }
        }

        int len = bb.position();
        byte[] data2 = new byte[len];
        bb = ByteBuffer.wrap(data2, 0, len);
        bb.put(data, 0, len);
        return data2;
    }

    // TODO: I suppose I really need to implement this?
    @Override
    public IPacket deserialize(final byte[] data, final int offset,
                               final int length) {
        final ByteBuffer bb = ByteBuffer.wrap(data, offset, length);
        return this;
    }

    /**
     * Return the J/P deserializer function.
     *
     * @return a function that will deserialize a J/P message.
     */
    public static Deserializer<PIMJoinPrune> deserializer() {
        return (data, offset, length) -> {

            /*
             * Delay buffer checks until we read enough of the packet to know how
             * much data we will require.  Each encoded address deserializer function
             * will ensure there is enough data for that address.
             */
            PIMJoinPrune jp = new PIMJoinPrune();
            final ByteBuffer bb = ByteBuffer.wrap(data, offset, length);

            // We must get a PIM encoded unicast address
            PIMAddrUnicast upstream = new PIMAddrUnicast();
            upstream.deserialize(bb);
            jp.setUpstreamAddr(upstream);

            // Use this boolean to determine the buffer space we need according to address sizes
            boolean ipv4 = upstream.getAddr().isIp4();

            // We need at minimum 4 bytes for reserved(1), ngroups(1) & holdtime(2)
            checkInput(bb.array(), bb.position(), bb.limit() - bb.position(), 4);

            // get and skip the reserved byte
            bb.get();

            // Get the number of groups.
            int ngroups = bb.get();

            // Save the holdtime.
            jp.setHoldTime(bb.getShort());


            for (int i = 0; i < ngroups; i++) {
                PIMAddrGroup grp = new PIMAddrGroup();

                /*
                 * grp.deserialize will ensure the buffer has enough data to read the group address.
                 */
                grp.deserialize(bb);

                checkInput(bb.array(), bb.position(), bb.limit() - bb.position(), 4);
                int njoins = bb.getShort();
                int nprunes = bb.getShort();

                /*
                 * Now we'll verify we have enough buffer to read the next
                 * group of join and prune addresses for this group.
                 */
                int required = (njoins + nprunes) *
                        (ipv4 ? PIMAddrSource.ENC_SOURCE_IPV4_BYTE_LENGTH : PIMAddrSource.ENC_SOURCE_IPV6_BYTE_LENGTH);
                checkInput(bb.array(), bb.position(), bb.limit() - bb.position(), required);

                // Now iterate through the joins for this group
                for (; njoins > 0; njoins--) {

                    PIMAddrSource src = new PIMAddrSource();
                    src.deserialize(bb);

                    jp.addJoinPrune(
                            src.getAddr().toIpPrefix(),
                            grp.getAddr().toIpPrefix(), true);
                }

                // Now iterate through the prunes for this group
                for (; nprunes > 0; nprunes--) {

                    PIMAddrSource src = new PIMAddrSource();
                    src.deserialize(bb);
                    jp.addJoinPrune(
                            src.getAddr().toIpPrefix(),
                            grp.getAddr().toIpPrefix(), false);
                }
            }

            return jp;
        };
    }

    @Override
    public String toString() {
        return toStringHelper(getClass())
                .add("upstreamAddr", upstreamAddr.toString())
                .add("holdTime", Short.toString(holdTime))
                .toString();
        // TODO: need to handle joinPrunes
    }
}
