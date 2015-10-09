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

import static com.google.common.base.Preconditions.checkNotNull;

import org.onlab.packet.IpPrefix;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.intent.SinglePointToMultiPointIntent;
import org.onosproject.net.intent.Key;

import java.util.Set;
import java.util.HashSet;

/**
 * McastRouteBase base class for McastRouteGroup and McastRouteSource.
 */
public class McastRouteBase implements McastRoute {
    protected final IpPrefix gaddr;
    protected final IpPrefix saddr;

    protected McastConnectPoint ingressPoint;
    protected Set<McastConnectPoint> egressPoints;

    protected boolean isGroup = false;

    protected boolean dirty = false;

    /**
     * How may times has this packet been punted.
     */
    private int puntCount = 0;

    /**
     * If the intentKey is null that means no intent has
     * been installed.
     */
    protected Key intentKey = null;

    /**
     * Create a multicast route. This is the parent class for both the Group
     * and the source.
     *
     * @param saddr source address
     * @param gaddr multicast group address
     */
    public McastRouteBase(String saddr, String gaddr) {
        this.gaddr = IpPrefix.valueOf(checkNotNull(gaddr));
        if (saddr == null || saddr.equals("*")) {
            this.saddr = IpPrefix.valueOf(0, 0);
        } else {
            this.saddr = IpPrefix.valueOf(checkNotNull(gaddr));
        }
        this.init();
    }

    /**
     * Create a multicast group table entry.
     * @param gaddr multicast group address
     */
    public McastRouteBase(String gaddr) {
        this("*", gaddr);
    }

    /**
     * Set the source and group address value of a (*, G) group.
     *
     * @param gpfx the group prefix address
     */
    public McastRouteBase(IpPrefix gpfx) {
        this(IpPrefix.valueOf(0, 0), gpfx);
    }

    /**
     * Create a multicast route constructor.
     *
     * @param saddr source address
     * @param gaddr group address
     */
    public McastRouteBase(IpPrefix saddr, IpPrefix gaddr) {
        this.saddr = checkNotNull(saddr);
        this.gaddr = checkNotNull(gaddr);

        this.init();
    }

    private void init() {
        this.isGroup = (this.saddr.prefixLength() == 0);
        this.ingressPoint = null;
        this.egressPoints = new HashSet();
    }

    /**
     * Get the multicast group address.
     *
     * @return the multicast group address
     */
    @Override
    public IpPrefix getGaddr() {
        return gaddr;
    }

    /**
     * Get the multicast source address.
     *
     * @return the multicast source address
     */
    @Override
    public IpPrefix getSaddr() {
        return saddr;
    }

    /**
     * Is this an IPv4 multicast route.
     *
     * @return true if it is an IPv4 route
     */
    @Override
    public boolean isIp4() {
        return gaddr.isIp4();
    }

    /**
     * Is this an IPv6 multicast route.
     *
     * @return true if it is an IPv6 route
     */
    @Override
    public boolean isIp6() {
        return gaddr.isIp6();
    }

    /**
     * Is this a multicast group route?
     *
     * @return true if it is a multicast group route.
     */
    public boolean isGroup() {
        return isGroup;
    }

    /**
     * @return true if this is (S, G) false if it (*, G).
     */
    public boolean isSource() {
        return (!isGroup);
    }

    /**
     * Get the dirty state.
     *
     * @return whether this route is dirty or not.
     */
    public boolean getDirty() {
        return this.dirty;
    }

    /**
     * Set the dirty state to indicate that something changed.
     * This may require an update to the flow tables (intents).
     *
     * @param dirty set the dirty bit
     */
    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    /**
     * Add an ingress point to this route.
     *
     * @param ingress incoming connect point
     * @return whether ingress has been added, only add if ingressPoint is null
     */
    public boolean addIngressPoint(ConnectPoint ingress) {

        // Do NOT add the ingressPoint if it is not null.
        if (this.ingressPoint != null) {
            // TODO: Log an warning.
            return false;
        }
        this.ingressPoint = new McastConnectPoint(checkNotNull(ingress));
        setDirty(true);
        return true;
    }

    /**
     * Add or modify the ingress connect point.
     *
     * @param connectPoint string switch device Id
     * @return whether ingress has been added, only add if ingressPoint is null
     */
    public boolean addIngressPoint(String connectPoint) {

        if (this.ingressPoint != null) {
            // TODO: log a warning.
            return false;
        }
        ConnectPoint cp = ConnectPoint.deviceConnectPoint(checkNotNull(connectPoint));
        return this.addIngressPoint(cp);
    }

    /**
     * Get the ingress McastConnectPoint.
     *
     * @return the ingress McastConnectPoint
     */
    public McastConnectPoint getIngressPoint() {
        return this.ingressPoint;
    }

    /**
     * Add an egress McastConnectPoint.
     *
     * @param cp egress connect point
     * @return return the McastConnectPoint
     */
    public McastConnectPoint addEgressPoint(ConnectPoint cp) {
        McastConnectPoint mcp = this.findEgressConnectPoint(cp);
        if (mcp == null) {
            mcp = new McastConnectPoint(checkNotNull(cp));
            egressPoints.add(mcp);
            setDirty(true);
        }
        return mcp;
    }

    /**
     * Add an egress connect point from a string.
     *
     * @param connectPoint string representing a connect point
     * @return the MulticastConnectPoint
     */
    public McastConnectPoint addEgressPoint(String connectPoint) {
        checkNotNull(connectPoint);
        return this.addEgressPoint(ConnectPoint.deviceConnectPoint(connectPoint));
    }

    /**
     * Add an egress McastConnectPoint.
     *
     * @param cp the egress connect point
     * @param interest the source of interest for mcast traffic
     */
    public McastConnectPoint addEgressPoint(ConnectPoint cp, McastConnectPoint.JoinSource interest) {
        checkNotNull(cp);
        checkNotNull(interest);
        McastConnectPoint mcp = this.addEgressPoint(cp);
        if (mcp != null) {
            mcp.interest.add(interest);
            setDirty(true);
        }
        return mcp;
    }

    /**
     * Remove an egress from McastConnectPoint.
     *
     * @param connectPoint the egress connect point
     * @return boolean result of removal
     */
    public boolean removeEgressPoint(String connectPoint) {
        checkNotNull(connectPoint);
        return this.removeEgressPoint(ConnectPoint.deviceConnectPoint(connectPoint));
    }

    /**
     * Remove an egress from McastConnectPoint.
     *
     * @param cp the egress connect point
     * @return boolean result of removal
     */
    public boolean removeEgressPoint(ConnectPoint cp) {
        boolean removed = false;
        McastConnectPoint mcp = this.findEgressConnectPoint(checkNotNull(cp));
        if (mcp != null) {
            removed = egressPoints.remove(mcp);
            setDirty(true);
        }
        return removed;
    }

    /**
     * Add an egress McastConnectPoint.
     *
     * @param cpstr deviceId/port of the connect point
     */
    public McastConnectPoint addEgressPoint(String cpstr, McastConnectPoint.JoinSource interest) {
        checkNotNull(cpstr);
        checkNotNull(interest);
        return this.addEgressPoint(ConnectPoint.deviceConnectPoint(cpstr), interest);
    }

    /**
     * Get egress connect points for the route.
     *
     * @return Set of egress connect points
     */
    public Set<McastConnectPoint> getEgressPoints() {
        return egressPoints;
    }

    /**
     * Get egress McastConnectPoints points as ConnectPoints for intent system.
     *
     * @return Set of egress ConnectPoints
     */
    public Set<ConnectPoint> getEgressConnectPoints() {
        Set<ConnectPoint> cps = new HashSet<ConnectPoint>();

        for (McastConnectPoint mcp : egressPoints) {
            cps.add(mcp.getConnectPoint());
        }
        return cps;
    }

    /**
     * Find the Multicast Connect Point that contains the ConnectPoint.
     *
     * @param cp the regular ConnectPoint to match
     * @return the McastConnectPoint that contains cp or null if not found.
     */
    public McastConnectPoint findEgressConnectPoint(ConnectPoint cp) {
        for (McastConnectPoint mcp : this.egressPoints) {
            if (mcp.getConnectPoint().equals(cp)) {
                return mcp;
            }
        }
        return null;
    }

    /**
     * Remove specified interest from the given ConnectPoint.
     *
     * @param mcp connect point.
     * @param interest the protocol interested in this multicast stream
     * @return true if removed, false otherwise
     */
    public boolean removeInterest(McastConnectPoint mcp, McastConnectPoint.JoinSource interest) {
        checkNotNull(mcp);
        if (mcp.interest.contains(interest)) {
            mcp.interest.remove(interest);
            setDirty(true);
            return true;
        }
        return false;
    }

    /**
     * Get the number of times the packet has been punted.
     *
     * @return the punt count
     */
    @Override
    public int getPuntCount() {
        return puntCount;
    }

    /**
     * Increment the punt count.
     *
     * TODO: we need to handle wrapping.
     */
    @Override
    public void incrementPuntCount() {
        puntCount++;
    }

    /**
     * Have the McastIntentManager create and set the intent, then save the intent key.
     *
     * If we already have an intent, we will first withdraw the existing intent and
     * replace it with a new one.  This will support the case where the ingress connectPoint
     * or group of egress connectPoints change.
     */
    @Override
    public void setIntent() {
        if (this.intentKey != null) {
            this.withdrawIntent();
        }
        McastIntentManager im = McastIntentManager.getInstance();
        SinglePointToMultiPointIntent intent = im.setIntent(this);
        this.intentKey = intent.key();
    }

    /**
     * Set the Intent key.
     *
     * @param intent the multicast intent
     */
    @Override
    public void setIntent(SinglePointToMultiPointIntent intent) {
        intentKey = intent.key();
    }

    /**
     * Get the intent key represented by this route.
     *
     * @return intentKey
     */
    @Override
    public Key getIntentKey() {
        return this.intentKey;
    }


    /**
     * Withdraw the intent and set the key to null.
     */
    @Override
    public void withdrawIntent() {
        if (intentKey == null) {
            // nothing to withdraw
            return;
        }
        McastIntentManager im = McastIntentManager.getInstance();
        im.withdrawIntent(this);
        this.intentKey = null;
    }

    /**
     * Pretty Print this Multicast Route.  Works for McastRouteSource and McastRouteGroup.
     *
     * @return pretty string of the multicast route
     */
    @Override
    public String toString() {
        String out = String.format("(%s, %s)\n\t",
                saddr.toString(), gaddr.toString());

        out += "intent: ";
        out += (intentKey == null) ? "not installed" : this.intentKey.toString();
        out += "\n\tingress: ";
        out += (ingressPoint == null) ? "NULL" : ingressPoint.getConnectPoint().toString();
        out += "\n\tegress: {\n";
        if (egressPoints != null && !egressPoints.isEmpty()) {
            for (McastConnectPoint eg : egressPoints) {
                out += "\t\t" + eg.getConnectPoint().toString() + "\n";
            }
        }
        out += ("\t}\n");
        out += ("\tpunted: " + this.getPuntCount() + "\n");
        return out;
    }
}
