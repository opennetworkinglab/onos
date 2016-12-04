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
package org.onosproject.teyang.utils.topology;

import org.onosproject.tetopology.management.api.Network;
import org.onosproject.tetopology.management.api.Networks;

/**
 * Builds a sample Topology, which consists of two Nodes, one link,
 * and each node has two termination points.
 */
public final class DefaultBuilder {

    private static final String HUAWEI_NETWORK_NEW = "HUAWEI_NETWORK_NEW";
    private static final String HUAWEI_ROADM_1 = "HUAWEI_ROADM_1";
    private static final String CLIENT1_NODE1 = "CLIENT1_NODE1";
    private static final String LINE1_NODE1 = "LINE1_NODE1";
    private static final String NODE1_IP = "10.11.12.33";
    private static final String HUAWEI_ROADM_2 = "HUAWEI_ROADM_2";
    private static final String CLIENT1_NODE2 = "CLIENT1_NODE2";
    private static final String LINE1_NODE2 = "LINE1_NODE2";
    private static final String NODE2_IP = "10.11.12.34";
    private static final String LINK1FORNETWORK1 = "LINK1FORNETWORK1";
    private static final String HUAWEI_TE_TOPOLOGY_NEW = "HUAWEI_TE_TOPOLOGY_NEW";

    // no instantiation
    private DefaultBuilder() {
    }

    /**
     * Returns a sample TeSubsystem Networks object.
     *
     * @return the Networks object
     */
    public static Networks sampleTeSubsystemNetworksBuilder() {
        //TODO: implementation will be submitted as a separate review.
        return null;
    }

    /**
     * Returns a sample TeSubsystem Network object.
     *
     * @return the Network object
     */
    public static Network sampleTeSubsystemNetworkBuilder() {
        //TODO: implementation will be submitted as a separate review.
        return null;
    }
}
