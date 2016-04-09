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

package org.onosproject.pcepio.types;

import com.google.common.testing.EqualsTester;
import org.junit.Test;

/**
 * Equality test for PcepNaiIpv4Adjacency.
 */
public class PcepNaiIpv4AdjacencyTest {

    private final PcepNaiIpv4Adjacency obj1 = PcepNaiIpv4Adjacency.of(2, 16);
    private final PcepNaiIpv4Adjacency sameAsObj1 = PcepNaiIpv4Adjacency.of(2, 16);
    private final PcepNaiIpv4Adjacency obj2 = PcepNaiIpv4Adjacency.of(3, 16);

    @Test
    public void basics() {
        new EqualsTester().addEqualityGroup(obj1, sameAsObj1).addEqualityGroup(obj2).testEquals();
    }

}
