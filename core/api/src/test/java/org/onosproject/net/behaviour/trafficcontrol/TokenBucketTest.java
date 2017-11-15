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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.*;
import static org.onosproject.net.behaviour.trafficcontrol.TokenBucket.Action.DROP;
import static org.onosproject.net.behaviour.trafficcontrol.TokenBucket.Action.DSCP_CLASS;
import static org.onosproject.net.behaviour.trafficcontrol.TokenBucket.Action.DSCP_PRECEDENCE;
import static org.onosproject.net.behaviour.trafficcontrol.TokenBucket.Type.COMMITTED;
import static org.onosproject.net.behaviour.trafficcontrol.TokenBucket.Type.EXCESS;
import static org.onosproject.net.behaviour.trafficcontrol.TokenBucket.Type.PEAK;

/**
 * Test class for TokenBucket.
 */
public class TokenBucketTest {

    // Test rate
    private static final long RATE = 1;
    // Test dscp drop precedence
    private static final short DSCP_PREC = 2;
    // Test dscp class
    private static final short DSCP_CL = 250;
    // Test wrong dscp
    private static final short WRONG_DSCP = -1;

    /**
     * Test creation of a drop token bucket.
     */
    @Test
    public void testDropCreation() {
        // Create a drop token bucket
        TokenBucket drop = DefaultTokenBucket.builder()
                .withRate(RATE)
                .withAction(DROP)
                .withType(COMMITTED)
                .build();
        // Not null
        assertThat(drop, notNullValue());
        // Rate should be equal to RATE
        assertThat(drop.rate(), is(RATE));
        // Burst size should be equal to 2xMTU
        assertThat(drop.burstSize(), is(2 * 1500L));
        // Action should be drop
        assertThat(drop.action(), is(DROP));
        // For committed rate
        assertThat(drop.type(), is(COMMITTED));
    }

    /**
     * Test creation of a dscp precedence token bucket.
     */
    @Test
    public void testDscpPrecCreation() {
        // Create a dscp precedence token bucket
        TokenBucket mark = DefaultTokenBucket.builder()
                .withRate(RATE)
                .withAction(DSCP_PRECEDENCE)
                .withBurstSize(6 * 1500)
                .withDscp(DSCP_PREC)
                .withType(EXCESS)
                .build();
        // Not null
        assertThat(mark, notNullValue());
        // Rate should be equal to RATE
        assertThat(mark.rate(), is(RATE));
        // Burst size should be equal to 6xMTU
        assertThat(mark.burstSize(), is(6 * 1500L));
        // Action should increase dscp drop precedence
        assertThat(mark.action(), is(DSCP_PRECEDENCE));
        // Dcsp drop precedence should be increased of 2
        assertThat(mark.dscp(), is(DSCP_PREC));
        // For excess rate
        assertThat(mark.type(), is(EXCESS));
    }

    /**
     * Test creation of a dscp class token bucket.
     */
    @Test
    public void testDscpClassCreation() {
        // Create a dscp class token bucket
        TokenBucket mark = DefaultTokenBucket.builder()
                .withRate(RATE)
                .withAction(DSCP_CLASS)
                .withDscp(DSCP_CL)
                .withType(PEAK)
                .build();
        // Not null
        assertThat(mark, notNullValue());
        // Rate should be equal to RATE
        assertThat(mark.rate(), is(RATE));
        // Burst size should be equal to 2xMTU
        assertThat(mark.burstSize(), is(2 * 1500L));
        // Action should be drop
        assertThat(mark.action(), is(DSCP_CLASS));
        // Dcsp drop precedence should be increased of 2
        assertThat(mark.dscp(), is(DSCP_CL));
        // For peak rate
        assertThat(mark.type(), is(PEAK));
    }

    /**
     * Exception expected to raise when creating a token bucket with null action.
     */
    @Rule
    public ExpectedException exceptionNullAction = ExpectedException.none();

    /**
     * Test creation of a token bucket with null action.
     */
    @Test
    public void testNullActionCreation() {
        // Define expected exception
        exceptionNullAction.expect(NullPointerException.class);
        // Create a token bucket without action
        DefaultTokenBucket.builder()
                .withRate(RATE)
                .build();
    }

    /**
     * Exception expected to raise when creating a token bucket with wrong dscp.
     */
    @Rule
    public ExpectedException exceptionWrongDscp = ExpectedException.none();

    /**
     * Test creation of a token bucket with wrong dscp.
     */
    @Test
    public void testWrongDscpCreation() {
        // Define expected exception
        exceptionWrongDscp.expect(IllegalArgumentException.class);
        // Create a token bucket with wrong dscp
        DefaultTokenBucket.builder()
                .withRate(RATE)
                .withAction(DSCP_PRECEDENCE)
                .withDscp(WRONG_DSCP)
                .withType(COMMITTED)
                .build();
    }

    /**
     * Test equality between policer ids.
     */
    @Test
    public void testEqualilty() {
        // Create a drop token bucket
        TokenBucket drop = DefaultTokenBucket.builder()
                .withRate(RATE)
                .withAction(DROP)
                .withType(COMMITTED)
                .build();
        // Create a mark token bucket
        TokenBucket mark = DefaultTokenBucket.builder()
                .withRate(RATE)
                .withAction(DSCP_PRECEDENCE)
                .withDscp(DSCP_PREC)
                .withType(COMMITTED)
                .build();
        // Create a copy of the drop token bucket
        TokenBucket copyDrop = DefaultTokenBucket.builder()
                .withRate(RATE)
                .withAction(DROP)
                .withType(COMMITTED)
                .build();
        // Verify equality
        assertEquals(drop, copyDrop);
        // Verify not equals
        assertNotEquals(mark, drop);
        assertNotEquals(mark, copyDrop);
    }

}
