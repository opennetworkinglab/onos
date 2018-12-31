/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.openstacktelemetry.api;

import com.google.common.testing.EqualsTester;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Unit tests for DefaultStatsInfo class.
 */
public final class DefaultStatsInfoTest {

    private static final int STATIC_INTEGER_1 = 1;
    private static final int STATIC_INTEGER_2 = 2;

    private StatsInfo info1;
    private StatsInfo sameAsInfo1;
    private StatsInfo info2;

    /**
     * Initial setup for this unit test.
     */
    @Before
    public void setup() {
        StatsInfo.Builder builder1 = new DefaultStatsInfo.DefaultBuilder();
        StatsInfo.Builder builder2 = new DefaultStatsInfo.DefaultBuilder();
        StatsInfo.Builder builder3 = new DefaultStatsInfo.DefaultBuilder();

        info1 = builder1
                .withStartupTime(STATIC_INTEGER_1)
                .withCurrAccPkts(STATIC_INTEGER_1)
                .withCurrAccBytes(STATIC_INTEGER_1)
                .withPrevAccPkts(STATIC_INTEGER_1)
                .withPrevAccBytes(STATIC_INTEGER_1)
                .withFstPktArrTime(STATIC_INTEGER_1)
                .withLstPktOffset(STATIC_INTEGER_1)
                .withErrorPkts((short) STATIC_INTEGER_1)
                .withDropPkts((short) STATIC_INTEGER_1)
                .build();

        sameAsInfo1 = builder2
                .withStartupTime(STATIC_INTEGER_1)
                .withCurrAccPkts(STATIC_INTEGER_1)
                .withCurrAccBytes(STATIC_INTEGER_1)
                .withPrevAccPkts(STATIC_INTEGER_1)
                .withPrevAccBytes(STATIC_INTEGER_1)
                .withFstPktArrTime(STATIC_INTEGER_1)
                .withLstPktOffset(STATIC_INTEGER_1)
                .withErrorPkts((short) STATIC_INTEGER_1)
                .withDropPkts((short) STATIC_INTEGER_1)
                .build();

        info2 = builder3
                .withStartupTime(STATIC_INTEGER_2)
                .withCurrAccPkts(STATIC_INTEGER_2)
                .withCurrAccBytes(STATIC_INTEGER_2)
                .withPrevAccPkts(STATIC_INTEGER_2)
                .withPrevAccBytes(STATIC_INTEGER_2)
                .withFstPktArrTime(STATIC_INTEGER_2)
                .withLstPktOffset(STATIC_INTEGER_2)
                .withErrorPkts((short) STATIC_INTEGER_2)
                .withDropPkts((short) STATIC_INTEGER_2)
                .build();
    }

    /**
     * Tests class immutability.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(DefaultStatsInfo.class);
    }

    /**
     * Tests object equality.
     */
    @Test
    public void testEquality() {
        new EqualsTester()
                .addEqualityGroup(info1, sameAsInfo1)
                .addEqualityGroup(info2).testEquals();
    }

    /**
     * Tests object construction.
     */
    @Test
    public void testConstruction() {
        StatsInfo info = info1;

        assertThat(info.startupTime(), is((long) STATIC_INTEGER_1));
        assertThat(info.currAccPkts(), is(STATIC_INTEGER_1));
        assertThat(info.currAccBytes(), is((long) STATIC_INTEGER_1));
        assertThat(info.prevAccPkts(), is(STATIC_INTEGER_1));
        assertThat(info.prevAccBytes(), is((long) STATIC_INTEGER_1));
        assertThat(info.fstPktArrTime(), is((long) STATIC_INTEGER_1));
        assertThat(info.lstPktOffset(), is(STATIC_INTEGER_1));
        assertThat(info.errorPkts(), is((short) STATIC_INTEGER_1));
        assertThat(info.dropPkts(), is((short) STATIC_INTEGER_1));
    }
}
