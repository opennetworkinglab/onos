/*
 * Copyright 2015-present Open Networking Foundation
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

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNot.not;

/**
 * Unit tests for DefaultLoad class.
 */
public class DefaultLoadTest {

    @Before
    public void reset() {
        DefaultLoad.setPollInterval(10);
    }

    /**
     * Tests the default constructor.
     */
    @Test
    public void testDefaultConstructor() {
        DefaultLoad load = new DefaultLoad();
        assertThat(load.isValid(), is(false));
        assertThat(load.latest(), is(-1L));
        assertThat(load.rate(), is(0L));
        assertThat(load.time(), is(not(0)));
    }

    /**
     * Tests the current-previous constructor.
     */
    @Test
    public void testCurrentPreviousConstructor() {
        DefaultLoad load = new DefaultLoad(20, 10);
        assertThat(load.isValid(), is(true));
        assertThat(load.latest(), is(20L));
        assertThat(load.rate(), is(1L));
        assertThat(load.time(), is(not(0)));
    }

    /**
     * Tests the current-previous-interval constructor.
     */
    @Test
    public void testCurrentPreviousIntervalConstructor() {
        DefaultLoad load = new DefaultLoad(20, 10, 1);
        assertThat(load.isValid(), is(true));
        assertThat(load.latest(), is(20L));
        assertThat(load.rate(), is(10L));
        assertThat(load.time(), is(not(0)));
    }

    /**
     * Tests the toString operation.
     */
    @Test
    public void testToString() {
        DefaultLoad load = new DefaultLoad(20, 10);

        String s = load.toString();
        assertThat(s, containsString("Load{rate=1, latest=20}"));
    }

    /**
     * Tests setting the poll interval.
     */
    @Test
    public void testSettingPollInterval() {
        DefaultLoad.setPollInterval(1);
        DefaultLoad load = new DefaultLoad(40, 10);
        assertThat(load.rate(), is(30L));
    }
}
