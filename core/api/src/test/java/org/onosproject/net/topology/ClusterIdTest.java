/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onosproject.net.topology;

import com.google.common.testing.EqualsTester;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.onosproject.net.topology.ClusterId.clusterId;

/**
 * Test of the cluster ID.
 */
public class ClusterIdTest {

    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(clusterId(1), clusterId(1))
                .addEqualityGroup(clusterId(3), clusterId(3)).testEquals();
    }

    @Test
    public void basics() {
        assertEquals("incorrect index", 123, clusterId(123).index());
    }

}
