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
import org.onosproject.net.Path;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;

import com.google.common.testing.EqualsTester;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;
import static org.onosproject.net.NetTestTools.APP_ID;
import static org.onosproject.net.NetTestTools.createPath;

/**
 * Unit tests for the MplsPathIntent class.
 */
public class MplsPathIntentTest extends AbstractIntentTest {

    static final int PRIORITY = 777;

    MplsPathIntent intent1;
    MplsPathIntent intent2;
    Path defaultPath;
    Optional<MplsLabel> label1;
    Optional<MplsLabel> label2;
    TrafficSelector selector;
    TrafficTreatment treatment;
    static final Key KEY1 = Key.of(5L, APP_ID);

    @Before
    public void mplsPathIntentTestSetUp() {
        defaultPath = createPath("a", "b", "c");
        selector = new IntentTestsMocks.MockSelector();
        treatment = new IntentTestsMocks.MockTreatment();

        label1 = Optional.of(MplsLabel.mplsLabel(1));
        label2 = Optional.of(MplsLabel.mplsLabel(2));
        intent1 = MplsPathIntent.builder()
                .appId(APP_ID)
                .key(KEY1)
                .ingressLabel(label1)
                .egressLabel(label2)
                .path(defaultPath)
                .priority(PRIORITY)
                .build();

        intent2 = MplsPathIntent.builder()
                .appId(APP_ID)
                .ingressLabel(label1)
                .egressLabel(label2)
                .path(defaultPath)
                .priority(PRIORITY)
                .build();
    }


    /**
     * Checks that the MplsPathIntent class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(MplsPathIntent.class);
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
     * Checks that the MPLS path intent objects are created correctly.
     */
    @Test
    public void testContents() {
        assertThat(intent1.appId(), equalTo(APP_ID));
        assertThat(intent1.ingressLabel(), equalTo(label1));
        assertThat(intent1.egressLabel(), equalTo(label2));
        assertThat(intent1.selector(), equalTo(intent2.selector()));
        assertThat(intent1.treatment(), equalTo(intent2.treatment()));
        assertThat(intent1.priority(), is(PRIORITY));
        assertThat(intent1.path(), is(defaultPath));
        assertThat(intent1.key(), equalTo(KEY1));
    }

}
