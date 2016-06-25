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
package org.onosproject.codec.impl;

import org.junit.Test;
import org.onosproject.net.statistic.DefaultLoad;
import org.onosproject.net.statistic.Load;

import com.fasterxml.jackson.databind.JsonNode;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests for Load codec.
 */
public class LoadCodecTest {

    /**
     * Tests encoding of a Load object.
     */
    @Test
    public void testLoadEncode() {
        final long startTime = System.currentTimeMillis();
        final Load load = new DefaultLoad(20, 10, 1);
        final JsonNode node = new LoadCodec()
                .encode(load, new MockCodecContext());
        assertThat(node.get("valid").asBoolean(), is(true));
        assertThat(node.get("latest").asLong(), is(20L));
        assertThat(node.get("rate").asLong(), is(10L));
        assertThat(node.get("time").asLong(), greaterThanOrEqualTo(startTime));
    }
}
