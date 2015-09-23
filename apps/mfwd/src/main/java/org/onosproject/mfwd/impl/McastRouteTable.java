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
package org.onosproject.mfwd.impl;

import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.IpPrefix;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The Mcast Route Table holds all multicast state for the controller.
 *
 * State for IPv4 and IPv6 are maintained.  The tables are sets of McastRouteGroup
 * structures that represent (*, G) state with a series of egress ConnectPoints.
 * Each (*, G) may also have a set of (S, G) that may have there own set of
 * ingress and egress ConnectPoints.
 *
 * TODO: perhaps should probably create two separate singleton for IPv4 and IPv6 respectively.
 */
@Service(value = org.onosproject.mfwd.impl.McastRouteTable.class)
public final class McastRouteTable {

    /*
     * Create a map of the McastGroups indexed by the multicast group prefix.
     * We may choose to change the map data structure in to some form a radix trie
     * depending on the type of real world usage we see.
     */
    private final Map<IpPrefix, McastRouteGroup> mrib4;
    private final Map<IpPrefix, McastRouteGroup> mrib6;
    private static McastRouteTable instance = null;

    private Boolean ipv6Enabled = false;

    /**
     * Create the two v4 & v6 tables.
     */
    private McastRouteTable() {
        mrib4 = new ConcurrentHashMap<IpPrefix, McastRouteGroup>();
        if (ipv6Enabled) {
            mrib6 = new ConcurrentHashMap<IpPrefix, McastRouteGroup>();
        } else {
            mrib6 = null;
        }
    }

    /**
     * Get the single instance of this multicast group address.
     *
     * @return the multicast route table
     */
    public static McastRouteTable getInstance() {
        if (instance == null) {
            instance = new McastRouteTable();
        }
        return instance;
    }

    /**
     * Get the IPv4 MRIB.
     *
     * @return the IPv4 MRIB
     */
    public Map<IpPrefix, McastRouteGroup> getMrib4() {
        return mrib4;
    }

    /**
     * Get the IPv6 MRIB.
     *
     * @return Return the set of prefix keyed McastGroups
     */
    public Map<IpPrefix, McastRouteGroup> getMrib6() {
        return mrib6;
    }

    /**
     * Save the McastRouteGroup in the address family appropriate mrib.
     *
     * @param group The McastRouteGroup to save
     */
    private void storeGroup(McastRouteGroup group) {
        if (group.isIp4()) {
            mrib4.put(group.getGaddr(), group);
        } else if (group.isIp6() && ipv6Enabled) {
            mrib6.put(group.getGaddr(), group);
        }
    }

    /**
     * Remove the group.
     *
     * @param group the group to be removed
     */
    private void removeGroup(McastRouteGroup group) {
        IpPrefix gpfx = group.getGaddr();
        if (gpfx.isIp4()) {
            mrib4.remove(gpfx);
        } else if (gpfx.isIp6() && ipv6Enabled) {
            mrib6.remove(gpfx);
        }
    }

    /**
     * Add a multicast route to the MRIB.  This function will.
     *
     * @param saddr source address * or x.x.x.x or x.x.x.x/y
     * @param gaddr group address x.x.x.x or x.x.x.x/y
     * @return the multicast route
     */
    public McastRouteBase addRoute(String saddr, String gaddr) {
        IpPrefix gpfx = IpPrefix.valueOf(gaddr);
        IpPrefix spfx = IpPrefix.valueOf(0, 0);
        if (saddr != null && !saddr.equals("*")) {
            spfx = IpPrefix.valueOf(saddr);
        }
        return addRoute(spfx, gpfx);
    }

    /**
     * Add a multicast route to the MRIB.  This function will store either
     * (S, G) or (*, G) in the mrib if an entry does not already exist. If
     * an entry does exist it is returned to the caller.
     *
     * Every (S, G) is stored as part of it's parent group entry which also represents
     * (*, G) routes.  In the case of a (S, G) we will also create the (*, G) entry if needed
     * then save the (S, G) to the (*, G).
     *
     * @param spfx the source prefix
     * @param gpfx the group prefix
     * @return the resulting McastRouteSource or McastRouteGroup accordingly.
     */
    public McastRouteBase addRoute(IpPrefix spfx, IpPrefix gpfx) {

        /**
         * If a group route (*, g) does not exist we will need to make so we
         * can start attaching our sources to the group entry.
         */
        McastRouteGroup group = findMcastGroup(gpfx);
        if (group == null) {
            group = new McastRouteGroup(gpfx);

            // Save it for later
            if (gpfx.isIp4()) {
                this.mrib4.put(gpfx, group);
            } else if (gpfx.isIp6() && ipv6Enabled) {
                    this.mrib6.put(gpfx, group);
            }
        }

        /**
         * If the source prefix length is 0 then we have our (*, g) entry, we can
         * just return now.
         */
        if (spfx.prefixLength() == 0) {
            return group;
        }

        // See if the source already exists.  If so just return it.
        McastRouteSource source = group.findSource(spfx);
        if (source != null) {
            return source;
        }

        /**
         * We have the group but no source.  We need to create the source then add it
         * to the group.
         */
        source = new McastRouteSource(spfx, gpfx);

        // Have the source save it's parent
        source.setGroup(group);

        // Save this source as part of this group
        group.addSource(source);

        return source;
    }

    /**
     * Delete a multicast route from the MRIB.
     *
     * @param saddr source address * or x.x.x.x or x.x.x.x/y
     * @param gaddr group address x.x.x.x or x.x.x.x/y
     */
    public void removeRoute(String saddr, String gaddr) {
        IpPrefix gpfx = IpPrefix.valueOf(gaddr);
        IpPrefix spfx = IpPrefix.valueOf(0, 0);
        if (saddr != null && !saddr.equals("*")) {
            spfx = IpPrefix.valueOf(saddr);
        }
        removeRoute(spfx, gpfx);
    }

    /**
     * Remove a multicast route.
     *
     * @param spfx the source prefix
     * @param gpfx the group prefix
     */
    public void removeRoute(IpPrefix spfx, IpPrefix gpfx) {

        /**
         * If a group route (*, g) does not exist we will need to make so we
         * can start attaching our sources to the group entry.
         */
        McastRouteGroup group = findMcastGroup(gpfx);
        if (group == null) {
            // The group does not exist, we can't remove it.
            return;
        }

        /**
         * If the source prefix length is 0 then we have a (*, g) entry, which
         * means we will remove this group and all of it's sources. We will
         * also withdraw it's intent if need be.
         */
        if (spfx.prefixLength() > 0) {
            group.removeSource(spfx);

            /*
             * Now a little house keeping. If this group has no more sources
             * nor egress connectPoints git rid of it.
             */
            if (group.getSources().size() == 0 &&
                    group.getEgressPoints().size() == 0) {
                removeGroup(group);
            }

        } else {
            // Group remove has been explicitly requested.
            group.removeSources();
            group.withdrawIntent();
            removeGroup(group);
        }
    }

    /**
     * Find the specific multicast group entry.
     *
     * @param group the group address
     * @return McastRouteGroup the multicast (*, G) group route
     */
    public McastRouteGroup findMcastGroup(IpPrefix group) {
        McastRouteGroup g = null;
        if (group.isIp4()) {
            g = mrib4.get(group);
        } else if (group.isIp6() && ipv6Enabled) {
                g = mrib6.get(group);
        }
        return g;
    }

    /**
     * Find the multicast (S, G) entry if it exists.
     *
     * @param saddr the source address
     * @param gaddr the group address
     * @return The multicast source route entry if it exists, null if it does not.
     */
    public McastRouteSource findMcastSource(IpPrefix saddr, IpPrefix gaddr) {
        McastRouteGroup grp = findMcastGroup(checkNotNull(gaddr));
        if (grp == null) {
            return null;
        }
        return grp.findSource(saddr);
    }

    /**
     * This will first look up a Group entry. If no group entry was found null will
     * be returned. If the group entry has been found we will then look up the (s, g) entry.
     * If the (s, g) entry has been found, that will be returned.  If no (s, g) was found
     * the (*, g) group entry will be returned.
     *
     * @param saddr the source address
     * @param gaddr the group address
     * @return return the best matching McastRouteSource or McastRouteGroup
     */
    public McastRoute findBestMatch(IpPrefix saddr, IpPrefix gaddr) {
        McastRouteGroup grp = this.findMcastGroup(checkNotNull(gaddr));
        if (grp == null) {
            return null;
        }

        // Found a group now look for a source
        McastRouteSource src = grp.findSource(checkNotNull(saddr));
        if (src == null) {
            return grp;
        }

        return src;
    }

    /**
     * Print out the multicast route table in it's entirety.
     *
     * TODO: Eventually we will have to implement paging and how to handle large tables.
     * @return String
     */
    public String printMcastRouteTable() {
        String out = this.toString() + "\n";

        for (McastRouteGroup grp : mrib4.values()) {
            out += grp.toString() + "\n";
            for (McastRouteSource src : grp.getSources().values()) {
                out += src.toString() + "\n";
            }
        }
        return out;
    }

    /**
     * Print out a summary of groups in the MRIB.
     *
     * @return String
     */
    public String toString() {
        String out = "Mcast Route Table: ";
        out += mrib4.size() + " IPv4 Multicast Groups\n";
        if (ipv6Enabled) {
            out += mrib6.size() + " IPv6 Multicast Groups\n";
        }
        return out;
    }
}
