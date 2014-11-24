/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onlab.onos.net.intent;

import org.junit.Ignore;
import org.junit.Test;
import org.onlab.onos.net.HostId;
import org.onlab.onos.net.flow.TrafficSelector;

import com.google.common.testing.EqualsTester;

import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;
import static org.onlab.onos.net.NetTestTools.APP_ID;
import static org.onlab.onos.net.NetTestTools.hid;

/**
 * Unit tests for the HostToHostIntent class.
 */
public class HostToHostIntentTest extends IntentTest {
    private final TrafficSelector selector = new IntentTestsMocks.MockSelector();
    private final IntentTestsMocks.MockTreatment treatment = new IntentTestsMocks.MockTreatment();
    private final HostId id1 = hid("12:34:56:78:91:ab/1");
    private final HostId id2 = hid("12:34:56:78:92:ab/1");
    private final HostId id3 = hid("12:34:56:78:93:ab/1");

    /**
     * Checks that the HostToHostIntent class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(HostToHostIntent.class);
    }

    /**
     * Tests equals(), hashCode() and toString() methods.
     */
    @Test @Ignore("Equality is based on ids, which will be different")
    public void testEquals() {
        final HostToHostIntent intent1 = new HostToHostIntent(APP_ID,
                id1,
                id2,
                selector,
                treatment);
        final HostToHostIntent sameAsIntent1 = new HostToHostIntent(APP_ID,
                id1,
                id2,
                selector,
                treatment);
        final HostToHostIntent intent2 = new HostToHostIntent(APP_ID,
                id2,
                id3,
                selector,
                treatment);

        new EqualsTester()
                .addEqualityGroup(intent1, sameAsIntent1)
                .addEqualityGroup(intent2)
                .testEquals();
    }

    @Override
    protected Intent createOne() {
        return new HostToHostIntent(APP_ID, id1, id2, selector, treatment);
    }

    @Override
    protected Intent createAnother() {
        return new HostToHostIntent(APP_ID, id1, id3, selector, treatment);
    }
}
