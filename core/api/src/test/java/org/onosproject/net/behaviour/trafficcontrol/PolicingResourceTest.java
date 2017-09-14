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

package org.onosproject.net.behaviour.trafficcontrol;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.onlab.junit.ImmutableClassChecker;
import org.onosproject.net.ConnectPoint;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.*;

/**
 * Test class for PolicingResource.
 */
public class PolicingResourceTest {

    // Connectpoints
    private static final String SCP1 = "of:00000000000001/1";
    private static final String SCP2 = "of:00000000000001/2";
    private static final ConnectPoint CP1 = ConnectPoint.deviceConnectPoint(SCP1);
    private static final ConnectPoint CP2 = ConnectPoint.deviceConnectPoint(SCP2);
    // OpenFlow scheme
    private static final String OF_SCHEME = "of";
    // Policer identifier
    private static final String SID = OF_SCHEME + ":" + Integer.toHexString(1);
    private static final PolicerId PID = PolicerId.policerId(SID);

    /**
     * Test policing resource creation.
     */
    @Test
    public void testCreation() {
        // Create a new policing resource
        PolicingResource policingResource = new PolicingResource(PID, CP1);
        // Verify proper creation
        assertThat(policingResource, notNullValue());
        assertThat(policingResource.policerId(), is(PID));
        assertThat(policingResource.connectPoint(), is(CP1));
    }

    /**
     * Exception expected to raise when creating policing resource with null id.
     */
    @Rule
    public ExpectedException exceptionNullId = ExpectedException.none();

    /**
     * Test wrong creation of a policing resource.
     */
    @Test
    public void testNullIdCreation() {
        // Define expected exception
        exceptionNullId.expect(NullPointerException.class);
        // Create wrong policing resource
        new PolicingResource(null, CP1);
    }

    /**
     * Test equality between policing resources.
     */
    @Test
    public void testEqualilty() {
        // Create two identical resources
        PolicingResource one = new PolicingResource(PID, CP1);
        PolicingResource copyOfOne = new PolicingResource(PID, CP1);
        // Verify equality
        assertEquals(one, copyOfOne);
        // Create a different resource
        PolicingResource two = new PolicingResource(PID, CP2);
        // Verify not equals
        assertNotEquals(two, one);
        assertNotEquals(two, copyOfOne);
    }

    /**
     * Tests immutability of PolicingResource.
     */
    @Test
    public void testImmutability() {
        ImmutableClassChecker.assertThatClassIsImmutable(PolicingResource.class);
    }

}
