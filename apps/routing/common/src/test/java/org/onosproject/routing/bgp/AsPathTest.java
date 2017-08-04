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
 * Unit tests for the BgpRouteEntry.AsPath class.
 */
public class AsPathTest {
    /**
     * Generates Path Segments.
     *
     * @return the generated Path Segments
     */
    private ArrayList<BgpRouteEntry.PathSegment> generatePathSegments() {
        ArrayList<BgpRouteEntry.PathSegment> pathSegments = new ArrayList<>();
        byte pathSegmentType;
        ArrayList<Long> segmentAsNumbers;
        BgpRouteEntry.PathSegment pathSegment;

        pathSegmentType = (byte) BgpConstants.Update.AsPath.AS_CONFED_SEQUENCE;
        segmentAsNumbers = new ArrayList<>();
        segmentAsNumbers.add(1L);
        segmentAsNumbers.add(2L);
        segmentAsNumbers.add(3L);
        pathSegment =
            new BgpRouteEntry.PathSegment(pathSegmentType, segmentAsNumbers);
        pathSegments.add(pathSegment);
        //
        pathSegmentType = (byte) BgpConstants.Update.AsPath.AS_CONFED_SET;
        segmentAsNumbers = new ArrayList<>();
        segmentAsNumbers.add(4L);
        segmentAsNumbers.add(5L);
        segmentAsNumbers.add(6L);
        pathSegment =
            new BgpRouteEntry.PathSegment(pathSegmentType, segmentAsNumbers);
        pathSegments.add(pathSegment);
        //
        pathSegmentType = (byte) BgpConstants.Update.AsPath.AS_SEQUENCE;
        segmentAsNumbers = new ArrayList<>();
        segmentAsNumbers.add(7L);
        segmentAsNumbers.add(8L);
        segmentAsNumbers.add(9L);
        pathSegment =
            new BgpRouteEntry.PathSegment(pathSegmentType, segmentAsNumbers);
        pathSegments.add(pathSegment);
        //
        pathSegmentType = (byte) BgpConstants.Update.AsPath.AS_SET;
        segmentAsNumbers = new ArrayList<>();
        segmentAsNumbers.add(10L);
        segmentAsNumbers.add(11L);
        segmentAsNumbers.add(12L);
        pathSegment =
            new BgpRouteEntry.PathSegment(pathSegmentType, segmentAsNumbers);
        pathSegments.add(pathSegment);

        return pathSegments;
    }

    /**
     * Generates an AS Path.
     *
     * @return a generated AS Path
     */
    private BgpRouteEntry.AsPath generateAsPath() {
        ArrayList<BgpRouteEntry.PathSegment> pathSegments =
            generatePathSegments();
        BgpRouteEntry.AsPath asPath = new BgpRouteEntry.AsPath(pathSegments);

        return asPath;
    }

    /**
     * Tests valid class constructor.
     */
    @Test
    public void testConstructor() {
        BgpRouteEntry.AsPath asPath = generateAsPath();

        String expectedString =
            "AsPath{pathSegments=[" +
            "PathSegment{type=AS_CONFED_SEQUENCE, segmentAsNumbers=[1, 2, 3]}, " +
            "PathSegment{type=AS_CONFED_SET, segmentAsNumbers=[4, 5, 6]}, " +
            "PathSegment{type=AS_SEQUENCE, segmentAsNumbers=[7, 8, 9]}, " +
            "PathSegment{type=AS_SET, segmentAsNumbers=[10, 11, 12]}]}";
        assertThat(asPath.toString(), is(expectedString));
    }

    /**
     * Tests invalid class constructor for null Path Segments.
     */
    @Test(expected = NullPointerException.class)
    public void testInvalidConstructorNullPathSegments() {
        ArrayList<BgpRouteEntry.PathSegment> pathSegments = null;
        new BgpRouteEntry.AsPath(pathSegments);
    }

    /**
     * Tests getting the fields of an AS Path.
     */
    @Test
    public void testGetFields() {
        // Create the fields to compare against
        ArrayList<BgpRouteEntry.PathSegment> pathSegments =
            generatePathSegments();

        // Generate the entry to test
        BgpRouteEntry.AsPath asPath = generateAsPath();

        assertThat(asPath.getPathSegments(), is(pathSegments));
    }

    /**
     * Tests getting the AS Path Length.
     */
    @Test
    public void testGetAsPathLength() {
        //
        // NOTE:
        //  - AS_CONFED_SEQUENCE and AS_CONFED_SET are excluded
        //  - AS_SET counts as a single hop
        //
        BgpRouteEntry.AsPath asPath = generateAsPath();
        assertThat(asPath.getAsPathLength(), is(4));

        // Create an empty AS Path
        ArrayList<BgpRouteEntry.PathSegment> pathSegments = new ArrayList<>();
        asPath = new BgpRouteEntry.AsPath(pathSegments);
        assertThat(asPath.getAsPathLength(), is(0));
    }

    /**
     * Tests equality of {@link BgpRouteEntry.AsPath}.
     */
    @Test
    public void testEquality() {
        BgpRouteEntry.AsPath asPath1 = generateAsPath();
        BgpRouteEntry.AsPath asPath2 = generateAsPath();

        assertThat(asPath1, is(asPath2));
    }

    /**
     * Tests non-equality of {@link BgpRouteEntry.AsPath}.
     */
    @Test
    public void testNonEquality() {
        BgpRouteEntry.AsPath asPath1 = generateAsPath();

        // Setup AS Path 2
        ArrayList<BgpRouteEntry.PathSegment> pathSegments = new ArrayList<>();
        byte pathSegmentType;
        ArrayList<Long> segmentAsNumbers;
        BgpRouteEntry.PathSegment pathSegment;

        pathSegmentType = (byte) BgpConstants.Update.AsPath.AS_CONFED_SEQUENCE;
        segmentAsNumbers = new ArrayList<>();
        segmentAsNumbers.add(1L);
        segmentAsNumbers.add(2L);
        segmentAsNumbers.add(3L);
        pathSegment =
            new BgpRouteEntry.PathSegment(pathSegmentType, segmentAsNumbers);
        pathSegments.add(pathSegment);
        //
        pathSegmentType = (byte) BgpConstants.Update.AsPath.AS_CONFED_SET;
        segmentAsNumbers = new ArrayList<>();
        segmentAsNumbers.add(4L);
        segmentAsNumbers.add(5L);
        segmentAsNumbers.add(6L);
        pathSegment =
            new BgpRouteEntry.PathSegment(pathSegmentType, segmentAsNumbers);
        pathSegments.add(pathSegment);
        //
        pathSegmentType = (byte) BgpConstants.Update.AsPath.AS_SEQUENCE;
        segmentAsNumbers = new ArrayList<>();
        segmentAsNumbers.add(7L);
        segmentAsNumbers.add(8L);
        segmentAsNumbers.add(9L);
        pathSegment =
            new BgpRouteEntry.PathSegment(pathSegmentType, segmentAsNumbers);
        pathSegments.add(pathSegment);
        //
        pathSegmentType = (byte) BgpConstants.Update.AsPath.AS_SET;
        segmentAsNumbers = new ArrayList<>();
        segmentAsNumbers.add(10L);
        segmentAsNumbers.add(111L);                       // Different
        segmentAsNumbers.add(12L);
        pathSegment =
            new BgpRouteEntry.PathSegment(pathSegmentType, segmentAsNumbers);
        pathSegments.add(pathSegment);
        //
        BgpRouteEntry.AsPath asPath2 = new BgpRouteEntry.AsPath(pathSegments);

        assertThat(asPath1, Matchers.is(Matchers.not(asPath2)));
    }

    /**
     * Tests object string representation.
     */
    @Test
    public void testToString() {
        BgpRouteEntry.AsPath asPath = generateAsPath();

        String expectedString =
            "AsPath{pathSegments=[" +
            "PathSegment{type=AS_CONFED_SEQUENCE, segmentAsNumbers=[1, 2, 3]}, " +
            "PathSegment{type=AS_CONFED_SET, segmentAsNumbers=[4, 5, 6]}, " +
            "PathSegment{type=AS_SEQUENCE, segmentAsNumbers=[7, 8, 9]}, " +
            "PathSegment{type=AS_SET, segmentAsNumbers=[10, 11, 12]}]}";
        assertThat(asPath.toString(), is(expectedString));
    }
}
