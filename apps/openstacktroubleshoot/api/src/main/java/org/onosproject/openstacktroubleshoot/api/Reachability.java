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
package org.onosproject.openstacktroubleshoot.api;

import org.onlab.packet.IpAddress;

/**
 * Reachbility interface which stores VM to VM connectivity.
 */
public interface Reachability {

    /**
     * Source IP address.
     *
     * @return source IP address
     */
    IpAddress srcIp();

    /**
     * Destination IP address.
     *
     * @return destination IP address
     */
    IpAddress dstIp();

    /**
     * Indicates whether the given source and destination VMs are reachable.
     *
     * @return reachability indicator
     */
    boolean isReachable();

    /**
     * Builder of new reachability object.
     */
    interface Builder {

        /**
         * Returns reachability builder with supplied IP address.
         *
         * @param srcIp source IP address
         * @return reachability builder
         */
        Builder srcIp(IpAddress srcIp);

        /**
         * Returns reachability builder with supplied IP address.
         *
         * @param dstIp destination IP address
         * @return reachability builder
         */
        Builder dstIp(IpAddress dstIp);

        /**
         * Returns reachability builder with supplied reachability flag.
         *
         * @param isReachable reachability flag
         * @return reachability builder
         */
        Builder isReachable(boolean isReachable);

        /**
         * Builds an immutable reachability instance.
         *
         * @return reachability instance
         */
        Reachability build();
    }
}
