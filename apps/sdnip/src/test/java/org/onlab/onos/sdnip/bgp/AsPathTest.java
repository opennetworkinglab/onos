/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.onlab.onos.sdnip.bgp;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;

import org.junit.Test;

/**
 * Unit tests for the BgpRouteEntry.AsPath class.
 */
public class AsPathTest {
    /**
     * Generates an AS Path.
     *
     * @return a generated AS Path
     */
    private BgpRouteEntry.AsPath generateAsPath() {
        ArrayList<BgpRouteEntry.PathSegment> pathSegments = new ArrayList<>();
        byte pathSegmentType1 = (byte) BgpConstants.Update.AsPath.AS_SEQUENCE;
        ArrayList<Long> segmentAsNumbers1 = new ArrayList<>();
        segmentAsNumbers1.add((long) 1);
        segmentAsNumbers1.add((long) 2);
        segmentAsNumbers1.add((long) 3);
        BgpRouteEntry.PathSegment pathSegment1 =
            new BgpRouteEntry.PathSegment(pathSegmentType1, segmentAsNumbers1);
        pathSegments.add(pathSegment1);
        //
        byte pathSegmentType2 = (byte) BgpConstants.Update.AsPath.AS_SET;
        ArrayList<Long> segmentAsNumbers2 = new ArrayList<>();
        segmentAsNumbers2.add((long) 4);
        segmentAsNumbers2.add((long) 5);
        segmentAsNumbers2.add((long) 6);
        BgpRouteEntry.PathSegment pathSegment2 =
            new BgpRouteEntry.PathSegment(pathSegmentType2, segmentAsNumbers2);
        pathSegments.add(pathSegment2);
        //
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
            "AsPath{pathSegments=" +
            "[PathSegment{type=2, segmentAsNumbers=[1, 2, 3]}, " +
            "PathSegment{type=1, segmentAsNumbers=[4, 5, 6]}]}";
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
        ArrayList<BgpRouteEntry.PathSegment> pathSegments = new ArrayList<>();
        byte pathSegmentType1 = (byte) BgpConstants.Update.AsPath.AS_SEQUENCE;
        ArrayList<Long> segmentAsNumbers1 = new ArrayList<>();
        segmentAsNumbers1.add((long) 1);
        segmentAsNumbers1.add((long) 2);
        segmentAsNumbers1.add((long) 3);
        BgpRouteEntry.PathSegment pathSegment1 =
            new BgpRouteEntry.PathSegment(pathSegmentType1, segmentAsNumbers1);
        pathSegments.add(pathSegment1);
        //
        byte pathSegmentType2 = (byte) BgpConstants.Update.AsPath.AS_SET;
        ArrayList<Long> segmentAsNumbers2 = new ArrayList<>();
        segmentAsNumbers2.add((long) 4);
        segmentAsNumbers2.add((long) 5);
        segmentAsNumbers2.add((long) 6);
        BgpRouteEntry.PathSegment pathSegment2 =
            new BgpRouteEntry.PathSegment(pathSegmentType2, segmentAsNumbers2);
        pathSegments.add(pathSegment2);

        // Generate the entry to test
        BgpRouteEntry.AsPath asPath = generateAsPath();

        assertThat(asPath.getPathSegments(), is(pathSegments));
    }

    /**
     * Tests getting the AS Path Length.
     */
    @Test
    public void testGetAsPathLength() {
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
        byte pathSegmentType1 = (byte) BgpConstants.Update.AsPath.AS_SEQUENCE;
        ArrayList<Long> segmentAsNumbers1 = new ArrayList<>();
        segmentAsNumbers1.add((long) 1);
        segmentAsNumbers1.add((long) 2);
        segmentAsNumbers1.add((long) 3);
        BgpRouteEntry.PathSegment pathSegment1 =
            new BgpRouteEntry.PathSegment(pathSegmentType1, segmentAsNumbers1);
        pathSegments.add(pathSegment1);
        //
        byte pathSegmentType2 = (byte) BgpConstants.Update.AsPath.AS_SET;
        ArrayList<Long> segmentAsNumbers2 = new ArrayList<>();
        segmentAsNumbers2.add((long) 4);
        segmentAsNumbers2.add((long) 55);                       // Different
        segmentAsNumbers2.add((long) 6);
        BgpRouteEntry.PathSegment pathSegment2 =
            new BgpRouteEntry.PathSegment(pathSegmentType2, segmentAsNumbers2);
        pathSegments.add(pathSegment2);
        //
        BgpRouteEntry.AsPath asPath2 = new BgpRouteEntry.AsPath(pathSegments);

        assertThat(asPath1, is(not(asPath2)));
    }

    /**
     * Tests object string representation.
     */
    @Test
    public void testToString() {
        BgpRouteEntry.AsPath asPath = generateAsPath();

        String expectedString =
            "AsPath{pathSegments=" +
            "[PathSegment{type=2, segmentAsNumbers=[1, 2, 3]}, " +
            "PathSegment{type=1, segmentAsNumbers=[4, 5, 6]}]}";
        assertThat(asPath.toString(), is(expectedString));
    }
}
