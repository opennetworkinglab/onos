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

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.MplsLabel;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;

import com.google.common.testing.EqualsTester;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;
import static org.onosproject.net.NetTestTools.APP_ID;
import static org.onosproject.net.NetTestTools.connectPoint;

/**
 * Unit tests for the MplsIntent class.
 */

public class MplsIntentTest extends AbstractIntentTest {
    static final int PRIORITY = 22;

    MplsIntent intent1;
    MplsIntent intent2;

    Optional<MplsLabel> label1;
    Optional<MplsLabel> label2;

    TrafficSelector selector;
    TrafficTreatment treatment;

    @Before
    public void mplsIntentTestSetUp() throws Exception {

        label1 = Optional.of(MplsLabel.mplsLabel(1));
        label2 = Optional.of(MplsLabel.mplsLabel(2));

        selector = new IntentTestsMocks.MockSelector();
        treatment = new IntentTestsMocks.MockTreatment();

        intent1 = MplsIntent.builder()
                .appId(APP_ID)
                .ingressLabel(label1)
                .egressLabel(label2)
                .ingressPoint(connectPoint("in", 1))
                .egressPoint(connectPoint("out", 1))
                .selector(selector)
                .treatment(treatment)
                .priority(PRIORITY)
                .build();

        intent2 = MplsIntent.builder()
                .appId(APP_ID)
                .ingressLabel(label1)
                .egressLabel(label2)
                .ingressPoint(connectPoint("in", 2))
                .egressPoint(connectPoint("out", 2))
                .selector(selector)
                .treatment(treatment)
                .priority(PRIORITY)
                .build();
    }

    /**
     * Checks that the MplsIntent class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(MplsIntent.class);
    }

    /**
     * Checks the operation of equals(), hashCode() and toString() methods.
     */
    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(intent1)
                .addEqualityGroup(intent2)
                .testEquals();
    }

    /**
     * Checks that the MplsIntent objects are created correctly.
     */
    @Test
    public void testContents() {
        assertThat(intent1.appId(), equalTo(APP_ID));
        assertThat(intent1.ingressLabel(), equalTo(label1));
        assertThat(intent1.egressLabel(), equalTo(label2));
        assertThat(intent1.ingressPoint(), equalTo(connectPoint("in", 1)));
        assertThat(intent1.egressPoint(), equalTo(connectPoint("out", 1)));
        assertThat(intent1.selector(), equalTo(intent2.selector()));
        assertThat(intent1.treatment(), equalTo(intent2.treatment()));
        assertThat(intent1.priority(), is(PRIORITY));
    }
}
