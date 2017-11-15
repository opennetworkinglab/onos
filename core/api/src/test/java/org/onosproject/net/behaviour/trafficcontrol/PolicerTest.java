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

import com.google.common.collect.ImmutableList;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.onosproject.TestApplicationId;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.onosproject.net.behaviour.trafficcontrol.Policer.Unit.KB_PER_SEC;
import static org.onosproject.net.behaviour.trafficcontrol.Policer.Unit.MB_PER_SEC;
import static org.onosproject.net.behaviour.trafficcontrol.TokenBucket.Action.DROP;
import static org.onosproject.net.behaviour.trafficcontrol.TokenBucket.Action.DSCP_CLASS;
import static org.onosproject.net.behaviour.trafficcontrol.TokenBucket.Action.DSCP_PRECEDENCE;
import static org.onosproject.net.behaviour.trafficcontrol.TokenBucket.Type.COMMITTED;
import static org.onosproject.net.behaviour.trafficcontrol.TokenBucket.Type.EXCESS;
import static org.onosproject.net.behaviour.trafficcontrol.TokenBucket.Type.PEAK;

/**
 * Test class for policer implementation.
 */
public class PolicerTest {

    // Fake Application Id
    private static final ApplicationId FOO_APP_ID = new TestApplicationId("foo");
    // Connect points
    private static final String SDID1 = "of:00000000000001";
    private static final DeviceId DID1 = DeviceId.deviceId(SDID1);
    // OpenFlow scheme
    private static final String OF_SCHEME = "of";
    // Policers identifiers
    private static final String SID1 = OF_SCHEME + ":" + Integer.toHexString(1);
    private static final PolicerId ID1 = PolicerId.policerId(SID1);
    private static final String SID2 = OF_SCHEME + ":" + Integer.toHexString(2);
    private static final PolicerId ID2 = PolicerId.policerId(SID2);
    private static final String SID3 = OF_SCHEME + ":" + Integer.toHexString(3);
    private static final PolicerId ID3 = PolicerId.policerId(SID3);
    private static final String SID4 = OF_SCHEME + ":" + Integer.toHexString(4);
    private static final PolicerId ID4 = PolicerId.policerId(SID4);
    private static final String SID5 = OF_SCHEME + ":" + Integer.toHexString(5);
    private static final PolicerId ID5 = PolicerId.policerId(SID5);
    private static final String SID6 = OF_SCHEME + ":" + Integer.toHexString(6);
    private static final PolicerId ID6 = PolicerId.policerId(SID6);
    private static final String SID7 = OF_SCHEME + ":" + Integer.toHexString(7);
    private static final PolicerId ID7 = PolicerId.policerId(SID7);
    private static final String SID8 = OF_SCHEME + ":" + Integer.toHexString(8);
    private static final PolicerId ID8 = PolicerId.policerId(SID8);
    private static final String SID9 = OF_SCHEME + ":" + Integer.toHexString(9);
    private static final PolicerId ID9 = PolicerId.policerId(SID9);

    /**
     * Test block traffic policer.
     */
    @Test
    public void testBlockCreation() {
        // Create a block traffic token bucket
        TokenBucket tokenBucket = DefaultTokenBucket.builder()
                .withBurstSize(0)
                .withAction(DROP)
                .withType(COMMITTED)
                .build();
        // Create a policer with above token bucket
        Policer policer = DefaultPolicer.builder()
                .forDeviceId(DID1)
                .fromApp(FOO_APP_ID)
                .withId(ID1)
                .withTokenBuckets(ImmutableList.of(tokenBucket))
                .build();
        // Assert on device id
        assertThat(policer.deviceId(), is(DID1));
        // Assert on app id
        assertThat(policer.applicationId(), is(FOO_APP_ID));
        // Assert on policer id
        assertThat(policer.policerId(), is(ID1));
        // It is not color aware
        assertFalse(policer.isColorAware());
        // Unit is Mbps
        assertThat(policer.unit(), is(MB_PER_SEC));
        // One token bucket
        assertThat(policer.tokenBuckets().size(), is(1));
        // One token bucket equals to tokenBucket
        assertTrue(policer.tokenBuckets().contains(tokenBucket));
    }

    /**
     * Test simple drop policer.
     */
    @Test
    public void testDropCreation() {
        // Create a drop traffic token bucket at 1MB/s
        TokenBucket tokenBucket = DefaultTokenBucket.builder()
                .withRate(1)
                .withAction(DROP)
                .withType(COMMITTED)
                .build();
        // Create a policer with above token bucket
        Policer policer = DefaultPolicer.builder()
                .forDeviceId(DID1)
                .fromApp(FOO_APP_ID)
                .withId(ID2)
                .withTokenBuckets(ImmutableList.of(tokenBucket))
                .build();
        // Assert on device id
        assertThat(policer.deviceId(), is(DID1));
        // Assert on app id
        assertThat(policer.applicationId(), is(FOO_APP_ID));
        // Assert on policer id
        assertThat(policer.policerId(), is(ID2));
        // It is not color aware
        assertFalse(policer.isColorAware());
        // Unit is Mbps
        assertThat(policer.unit(), is(MB_PER_SEC));
        // One token bucket
        assertThat(policer.tokenBuckets().size(), is(1));
        // One token bucket equals to tokenBucket
        assertTrue(policer.tokenBuckets().contains(tokenBucket));
    }

    /**
     * Test simple mark policer.
     */
    @Test
    public void testMarkCreation() {
        // Create a drop traffic token bucket at 1MB/s
        TokenBucket tokenBucket = DefaultTokenBucket.builder()
                .withRate(1)
                .withAction(DSCP_PRECEDENCE)
                .withDscp((short) 2)
                .withType(COMMITTED)
                .build();
        // Create a policer with above token bucket
        Policer policer = DefaultPolicer.builder()
                .forDeviceId(DID1)
                .fromApp(FOO_APP_ID)
                .withId(ID3)
                .withTokenBuckets(ImmutableList.of(tokenBucket))
                .build();
        // Assert on device id
        assertThat(policer.deviceId(), is(DID1));
        // Assert on app id
        assertThat(policer.applicationId(), is(FOO_APP_ID));
        // Assert on policer id
        assertThat(policer.policerId(), is(ID3));
        // It is not color aware
        assertFalse(policer.isColorAware());
        // Unit is Mbps
        assertThat(policer.unit(), is(MB_PER_SEC));
        // One token bucket
        assertThat(policer.tokenBuckets().size(), is(1));
        // One token bucket equals to tokenBucket
        assertTrue(policer.tokenBuckets().contains(tokenBucket));
    }

    /**
     * Test single rate three colors scenario (RFC 2697).
     */
    @Test
    public void testSingleRateThreeColors() {
        // Create token bucket for committed rate
        TokenBucket crTokenBucket = DefaultTokenBucket.builder()
                .withRate(1)
                .withAction(DSCP_PRECEDENCE)
                .withDscp((short) 2)
                .withType(COMMITTED)
                .build();
        // Create token bucket for excess rate
        TokenBucket erTokenBucket = DefaultTokenBucket.builder()
                .withRate(1)
                .withBurstSize(4 * 1500)
                .withAction(DROP)
                .withType(EXCESS)
                .build();
        // Create a policer with above token buckets
        Policer policer = DefaultPolicer.builder()
                .forDeviceId(DID1)
                .fromApp(FOO_APP_ID)
                .withId(ID4)
                // The order is important
                .withTokenBuckets(ImmutableList.of(crTokenBucket, erTokenBucket))
                .build();
        // Assert on device id
        assertThat(policer.deviceId(), is(DID1));
        // Assert on app id
        assertThat(policer.applicationId(), is(FOO_APP_ID));
        // Assert on policer id
        assertThat(policer.policerId(), is(ID4));
        // It is not color aware
        assertFalse(policer.isColorAware());
        // Unit is Mbps
        assertThat(policer.unit(), is(MB_PER_SEC));
        // Two token buckets
        assertThat(policer.tokenBuckets().size(), is(2));
        // One token bucket equals to crTokenBucket
        assertTrue(policer.tokenBuckets().contains(crTokenBucket));
        // One token bucket equals to erTokenBucket
        assertTrue(policer.tokenBuckets().contains(erTokenBucket));
    }

    /**
     * Test two rates three colors scenario (RFC 2698 and P4 meter).
     */
    @Test
    public void testTwoRatesThreeColors() {
        // Create token bucket for peak rate at 10Mb/s
        TokenBucket prTokenBucket = DefaultTokenBucket.builder()
                // (10 * 1000)/8 ---> 1250KB/s
                .withRate(1250)
                .withBurstSize(10 * 1500)
                .withAction(DROP)
                .withType(PEAK)
                .build();
        // Create token bucket for committed rate at 1Mb/s
        TokenBucket crTokenBucket = DefaultTokenBucket.builder()
                // (1 * 1000)/8 ---> 125KB/s
                .withRate(125)
                .withAction(DSCP_CLASS)
                .withDscp((short) 10)
                .withType(COMMITTED)
                .build();
        // Create a policer with above token buckets
        Policer policer = DefaultPolicer.builder()
                .forDeviceId(DID1)
                .fromApp(FOO_APP_ID)
                .withId(ID5)
                .withUnit(KB_PER_SEC)
                // The order is important
                .withTokenBuckets(ImmutableList.of(prTokenBucket, crTokenBucket))
                .build();
        // Assert on device id
        assertThat(policer.deviceId(), is(DID1));
        // Assert on app id
        assertThat(policer.applicationId(), is(FOO_APP_ID));
        // Assert on policer id
        assertThat(policer.policerId(), is(ID5));
        // It is not color aware
        assertFalse(policer.isColorAware());
        // Unit is Mbps
        assertThat(policer.unit(), is(KB_PER_SEC));
        // Two token buckets
        assertThat(policer.tokenBuckets().size(), is(2));
        // One token bucket equals to prTokenBucket
        assertTrue(policer.tokenBuckets().contains(prTokenBucket));
        // One token bucket equals to crTokenBucket
        assertTrue(policer.tokenBuckets().contains(crTokenBucket));
    }

    /**
     * Exception expected to raise when creating a policer with null params.
     */
    @Rule
    public ExpectedException exceptionNullParam = ExpectedException.none();

    /**
     * Test creation with null parameters.
     */
    @Test
    public void testNullParam() {
        // Define expected exception
        exceptionNullParam.expect(NullPointerException.class);
        // Invalid policer, device id is not defined
        DefaultPolicer.builder()
                .fromApp(FOO_APP_ID)
                .withId(ID6)
                .build();
    }

    /**
     * Exception expected to raise when creating a policer without token buckets.
     */
    @Rule
    public ExpectedException exceptionNoTokenBuckets = ExpectedException.none();

    /**
     * Test creation without token buckets.
     */
    @Test
    public void testNoTokenBuckets() {
        // Define expected exception
        exceptionNoTokenBuckets.expect(IllegalArgumentException.class);
        // Invalid policer, no token buckets
        DefaultPolicer.builder()
                .fromApp(FOO_APP_ID)
                .withId(ID7)
                .forDeviceId(DID1)
                .withTokenBuckets(ImmutableList.of())
                .build();
    }

    /**
     * Test equality between policers.
     */
    @Test
    public void testEqualilty() {
        // Create a block traffic token bucket
        TokenBucket blockTokenBucket = DefaultTokenBucket.builder()
                .withBurstSize(0)
                .withAction(DROP)
                .withType(COMMITTED)
                .build();
        // Create a mark traffic token bucket
        TokenBucket markTokenBucket = DefaultTokenBucket.builder()
                .withBurstSize(0)
                .withAction(DSCP_CLASS)
                .withDscp((short) 10)
                .withType(COMMITTED)
                .build();
        // Create first policer
        Policer policerOne = DefaultPolicer.builder()
                .forDeviceId(DID1)
                .fromApp(FOO_APP_ID)
                .withId(ID8)
                .withTokenBuckets(ImmutableList.of(blockTokenBucket))
                .build();
        // Create second policer
        Policer policerTwo = DefaultPolicer.builder()
                .forDeviceId(DID1)
                .fromApp(FOO_APP_ID)
                .withId(ID9)
                .withTokenBuckets(ImmutableList.of(markTokenBucket))
                .build();
        // Create third policer copy of one
        // Create first policer
        Policer policerThree = DefaultPolicer.builder()
                .forDeviceId(DID1)
                .fromApp(FOO_APP_ID)
                .withId(ID8)
                .withTokenBuckets(ImmutableList.of(blockTokenBucket))
                .build();
        // One and Three are equal
        assertEquals(policerOne, policerThree);
        // Two is different due to the different id
        assertNotEquals(policerOne, policerTwo);
        assertNotEquals(policerThree, policerTwo);
    }

}
