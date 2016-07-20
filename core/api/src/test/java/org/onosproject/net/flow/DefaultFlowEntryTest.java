/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onosproject.net.flow;

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.onosproject.net.intent.IntentTestsMocks;

import com.google.common.testing.EqualsTester;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.onosproject.net.NetTestTools.did;

/**
 * Unit tests for the DefaultFlowEntry class.
 */
public class DefaultFlowEntryTest {
    private static final IntentTestsMocks.MockSelector SELECTOR =
            new IntentTestsMocks.MockSelector();
    private static final IntentTestsMocks.MockTreatment TREATMENT =
            new IntentTestsMocks.MockTreatment();

    private static DefaultFlowEntry makeFlowEntry(int uniqueValue) {
        FlowRule rule = DefaultFlowRule.builder()
                .forDevice(did("id" + Integer.toString(uniqueValue)))
                .withSelector(SELECTOR)
                .withTreatment(TREATMENT)
                .withPriority(uniqueValue)
                .withCookie(uniqueValue)
                .makeTemporary(uniqueValue)
                .build();

        return new DefaultFlowEntry(rule, FlowEntry.FlowEntryState.ADDED,
                uniqueValue, uniqueValue, uniqueValue);
    }

    final DefaultFlowEntry defaultFlowEntry1 = makeFlowEntry(1);
    final DefaultFlowEntry sameAsDefaultFlowEntry1 = makeFlowEntry(1);
    final DefaultFlowEntry defaultFlowEntry2 = makeFlowEntry(2);

    /**
     * Tests the equals, hashCode and toString methods using Guava EqualsTester.
     */
    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(defaultFlowEntry1, sameAsDefaultFlowEntry1)
                .addEqualityGroup(defaultFlowEntry2)
                .testEquals();
    }

    /**
     * Tests the construction of a default flow entry from a device id.
     */
    @Test
    public void testDeviceBasedObject() {
        assertThat(defaultFlowEntry1.deviceId(), is(did("id1")));
        assertThat(defaultFlowEntry1.selector(), is(SELECTOR));
        assertThat(defaultFlowEntry1.treatment(), is(TREATMENT));
        assertThat(defaultFlowEntry1.timeout(), is(1));
        assertThat(defaultFlowEntry1.life(), is(1L));
        assertThat(defaultFlowEntry1.life(TimeUnit.SECONDS), is(1L));
        assertThat(defaultFlowEntry1.life(TimeUnit.MILLISECONDS), is(1000L));
        assertThat(defaultFlowEntry1.life(TimeUnit.MINUTES), is(0L));
        assertThat(defaultFlowEntry1.packets(), is(1L));
        assertThat(defaultFlowEntry1.bytes(), is(1L));
        assertThat(defaultFlowEntry1.state(), is(FlowEntry.FlowEntryState.ADDED));
        assertThat(defaultFlowEntry1.lastSeen(),
                   greaterThan(System.currentTimeMillis() -
                           TimeUnit.MILLISECONDS.convert(1, TimeUnit.SECONDS)));
    }

    /**
     * Tests the setters on a default flow entry object.
     */
    @Test
    public void testSetters() {
        final DefaultFlowEntry entry = makeFlowEntry(1);

        entry.setLastSeen();
        entry.setState(FlowEntry.FlowEntryState.PENDING_REMOVE);
        entry.setPackets(11);
        entry.setBytes(22);
        entry.setLife(33333, TimeUnit.MILLISECONDS);

        assertThat(entry.deviceId(), is(did("id1")));
        assertThat(entry.selector(), is(SELECTOR));
        assertThat(entry.treatment(), is(TREATMENT));
        assertThat(entry.timeout(), is(1));
        assertThat(entry.life(), is(33L));
        assertThat(entry.life(TimeUnit.MILLISECONDS), is(33333L));
        assertThat(entry.packets(), is(11L));
        assertThat(entry.bytes(), is(22L));
        assertThat(entry.state(), is(FlowEntry.FlowEntryState.PENDING_REMOVE));
        assertThat(entry.lastSeen(),
                greaterThan(System.currentTimeMillis() -
                        TimeUnit.MILLISECONDS.convert(1, TimeUnit.SECONDS)));
    }

    /**
     * Tests a default flow rule built for an error.
     */
    @Test
    public void testErrorObject() {
        final DefaultFlowEntry errorEntry =
                new DefaultFlowEntry(new IntentTestsMocks.MockFlowRule(1),
                                     111,
                                     222);
        assertThat(errorEntry.errType(), is(111));
        assertThat(errorEntry.errCode(), is(222));
        assertThat(errorEntry.state(), is(FlowEntry.FlowEntryState.FAILED));
        assertThat(errorEntry.lastSeen(),
                greaterThan(System.currentTimeMillis() -
                        TimeUnit.MILLISECONDS.convert(1, TimeUnit.SECONDS)));
    }

    /**
     * Tests a default flow entry constructed from a flow rule.
     */
    @Test
    public void testFlowBasedObject() {
        final DefaultFlowEntry entry =
                new DefaultFlowEntry(new IntentTestsMocks.MockFlowRule(1));
        assertThat(entry.priority(), is(1));
        assertThat(entry.appId(), is((short) 0));
        assertThat(entry.lastSeen(),
                greaterThan(System.currentTimeMillis() -
                        TimeUnit.MILLISECONDS.convert(1, TimeUnit.SECONDS)));
    }

    /**
     * Tests a default flow entry constructed from a flow rule plus extra
     * parameters.
     */
    @Test
    public void testFlowBasedObjectWithParameters() {
        final DefaultFlowEntry entry =
                new DefaultFlowEntry(new IntentTestsMocks.MockFlowRule(33),
                        FlowEntry.FlowEntryState.REMOVED,
                        101, 102, 103);
        assertThat(entry.state(), is(FlowEntry.FlowEntryState.REMOVED));
        assertThat(entry.life(), is(101L));
        assertThat(entry.packets(), is(102L));
        assertThat(entry.bytes(), is(103L));
        assertThat(entry.lastSeen(),
                greaterThan(System.currentTimeMillis() -
                        TimeUnit.MILLISECONDS.convert(1, TimeUnit.SECONDS)));
    }
}
