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

import org.onlab.packet.IpPrefix;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.SinglePointToMultiPointIntent;

import java.util.Set;

/**
 * This McastRouteBase interface is implemented by the McastRouteBase class which
 * in turn acts as the base class for both the McastRouteGroup and McastRouteSource.
 */
interface McastRoute {

    /**
     * Gets the group addresses.
     *
     * @return group address
     */
    public IpPrefix getGaddr();

    /**
     * Gets the source address.
     *
     * @return the source address
     */
    public IpPrefix getSaddr();

    /**
     * Determines if this is an IPv4 multicast route.
     *
     * @return true if it is an IPv4 route
     */
    public boolean isIp4();

    /**
     * Determines if this is an IPv6 multicast route.
     *
     * @return true if it is an IPv6 route
     */
    public boolean isIp6();

    /**
     * Add the ingress ConnectPoint with a ConnectPoint.
     *
     * @param ingress ingress point
     */
    public void addIngressPoint(ConnectPoint ingress);

    /**
     * Add the ingress Connect Point using. ..
     *
     * @param deviceId device ID
     * @param portNum port number
     */
    public void addIngressPoint(String deviceId, long portNum);

    /**
     * Get the ingress connect point.
     *
     * @return the ingress connect point
     */
    public ConnectPoint getIngressPoint();

    /**
     * Add an egress connect point.
     *
     * @param member the egress ConnectPoint to be added
     */
    public void addEgressPoint(ConnectPoint member);

    /**
     * Add an egress connect point.
     *
     * @param deviceId the device ID of the connect point
     * @param portNum the port number of the connect point
     */
    public void addEgressPoint(String deviceId, long portNum);

    /**
     * Get the egress connect points.
     *
     * @return a set of egress connect points
     */
    public Set<ConnectPoint> getEgressPoints();

    /**
     * Increment the punt count.
     */
    public void incrementPuntCount();

    /**
     * Get the punt count.
     *
     * @return the punt count
     */
    public int getPuntCount();

    /**
     * Have the McastIntentManager create an intent, attempt to
     * install the intent and then save the key.
     */
    public void setIntent();

    /**
     * Set the Intent key.
     *
     * @param intent intent
     */
    public void setIntent(SinglePointToMultiPointIntent intent);

    /**
     * Withdraw the intent if it has been installed.
     */
    public void withdrawIntent();

    /**
     * Get the intent key.
     *
     * @return the intentKey
     */
    public Key getIntentKey();

    /**
     * Pretty print the the route.
     *
     * @return a pretty string
     */
    public String toString();
}