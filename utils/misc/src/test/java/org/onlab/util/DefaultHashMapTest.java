/*
 *  Copyright 2015-present Open Networking Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.onlab.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link DefaultHashMap}.
 */
public class DefaultHashMapTest {

    private static final String ONE = "one";
    private static final String TWO = "two";
    private static final String THREE = "three";
    private static final String FOUR = "four";

    private static final String ALPHA = "Alpha";
    private static final String BETA = "Beta";
    private static final String OMEGA = "Omega";

    private DefaultHashMap<String, Integer> map;
    private DefaultHashMap<String, String> chartis;

    private void loadMap() {
        map.put(ONE, 1);
        map.put(TWO, 2);
    }

    private void fortioCharti() {
        chartis.put(ONE, ALPHA);
        chartis.put(TWO, BETA);
    }

    @Test
    public void nullDefaultIsAllowed() {
        // but makes this class behave no different than HashMap
        map = new DefaultHashMap<>(null);
        loadMap();
        assertEquals("missing 1", 1, (int) map.get(ONE));
        assertEquals("missing 2", 2, (int) map.get(TWO));
        assertEquals("three?", null, map.get(THREE));
        assertEquals("four?", null, map.get(FOUR));
    }

    @Test
    public void defaultToFive() {
        map = new DefaultHashMap<>(5);
        loadMap();
        assertEquals("missing 1", 1, (int) map.get(ONE));
        assertEquals("missing 2", 2, (int) map.get(TWO));
        assertEquals("three?", 5, (int) map.get(THREE));
        assertEquals("four?", 5, (int) map.get(FOUR));
    }

    @Test
    public void defaultToOmega() {
        chartis = new DefaultHashMap<>(OMEGA);
        fortioCharti();
        assertEquals("missing 1", ALPHA, chartis.get(ONE));
        assertEquals("missing 2", BETA, chartis.get(TWO));
        assertEquals("three?", OMEGA, chartis.get(THREE));
        assertEquals("four?", OMEGA, chartis.get(FOUR));
    }

}
