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

package org.onosproject.net.statistic;

import org.junit.Test;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.NetTestTools;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.StoredFlowEntry;
import org.onosproject.net.flow.StoredFlowEntryAdapter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests for flow entry with load.
 */
public class FlowEntryWithLoadTest {
    class MockFlowEntry extends StoredFlowEntryAdapter {
        FlowLiveType liveType;

        public MockFlowEntry(FlowLiveType liveType) {
            this.liveType = liveType;
        }

        @Override
        public FlowLiveType liveType() {
            return liveType;
        }
    }

    @Test
    public void testConstructionCpFeLoad() {
        ConnectPoint cp = NetTestTools.connectPoint("id1", 1);
        StoredFlowEntry fe = new MockFlowEntry(FlowEntry.FlowLiveType.IMMEDIATE);
        Load load = new DefaultLoad();
        FlowEntryWithLoad underTest = new FlowEntryWithLoad(cp, fe, load);

        assertThat(underTest.connectPoint(), is(cp));
        assertThat(underTest.load(), is(load));
        assertThat(underTest.storedFlowEntry(), is(fe));
    }

    @Test
    public void testConstructionDEfaultLoad() {
        ConnectPoint cp = NetTestTools.connectPoint("id1", 1);
        StoredFlowEntry fe = new MockFlowEntry(FlowEntry.FlowLiveType.IMMEDIATE);
        FlowEntryWithLoad underTest;

        fe = new MockFlowEntry(FlowEntry.FlowLiveType.IMMEDIATE);
        underTest = new FlowEntryWithLoad(cp, fe);
        assertThat(underTest.connectPoint(), is(cp));
        assertThat(underTest.load(), instanceOf(DefaultLoad.class));
        assertThat(underTest.storedFlowEntry(), is(fe));

        fe = new MockFlowEntry(FlowEntry.FlowLiveType.LONG);
        underTest = new FlowEntryWithLoad(cp, fe);
        assertThat(underTest.connectPoint(), is(cp));
        assertThat(underTest.load(), instanceOf(DefaultLoad.class));

        fe = new MockFlowEntry(FlowEntry.FlowLiveType.MID);
        underTest = new FlowEntryWithLoad(cp, fe);
        assertThat(underTest.connectPoint(), is(cp));
        assertThat(underTest.load(), instanceOf(DefaultLoad.class));

        fe = new MockFlowEntry(FlowEntry.FlowLiveType.SHORT);
        underTest = new FlowEntryWithLoad(cp, fe);
        assertThat(underTest.connectPoint(), is(cp));
        assertThat(underTest.load(), instanceOf(DefaultLoad.class));

        fe = new MockFlowEntry(FlowEntry.FlowLiveType.UNKNOWN);
        underTest = new FlowEntryWithLoad(cp, fe);
        assertThat(underTest.connectPoint(), is(cp));
        assertThat(underTest.load(), instanceOf(DefaultLoad.class));
    }
}
