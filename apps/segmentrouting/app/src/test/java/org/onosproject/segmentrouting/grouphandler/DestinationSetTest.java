/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.segmentrouting.grouphandler;

import org.junit.Before;
import org.junit.Test;
import org.onosproject.net.DeviceId;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DestinationSetTest {
    DestinationSet ds1, ds2, ds3, ds4, ds5, ds6;
    DeviceId d201, d202;
    int el201, el202;

    @Before
    public void setUp() {
        d201 = DeviceId.deviceId("of:0000000000000201");
        d202 = DeviceId.deviceId("of:0000000000000202");
        el201 = 201;
        el202 = 202;
        ds1 = new DestinationSet(false, false, d201);
        ds2 = new DestinationSet(false, false, el201, d201);
        ds3 = new DestinationSet(false, false, el201, d201, el202, d202);
        ds4 = new DestinationSet(false, false,
                                 DestinationSet.NO_EDGE_LABEL, d201,
                                 DestinationSet.NO_EDGE_LABEL, d202);
        ds5 = new DestinationSet(true, false, d201); // not-bos
        ds6 = new DestinationSet(false, true, el201, d201); // swap group
    }

    @Test
    public void testIsValid() {
        assertTrue(!ds1.notBos());
        assertTrue(!ds1.swap());
        assertTrue(ds1.getEdgeLabel(d201) == DestinationSet.NO_EDGE_LABEL);
        assertTrue(ds1.getDestinationSwitches().size() == 1);

        assertTrue(!ds2.notBos());
        assertTrue(!ds2.swap());
        assertTrue(ds2.getEdgeLabel(d201) == el201);
        assertTrue(ds2.getDestinationSwitches().size() == 1);

        assertTrue(!ds3.notBos());
        assertTrue(!ds3.swap());
        assertTrue(ds3.getEdgeLabel(d201) == el201);
        assertTrue(ds3.getEdgeLabel(d202) == el202);
        assertTrue(ds3.getDestinationSwitches().size() == 2);

        assertTrue(!ds4.notBos());
        assertTrue(!ds4.swap());
        assertTrue(ds4.getEdgeLabel(d201) == DestinationSet.NO_EDGE_LABEL);
        assertTrue(ds4.getEdgeLabel(d202) == DestinationSet.NO_EDGE_LABEL);
        assertTrue(ds4.getDestinationSwitches().size() == 2);

        assertFalse(ds1.equals(ds2));
        assertFalse(ds1.equals(ds4));
        assertFalse(ds3.equals(ds4));
        assertFalse(ds2.equals(ds3));
        assertFalse(ds1.equals(ds3));

        assertFalse(ds1.equals(ds5));
        assertFalse(ds2.equals(ds6));
    }


    @Test
    public void testOneDestinationWithoutLabel() {
        DestinationSet testds = new DestinationSet(false, false, d201);
        assertTrue(testds.equals(ds1)); // match

        testds = new DestinationSet(true, false, d201);
        assertFalse(testds.equals(ds1)); // wrong notBos
        assertTrue(testds.equals(ds5)); // correct notBos

        testds = new DestinationSet(false, false, d202);
        assertFalse(testds.equals(ds1)); //wrong device

        testds = new DestinationSet(false, false, el201, d201);
        assertFalse(testds.equals(ds1)); // wrong label

        testds = new DestinationSet(false, false, -1, d201, -1, d202);
        assertFalse(testds.equals(ds1)); // 2-devs should not match

        testds = new DestinationSet(false, true, d201);
        assertFalse(testds.equals(ds1)); // wrong swap
        assertFalse(testds.equals(ds6)); // wrong label

        testds = new DestinationSet(false, true, el201, d201);
        assertTrue(testds.equals(ds6)); // correct swap

        testds = new DestinationSet(false, true, DestinationSet.NO_EDGE_LABEL, d201);
        assertFalse(testds.equals(ds6)); // wrong label

        testds = new DestinationSet(true, true, el201, d201);
        assertFalse(testds.equals(ds6)); // wrong notbos
    }



    @Test
    public void testOneDestinationWithLabel() {
        DestinationSet testds = new DestinationSet(false, false, 203, d202);
        assertFalse(testds.equals(ds2)); //wrong label

        testds = new DestinationSet(true, false, 201, d201);
        assertFalse(testds.equals(ds2)); // wrong notBos

        testds = new DestinationSet(false, false, 201, d202);
        assertFalse(testds.equals(ds2)); //wrong device

        testds = new DestinationSet(false, false, 201,
                                    DeviceId.deviceId("of:0000000000000201"));
        assertTrue(testds.equals(ds2)); // match

        testds = new DestinationSet(false, false, d201);
        assertFalse(testds.equals(ds2)); // wrong label

        testds = new DestinationSet(false, false, el201, d201, el202, d202);
        assertFalse(testds.equals(ds1)); // 2-devs should not match
    }

    @Test
    public void testDestPairWithLabel() {
        DestinationSet testds = new DestinationSet(false, false, el201, d201, el202, d202);
        assertTrue(testds.equals(ds3)); // match same switches, same order
        assertTrue(testds.hashCode() == ds3.hashCode());

        testds = new DestinationSet(false, false, el202, d202, el201, d201);
        assertTrue(testds.equals(ds3)); // match same switches, order reversed
        assertTrue(testds.hashCode() == ds3.hashCode());

        testds = new DestinationSet(false, false, el202, d202);
        assertFalse(testds.equals(ds3)); // one less switch should not match
        assertFalse(testds.hashCode() == ds3.hashCode());

        testds = new DestinationSet(false, false, el201, d201);
        assertFalse(testds.equals(ds3)); // one less switch should not match
        assertFalse(testds.hashCode() == ds3.hashCode());

        testds = new DestinationSet(false, false, el201, d201, 0, DeviceId.NONE);
        assertFalse(testds.equals(ds3)); // one less switch should not match
        assertFalse(testds.hashCode() == ds3.hashCode());

        testds = new DestinationSet(false, false, el201, d202, el201, d201);
        assertFalse(testds.equals(ds3)); // wrong labels
        assertFalse(testds.hashCode() == ds3.hashCode());

        testds = new DestinationSet(true, false, el202, d202, el201, d201);
        assertFalse(testds.equals(ds3)); // wrong not bos
        assertFalse(testds.hashCode() == ds3.hashCode());

        testds = new DestinationSet(false, false, el202, d202, el201, d202);
        assertFalse(testds.equals(ds3)); // wrong device
        assertFalse(testds.hashCode() == ds3.hashCode());

        testds = new DestinationSet(false, false,
                                    el202, DeviceId.deviceId("of:0000000000000205"),
                                    el201, d201);
        assertFalse(testds.equals(ds3)); // wrong device
        assertFalse(testds.hashCode() == ds3.hashCode());
    }

    @Test
    public void testDestPairWithoutLabel() {
        DestinationSet testds = new DestinationSet(false, false, -1, d201, -1, d202);
        assertTrue(testds.equals(ds4)); // match same switches, same order
        assertTrue(testds.hashCode() == ds4.hashCode());

        testds = new DestinationSet(false, false, -1, d202, -1, d201);
        assertTrue(testds.equals(ds4)); // match same switches, order reversed
        assertTrue(testds.hashCode() == ds4.hashCode());

        testds = new DestinationSet(false, false, -1, d202);
        assertFalse(testds.equals(ds4)); // one less switch should not match
        assertFalse(testds.hashCode() == ds4.hashCode());

        testds = new DestinationSet(false, false, -1, d201);
        assertFalse(testds.equals(ds4)); // one less switch should not match
        assertFalse(testds.hashCode() == ds4.hashCode());

        testds = new DestinationSet(false, false, -1, d201, 0, DeviceId.NONE);
        assertFalse(testds.equals(ds4)); // one less switch should not match
        assertFalse(testds.hashCode() == ds4.hashCode());

        testds = new DestinationSet(false, false, el201, d201, -1, d202);
        assertFalse(testds.equals(ds4)); // wrong labels
        assertFalse(testds.hashCode() == ds4.hashCode());

        testds = new DestinationSet(true, false, -1, d202, -1, d201);
        assertFalse(testds.equals(ds4)); // wrong mpls set
        assertFalse(testds.hashCode() == ds4.hashCode());

        testds = new DestinationSet(false, false, -1, d202, -1, d202);
        assertFalse(testds.equals(ds4)); // wrong device
        assertFalse(testds.hashCode() == ds4.hashCode());

        testds = new DestinationSet(false, false,
                                    -1, DeviceId.deviceId("of:0000000000000205"),
                                    -1, d201);
        assertFalse(testds.equals(ds4)); // wrong device
        assertFalse(testds.hashCode() == ds4.hashCode());
    }

}
