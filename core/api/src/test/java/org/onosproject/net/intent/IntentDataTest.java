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
package org.onosproject.net.intent;

import com.google.common.testing.EqualsTester;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.core.IdGenerator;
import org.onosproject.store.Timestamp;

import static junit.framework.TestCase.assertFalse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;
import static org.onosproject.net.intent.IntentState.FAILED;
import static org.onosproject.net.intent.IntentTestsMocks.MockIntent;
import static org.onosproject.net.intent.IntentTestsMocks.MockTimestamp;

/**
 * Unit tests for intent data objects.
 */
public class IntentDataTest extends AbstractIntentTest {

    private Timestamp timestamp1;
    private Timestamp timestamp2;
    private Timestamp timestamp3;

    private Intent intent1;
    private Intent intent2;
    private Intent intent3;

    private IntentData data1;
    private IntentData data1Copy;
    private IntentData data2;
    private IntentData data2Copy;
    private IntentData data3;
    private IntentData data3Copy;

    IdGenerator idGenerator;

    @Override
    @Before
    public void setUp() {
        super.setUp();

        timestamp1 = new MockTimestamp(1);
        timestamp2 = new MockTimestamp(2);
        timestamp3 = new MockTimestamp(3);

        intent1 = new MockIntent(1L);
        intent2 = new MockIntent(2L);
        intent3 = new MockIntent(3L);

        data1 = new IntentData(intent1, IntentState.INSTALLED, timestamp1);
        data1Copy = new IntentData(intent1, IntentState.INSTALLED, timestamp1);
        data2 = new IntentData(intent2, IntentState.INSTALLED, timestamp2);
        data2Copy = new IntentData(intent2, IntentState.INSTALLED, timestamp2);
        data3 = new IntentData(intent3, IntentState.INSTALLED, timestamp3);
        data3Copy = new IntentData(intent3, IntentState.INSTALLED, timestamp3);
    }

    /**
     * Checks that intent data objects are properly constructed.
     */
    @Test
    public void checkConstruction() {
        assertThat(data1.state(), is(IntentState.INSTALLED));
        assertThat(data1.version(), is(timestamp1));
        assertThat(data1.intent(), is(intent1));
    }

    /**
     * Checks equals() for intent data objects.
     */
    @Test
    public void checkEquals() {
        new EqualsTester()
                .addEqualityGroup(data1, data1Copy)
                .addEqualityGroup(data2, data2Copy)
                .addEqualityGroup(data3, data3Copy)
                .testEquals();
    }

    @Test
    public void testIsUpdateAcceptable() {
        // Going from null to something is always allowed
        assertTrue(IntentData.isUpdateAcceptable(null, data1));

        // we can go from older version to newer but not they other way
        assertTrue(IntentData.isUpdateAcceptable(data1, data2));
        assertFalse(IntentData.isUpdateAcceptable(data2, data1));

        IntentData installing = new IntentData(intent1, IntentState.INSTALLING, timestamp1);
        IntentData installed = IntentData.nextState(installing, IntentState.INSTALLED);
        IntentData withdrawing = new IntentData(intent1, IntentState.WITHDRAWING, timestamp1);
        IntentData withdrawn = IntentData.nextState(withdrawing, IntentState.WITHDRAWN);

        IntentData failed = new IntentData(intent1, IntentState.FAILED, timestamp1);

        IntentData compiling = new IntentData(intent1, IntentState.COMPILING, timestamp1);
        IntentData recompiling = new IntentData(intent1, IntentState.RECOMPILING, timestamp1);

        IntentData installReq = new IntentData(intent1, IntentState.INSTALL_REQ, timestamp2);
        IntentData withdrawReq = new IntentData(intent1, IntentState.WITHDRAW_REQ, timestamp2);
        IntentData purgeReq = new IntentData(intent1, IntentState.PURGE_REQ, timestamp2);

        // We can't change to the same state
        assertFalse(IntentData.isUpdateAcceptable(installing, installing));
        assertFalse(IntentData.isUpdateAcceptable(installed, installed));

        // From installing we can change to installed
        assertTrue(IntentData.isUpdateAcceptable(installing, installed));
        // transition in reverse should be rejected
        assertFalse(IntentData.isUpdateAcceptable(installed, installing));

        // Sanity checks in case the manager submits bogus state transitions
        assertFalse(IntentData.isUpdateAcceptable(installing, withdrawing));
        assertFalse(IntentData.isUpdateAcceptable(installing, withdrawn));
        assertFalse(IntentData.isUpdateAcceptable(installed, withdrawing));
        assertFalse(IntentData.isUpdateAcceptable(installed, withdrawn));

        // We can't change to the same state
        assertFalse(IntentData.isUpdateAcceptable(withdrawing, withdrawing));
        assertFalse(IntentData.isUpdateAcceptable(withdrawn, withdrawn));

        // From withdrawing we can change to withdrawn
        assertTrue(IntentData.isUpdateAcceptable(withdrawing, withdrawn));

        // Sanity checks in case the manager submits bogus state transitions
        assertFalse(IntentData.isUpdateAcceptable(withdrawing, installing));
        assertFalse(IntentData.isUpdateAcceptable(withdrawing, installed));
        assertFalse(IntentData.isUpdateAcceptable(withdrawn, installing));
        assertFalse(IntentData.isUpdateAcceptable(withdrawn, installed));

        // We can't go from failed to failed
        assertFalse(IntentData.isUpdateAcceptable(failed, failed));

        // But we can go from any install* or withdraw* state to failed
        assertTrue(IntentData.isUpdateAcceptable(installing, IntentData.nextState(installing, FAILED)));
        assertTrue(IntentData.isUpdateAcceptable(installed, IntentData.nextState(installed, FAILED)));
        assertTrue(IntentData.isUpdateAcceptable(withdrawing, IntentData.nextState(withdrawing, FAILED)));
        assertTrue(IntentData.isUpdateAcceptable(withdrawn, IntentData.nextState(withdrawn, FAILED)));

        // We can go from anything to purgeReq
        assertTrue(IntentData.isUpdateAcceptable(installing, purgeReq));
        assertTrue(IntentData.isUpdateAcceptable(installed, purgeReq));
        assertTrue(IntentData.isUpdateAcceptable(withdrawing, purgeReq));
        assertTrue(IntentData.isUpdateAcceptable(withdrawn, purgeReq));
        assertTrue(IntentData.isUpdateAcceptable(failed, purgeReq));

        // We can't go from purgeReq back to anything else
        assertFalse(IntentData.isUpdateAcceptable(purgeReq, withdrawn));
        assertFalse(IntentData.isUpdateAcceptable(purgeReq, withdrawing));
        assertFalse(IntentData.isUpdateAcceptable(purgeReq, installed));
        assertFalse(IntentData.isUpdateAcceptable(purgeReq, installing));

        // We're never allowed to store transient states
        assertFalse(IntentData.isUpdateAcceptable(installing, compiling));
        assertFalse(IntentData.isUpdateAcceptable(installing, recompiling));
        assertFalse(IntentData.isUpdateAcceptable(installing, installing));
        assertFalse(IntentData.isUpdateAcceptable(installing, withdrawing));
    }
}
