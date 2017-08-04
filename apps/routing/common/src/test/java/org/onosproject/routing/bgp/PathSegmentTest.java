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

package org.onosproject.routing.bgp;

import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.ArrayList;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Unit tests for the BgpRouteEntry.PathSegment class.
 */
public class PathSegmentTest {
    /**
     * Generates a Path Segment.
     *
     * @return a generated PathSegment
     */
    private BgpRouteEntry.PathSegment generatePathSegment() {
        byte pathSegmentType = (byte) BgpConstants.Update.AsPath.AS_SEQUENCE;
        ArrayList<Long> segmentAsNumbers = new ArrayList<>();
        segmentAsNumbers.add(1L);
        segmentAsNumbers.add(2L);
        segmentAsNumbers.add(3L);
        BgpRouteEntry.PathSegment pathSegment =
            new BgpRouteEntry.PathSegment(pathSegmentType, segmentAsNumbers);

        return pathSegment;
    }

    /**
     * Tests valid class constructor.
     */
    @Test
    public void testConstructor() {
        BgpRouteEntry.PathSegment pathSegment = generatePathSegment();

        String expectedString =
            "PathSegment{type=AS_SEQUENCE, segmentAsNumbers=[1, 2, 3]}";
        assertThat(pathSegment.toString(), is(expectedString));
    }

    /**
     * Tests invalid class constructor for null Segment AS Numbers.
     */
    @Test(expected = NullPointerException.class)
    public void testInvalidConstructorNullSegmentAsNumbers() {
        byte pathSegmentType = (byte) BgpConstants.Update.AsPath.AS_SEQUENCE;
        ArrayList<Long> segmentAsNumbers = null;
        new BgpRouteEntry.PathSegment(pathSegmentType, segmentAsNumbers);
    }

    /**
     * Tests getting the fields of a Path Segment.
     */
    @Test
    public void testGetFields() {
        // Create the fields to compare against
        byte pathSegmentType = (byte) BgpConstants.Update.AsPath.AS_SEQUENCE;
        ArrayList<Long> segmentAsNumbers = new ArrayList<>();
        segmentAsNumbers.add(1L);
        segmentAsNumbers.add(2L);
        segmentAsNumbers.add(3L);

        // Generate the entry to test
        BgpRouteEntry.PathSegment pathSegment = generatePathSegment();

        assertThat(pathSegment.getType(), is(pathSegmentType));
        assertThat(pathSegment.getSegmentAsNumbers(), is(segmentAsNumbers));
    }

    /**
     * Tests equality of {@link BgpRouteEntry.PathSegment}.
     */
    @Test
    public void testEquality() {
        BgpRouteEntry.PathSegment pathSegment1 = generatePathSegment();
        BgpRouteEntry.PathSegment pathSegment2 = generatePathSegment();

        assertThat(pathSegment1, is(pathSegment2));
    }

    /**
     * Tests non-equality of {@link BgpRouteEntry.PathSegment}.
     */
    @Test
    public void testNonEquality() {
        BgpRouteEntry.PathSegment pathSegment1 = generatePathSegment();

        // Setup Path Segment 2
        byte pathSegmentType = (byte) BgpConstants.Update.AsPath.AS_SEQUENCE;
        ArrayList<Long> segmentAsNumbers = new ArrayList<>();
        segmentAsNumbers.add(1L);
        segmentAsNumbers.add(22L);                        // Different
        segmentAsNumbers.add(3L);
        //
        BgpRouteEntry.PathSegment pathSegment2 =
            new BgpRouteEntry.PathSegment(pathSegmentType, segmentAsNumbers);

        assertThat(pathSegment1, Matchers.is(Matchers.not(pathSegment2)));
    }

    /**
     * Tests object string representation.
     */
    @Test
    public void testToString() {
        BgpRouteEntry.PathSegment pathSegment = generatePathSegment();

        String expectedString =
            "PathSegment{type=AS_SEQUENCE, segmentAsNumbers=[1, 2, 3]}";
        assertThat(pathSegment.toString(), is(expectedString));
    }
}
