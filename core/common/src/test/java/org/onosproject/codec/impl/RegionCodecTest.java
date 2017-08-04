/*
 * Copyright 2016-present Open Networking Foundation
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.cluster.NodeId;
import org.onosproject.codec.JsonCodec;
import org.onosproject.core.CoreService;
import org.onosproject.net.Annotations;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.region.DefaultRegion;
import org.onosproject.net.region.Region;
import org.onosproject.net.region.RegionId;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

import static org.easymock.EasyMock.createMock;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.onosproject.codec.impl.RegionJsonMatcher.matchesRegion;

/**
 * Unit tests for region codec.
 */
public class RegionCodecTest {

    MockCodecContext context;
    JsonCodec<Region> regionCodec;
    final CoreService mockCoreService = createMock(CoreService.class);

    @Before
    public void setUp() {
        context = new MockCodecContext();
        regionCodec = context.codec(Region.class);
        assertThat(regionCodec, notNullValue());
    }

    /**
     * Tests encoding of a Region object.
     */
    @Test
    public void testRegionEncode() {
        NodeId nodeId1 = NodeId.nodeId("1");
        NodeId nodeId2 = NodeId.nodeId("2");
        NodeId nodeId3 = NodeId.nodeId("3");
        NodeId nodeId4 = NodeId.nodeId("4");

        Set<NodeId> set1 = ImmutableSet.of(nodeId1);
        Set<NodeId> set2 = ImmutableSet.of(nodeId1, nodeId2);
        Set<NodeId> set3 = ImmutableSet.of(nodeId1, nodeId2, nodeId3);
        Set<NodeId> set4 = ImmutableSet.of(nodeId1, nodeId2, nodeId3, nodeId4);
        List<Set<NodeId>> masters = ImmutableList.of(set1, set2, set3, set4);

        RegionId regionId = RegionId.regionId("1");
        String name = "foo";
        Region.Type type = Region.Type.ROOM;
        Annotations noAnnots = DefaultAnnotations.EMPTY;

        Region region = new DefaultRegion(regionId, name, type, noAnnots, masters);

        ObjectNode regionJson = regionCodec.encode(region, context);
        assertThat(regionJson, matchesRegion(region));
    }

    /**
     * Tests decoding of a json object.
     */
    @Test
    public void testRegionDecode() throws IOException {
        Region region = getRegion("Region.json");
        checkCommonData(region);

        assertThat(region.masters().size(), is(2));

        NodeId nodeId1 = NodeId.nodeId("1");
        NodeId nodeId2 = NodeId.nodeId("2");
        Set<NodeId> nodeIds1 = region.masters().get(0);
        Set<NodeId> nodeIds2 = region.masters().get(1);
        assertThat(nodeIds1.containsAll(ImmutableSet.of(nodeId1)), is(true));
        assertThat(nodeIds2.containsAll(ImmutableSet.of(nodeId1, nodeId2)), is(true));
    }

    /**
     * Checks that the data shared by all the resource is correct for a given region.
     *
     * @param region region to check
     */
    private void checkCommonData(Region region) {
        assertThat(region.id().toString(), is("1"));
        assertThat(region.type().toString(), is("ROOM"));
        assertThat(region.name(), is("foo"));
    }

    /**
     * Reads in a region from the given resource and decodes it.
     *
     * @param resourceName resource to use to read the JSON for the rule
     * @return decoded region
     * @throws IOException if processing the resource fails
     */
    private Region getRegion(String resourceName) throws IOException {
        InputStream jsonStream = RegionCodecTest.class.getResourceAsStream(resourceName);
        JsonNode json = context.mapper().readTree(jsonStream);
        MatcherAssert.assertThat(json, notNullValue());
        Region region = regionCodec.decode((ObjectNode) json, context);
        assertThat(region, notNullValue());
        return region;
    }
}
