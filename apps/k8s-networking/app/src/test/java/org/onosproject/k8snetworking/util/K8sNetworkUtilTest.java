/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.k8snetworking.util;

import org.junit.Test;
import org.onlab.packet.IpAddress;

import java.util.Set;

import static junit.framework.TestCase.assertEquals;
import static org.onosproject.k8snetworking.util.K8sNetworkingUtil.getSubnetIps;

/**
 * Unit tests for kubernetes networking utils.
 */
public final class K8sNetworkUtilTest {

    /**
     * Tests the getSubnetIps method.
     */
    @Test
    public void testGetSubnetIps() {
        String bClassCidr = "10.10.0.0/16";
        Set<IpAddress> bClassIps = getSubnetIps(bClassCidr);
        assertEquals(((Double) Math.pow(2, 16)).intValue() - 4, bClassIps.size());

        String cClassCidr = "10.10.10.0/24";
        Set<IpAddress> cClassIps = getSubnetIps(cClassCidr);
        assertEquals(((Double) Math.pow(2, 8)).intValue() - 4, cClassIps.size());

        String dClassCidr = "10.10.10.10/32";
        Set<IpAddress> dClassIps = getSubnetIps(dClassCidr);
        assertEquals(0, dClassIps.size());
    }
}
