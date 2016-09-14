/*
 * Copyright 2016 Open Networking Laboratory
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

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.onosproject.teyang.api.OperationType;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network
            .rev20151208.ietfnetwork.networks.Network;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.networks.network.node.AugmentedNwNode;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.TeAdminStatus;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.teadminstatus.TeAdminStatusEnum;


/**
 * Unit tests for converter functions.
 */
public class ConverterTest {

    Network output;

    @Before
    public void setUp() {
        output = NetworkConverter.teSubsystem2YangNetwork(
                        DefaultBuilder.sampleTeSubsystemNetworkBuilder(),
                        OperationType.NONE);
    }

    @Test
    public void basics() {
        assertEquals("Wrong networkId",
                     output.networkId().uri().string(),
                     "HUAWEI_NETWORK_NEW");
        assertEquals("Wrong 1st nodeId",
                     output.node().get(0).nodeId().uri().string(),
                     "HUAWEI_ROADM_1");
        assertEquals("Wrong 2dn nodeId",
                     output.node().get(1).nodeId().uri().string(),
                     "HUAWEI_ROADM_2");
        AugmentedNwNode augmentedNode = (AugmentedNwNode) output.node().get(0)
                .yangAugmentedInfo(AugmentedNwNode.class);

        assertEquals("Wrong adminStatus",
                     augmentedNode.te().config().teNodeAttributes().adminStatus(),
                     TeAdminStatus.of(TeAdminStatusEnum.UP));
    }

}
