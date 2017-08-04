/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.driver.extensions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.Test;
import org.onlab.packet.MacAddress;

import com.google.common.testing.EqualsTester;

/**
 * Unit tests for NiciraEncapEthDstTest class.
 */
public class NiciraEncapEthDstTest {

    private final MacAddress mac1 = MacAddress.valueOf("fa:16:3e:da:45:23");
    private final MacAddress mac2 = MacAddress.valueOf("fa:16:3e:f3:d1:fe");

    /**
     * Checks the operation of equals() methods.
     */
    @Test
    public void testEquals() {
        final NiciraEncapEthDst encapEthDst1 = new NiciraEncapEthDst(mac1);
        final NiciraEncapEthDst sameAsEncapEthDst1 = new NiciraEncapEthDst(mac1);
        final NiciraEncapEthDst encapEthDst2 = new NiciraEncapEthDst(mac2);

        new EqualsTester().addEqualityGroup(encapEthDst1, sameAsEncapEthDst1).addEqualityGroup(encapEthDst2)
        .testEquals();
    }

    /**
     * Checks the construction of a NiciraEncapEthDstTest object.
     */
    @Test
    public void testConstruction() {
        final NiciraEncapEthDst encapEthDst1 = new NiciraEncapEthDst(mac1);
        assertThat(encapEthDst1, is(notNullValue()));
        assertThat(encapEthDst1.encapEthDst(), is(mac1));
    }
}
