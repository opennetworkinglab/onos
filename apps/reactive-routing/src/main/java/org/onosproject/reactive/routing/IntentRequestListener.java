/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.reactive.routing;

import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onosproject.net.ConnectPoint;

/**
 * An interface to process intent requests.
 */
public interface IntentRequestListener {

    /**
     * Sets up connectivity for packet from Internet to a host in local
     * SDN network.
     *
     * @param dstIpAddress IP address of destination host in local SDN network
     */
    void setUpConnectivityInternetToHost(IpAddress dstIpAddress);

    /**
     * Sets up the connectivity for two hosts in local SDN network.
     *
     * @param dstIpAddress the destination IP address
     * @param srcIpAddress the source IP address
     * @param srcMacAddress the source MAC address
     * @param srcConnectPoint the connectPoint of the source host
     */
    void setUpConnectivityHostToHost(IpAddress dstIpAddress,
                                     IpAddress srcIpAddress,
                                     MacAddress srcMacAddress,
                                     ConnectPoint srcConnectPoint);

    /**
     * Sets up connectivity for packet from a local host to the Internet.
     *
     * @param hostIp IP address of the local host
     * @param prefix external IP prefix that the host is talking to
     * @param nextHopIpAddress IP address of the next hop router for the prefix
     */
    void setUpConnectivityHostToInternet(IpAddress hostIp, IpPrefix prefix,
                                                IpAddress nextHopIpAddress);

    /**
     * Adds one new ingress connect point into ingress points of an existing
     * intent and resubmits the new intent.
     * <p>
     * If there is already an intent for an IP prefix in the system, we do not
     * need to create a new one, we only need to update this existing intent by
     * adding more ingress points.
     * </p>
     *
     * @param ipPrefix the IP prefix used to search the existing
     *        MultiPointToSinglePointIntent
     * @param ingressConnectPoint the ingress connect point to be added into
     *        the exiting intent
     */
    void updateExistingMp2pIntent(IpPrefix ipPrefix,
                                  ConnectPoint ingressConnectPoint);

    /**
     * Checks whether there is a MultiPointToSinglePointIntent in memory for a
     * given IP prefix.
     *
     * @param ipPrefix the IP prefix used to search the existing
     *        MultiPointToSinglePointIntent
     * @return true if there is a MultiPointToSinglePointIntent, otherwise false
     */
    boolean mp2pIntentExists(IpPrefix ipPrefix);

}
