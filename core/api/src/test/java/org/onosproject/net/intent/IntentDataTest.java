/*
 * Copyright 2015 Open Networking Laboratory
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

import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.core.IdGenerator;
import org.onosproject.net.NetTestTools;
import org.onosproject.store.Timestamp;

import com.google.common.testing.EqualsTester;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests for intent data objects.
 */
public class IntentDataTest {
    private static class MockIntent extends Intent {
        private static AtomicLong counter = new AtomicLong(0);

        private final Long number;

        public MockIntent(Long number) {
            super(NetTestTools.APP_ID, Collections.emptyList());
            this.number = number;
        }

        public Long number() {
            return number;
        }

        public static Long nextId() {
            return counter.getAndIncrement();
        }
    }

    private static class MockTimestamp implements Timestamp {
        final int value;

        MockTimestamp(int value) {
            this.value = value;
        }

        @Override
        public int compareTo(Timestamp o) {
            if (!(o instanceof MockTimestamp)) {
                return -1;
            }
            MockTimestamp that = (MockTimestamp) o;
            return (this.value > that.value ? -1 : (this.value == that.value ? 0 : 1));
        }
    }

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

    @Before
    public void setUpTest() {
        idGenerator = new MockIdGenerator();
        Intent.bindIdGenerator(idGenerator);

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

    @After
    public void tearDownTest() {
        Intent.unbindIdGenerator(idGenerator);
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
}
