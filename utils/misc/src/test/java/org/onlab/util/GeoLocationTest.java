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

package org.onlab.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test suite of the geo location.
 */
public class GeoLocationTest {

    @Test
    public void basics() {
        GeoLocation nLoc = new GeoLocation(40.7127, -74.0059);
        GeoLocation wLoc = new GeoLocation(38.9047, -77.0164);

        assertEquals("incorrect latitude", 40.7127, nLoc.latitude(), 0.0001);
        assertEquals("incorrect longitude", -74.00598, nLoc.longitude(), 0.0001);
        assertEquals("incorrect distance", 326.74, nLoc.kilometersTo(wLoc), 0.01);
    }

}