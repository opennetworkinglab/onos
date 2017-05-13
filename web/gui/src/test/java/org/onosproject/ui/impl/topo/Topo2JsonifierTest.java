/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.ui.impl.topo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.onosproject.net.Annotated;
import org.onosproject.net.Annotations;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.region.RegionId;
import org.onosproject.ui.impl.AbstractUiImplTest;
import org.onosproject.ui.model.topo.UiDeviceLink;
import org.onosproject.ui.model.topo.UiLink;
import org.onosproject.ui.model.topo.UiLinkId;
import org.onosproject.ui.model.topo.UiNode;
import org.onosproject.ui.model.topo.UiRegionLink;
import org.onosproject.ui.model.topo.UiSynthLink;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.region.RegionId.regionId;
import static org.onosproject.ui.model.topo.UiLinkId.uiLinkId;
import static org.onosproject.ui.model.topo.UiNode.LAYER_DEFAULT;
import static org.onosproject.ui.model.topo.UiNode.LAYER_OPTICAL;
import static org.onosproject.ui.model.topo.UiNode.LAYER_PACKET;

/**
 * Unit tests for {@link Topo2ViewMessageHandler}.
 */
public class Topo2JsonifierTest extends AbstractUiImplTest {

    // mock node class for testing
    private static class MockNode extends UiNode {
        private final String id;

        MockNode(String id, String layer) {
            this.id = id;
            setLayer(layer);
        }

        @Override
        public String idAsString() {
            return id;
        }

        @Override
        public String toString() {
            return id;
        }
    }

    private static final List<String> ALL_TAGS = ImmutableList.of(
            LAYER_OPTICAL, LAYER_PACKET, LAYER_DEFAULT
    );

    private static final List<String> PKT_DEF_TAGS = ImmutableList.of(
            LAYER_PACKET, LAYER_DEFAULT
    );

    private static final List<String> DEF_TAG_ONLY = ImmutableList.of(
            LAYER_DEFAULT
    );

    private static final MockNode NODE_A = new MockNode("A-O", LAYER_OPTICAL);
    private static final MockNode NODE_B = new MockNode("B-P", LAYER_PACKET);
    private static final MockNode NODE_C = new MockNode("C-O", LAYER_OPTICAL);
    private static final MockNode NODE_D = new MockNode("D-D", LAYER_DEFAULT);
    private static final MockNode NODE_E = new MockNode("E-P", LAYER_PACKET);
    private static final MockNode NODE_F = new MockNode("F-r", "random");

    private static final Set<MockNode> NODES = ImmutableSet.of(
            NODE_A, NODE_B, NODE_C, NODE_D, NODE_E, NODE_F
    );

    private Topo2Jsonifier t2 = new Topo2Jsonifier();

    @Test
    public void threeLayers() {
        title("threeLayers()");

        List<Set<UiNode>> result = t2.splitByLayer(ALL_TAGS, NODES);
        print(result);

        assertEquals("wrong split size", 3, result.size());
        Set<UiNode> opt = result.get(0);
        Set<UiNode> pkt = result.get(1);
        Set<UiNode> def = result.get(2);

        assertEquals("opt bad size", 2, opt.size());
        assertEquals("missing node A", true, opt.contains(NODE_A));
        assertEquals("missing node C", true, opt.contains(NODE_C));

        assertEquals("pkt bad size", 2, pkt.size());
        assertEquals("missing node B", true, pkt.contains(NODE_B));
        assertEquals("missing node E", true, pkt.contains(NODE_E));

        assertEquals("def bad size", 2, def.size());
        assertEquals("missing node D", true, def.contains(NODE_D));
        assertEquals("missing node F", true, def.contains(NODE_F));
    }

    @Test
    public void twoLayers() {
        title("twoLayers()");

        List<Set<UiNode>> result = t2.splitByLayer(PKT_DEF_TAGS, NODES);
        print(result);

        assertEquals("wrong split size", 2, result.size());
        Set<UiNode> pkt = result.get(0);
        Set<UiNode> def = result.get(1);

        assertEquals("pkt bad size", 2, pkt.size());
        assertEquals("missing node B", true, pkt.contains(NODE_B));
        assertEquals("missing node E", true, pkt.contains(NODE_E));

        assertEquals("def bad size", 4, def.size());
        assertEquals("missing node D", true, def.contains(NODE_D));
        assertEquals("missing node F", true, def.contains(NODE_F));
        assertEquals("missing node A", true, def.contains(NODE_A));
        assertEquals("missing node C", true, def.contains(NODE_C));
    }

    @Test
    public void oneLayer() {
        title("oneLayer()");

        List<Set<UiNode>> result = t2.splitByLayer(DEF_TAG_ONLY, NODES);
        print(result);

        assertEquals("wrong split size", 1, result.size());
        Set<UiNode> def = result.get(0);

        assertEquals("def bad size", 6, def.size());
        assertEquals("missing node D", true, def.contains(NODE_D));
        assertEquals("missing node F", true, def.contains(NODE_F));
        assertEquals("missing node A", true, def.contains(NODE_A));
        assertEquals("missing node C", true, def.contains(NODE_C));
        assertEquals("missing node B", true, def.contains(NODE_B));
        assertEquals("missing node E", true, def.contains(NODE_E));
    }

    private static final String K1 = "K1";
    private static final String K2 = "K2";
    private static final String K3 = "K3";
    private static final String K4 = "K4";

    private static final String V1 = "V1";
    private static final String V2 = "V2";
    private static final String V3 = "V3";

    private static final Annotations ANNOTS = new Annotations() {
        @Override
        public Set<String> keys() {
            return ImmutableSet.of(K1, K2, K3);
        }

        @Override
        public String value(String key) {
            switch (key) {
                case K1:
                    return V1;
                case K2:
                    return V2;
                case K3:
                    return V3;
                default:
                    return null;
            }
        }
    };

    private static final Annotated THING = () -> ANNOTS;

    private void verifyValues(List<String> vals, String... exp) {
        print(vals);
        if (exp.length == 0) {
            // don't expect any results
            assertNull("huh?", vals);
        } else {
            assertEquals("wrong list len", exp.length, vals.size());

            for (int i = 0; i < exp.length; i++) {
                assertEquals("wrong value " + i, exp[i], vals.get(i));
            }
        }
    }

    @Test
    public void annotValues() {
        title("annotValues()");
        verifyValues(t2.getAnnotValues(THING, K1), V1);
        verifyValues(t2.getAnnotValues(THING, K3, K1), V3, V1);
        verifyValues(t2.getAnnotValues(THING, K1, K2, K3), V1, V2, V3);
        verifyValues(t2.getAnnotValues(THING, K1, K4));
    }


    /*
     * Test collation of region links in the following scenario...
     *
     *   Region A     Region B     Region C
     *   +.......+    +.......+    +.......+
     *   :  [1] -------- [3] -------- [5]  :
     *   :   |   :    :   |   :    :   |   :
     *   :   |   :    :   |   :    :   |   :
     *   :  [2] -------- [4] -------- [6]  :
     *   +.......+    +.......+    +.......+
     */

    private static PortNumber pn(long i) {
        return PortNumber.portNumber(i);
    }

    private static UiLink rrLink(UiLinkId id) {
        if (!id.isRegionRegion()) {
            throw new IllegalArgumentException();
        }
        return new UiRegionLink(null, id);
    }

    private static UiLink ddLink(UiLinkId id) {
        if (!id.isDeviceDevice()) {
            throw new IllegalArgumentException();
        }
        return new UiDeviceLink(null, id);
    }

    private static final RegionId REGION_ROOT = regionId("root");
    private static final RegionId REGION_A = regionId("rA");
    private static final RegionId REGION_B = regionId("rB");
    private static final RegionId REGION_C = regionId("rC");

    private static final DeviceId DEV_1 = deviceId("d1");
    private static final DeviceId DEV_2 = deviceId("d2");
    private static final DeviceId DEV_3 = deviceId("d3");
    private static final DeviceId DEV_4 = deviceId("d4");
    private static final DeviceId DEV_5 = deviceId("d5");
    private static final DeviceId DEV_6 = deviceId("d6");

    private static final UiLinkId D1_D2 = uiLinkId(DEV_1, pn(2), DEV_2, pn(1));
    private static final UiLinkId D1_D3 = uiLinkId(DEV_1, pn(3), DEV_3, pn(1));
    private static final UiLinkId D2_D4 = uiLinkId(DEV_2, pn(4), DEV_4, pn(2));
    private static final UiLinkId D3_D4 = uiLinkId(DEV_3, pn(4), DEV_4, pn(3));
    private static final UiLinkId D3_D5 = uiLinkId(DEV_3, pn(5), DEV_5, pn(3));
    private static final UiLinkId D4_D6 = uiLinkId(DEV_4, pn(6), DEV_6, pn(4));
    private static final UiLinkId D5_D6 = uiLinkId(DEV_5, pn(6), DEV_6, pn(5));

    private static final UiLinkId RA_RB = uiLinkId(REGION_A, REGION_B);
    private static final UiLinkId RB_RC = uiLinkId(REGION_B, REGION_C);

    private UiSynthLink makeSynth(RegionId container, UiLinkId rr, UiLinkId dd) {
        return new UiSynthLink(container, rrLink(rr), ddLink(dd));
    }

    private List<UiSynthLink> createSynthLinks() {
        List<UiSynthLink> links = new ArrayList<>();
        links.add(makeSynth(REGION_ROOT, RA_RB, D1_D3));
        links.add(makeSynth(REGION_ROOT, RA_RB, D2_D4));
        links.add(makeSynth(REGION_ROOT, RB_RC, D3_D5));
        links.add(makeSynth(REGION_ROOT, RB_RC, D4_D6));
        return links;
    }

    @Test
    public void encodeSynthLinks() {
        title("encodeSynthLinks()");
        ArrayNode array = (ArrayNode) t2.jsonLinks(createSynthLinks());
        print(array);

        assertEquals("wrong size", 2, array.size());
        ObjectNode first = (ObjectNode) array.get(0);
        ObjectNode second = (ObjectNode) array.get(1);

        boolean firstIsAB = first.get("id").asText().equals("rA~rB");
        if (firstIsAB) {
            validateSynthLinks(first, second);
        } else {
            validateSynthLinks(second, first);
        }
    }

    private void validateSynthLinks(ObjectNode ab, ObjectNode bc) {
        validateLinkRollup(ab, RA_RB, D1_D3, D2_D4);
        validateLinkRollup(bc, RB_RC, D3_D5, D4_D6);
    }

    private void validateLinkRollup(ObjectNode link, UiLinkId id,
                                    UiLinkId... expInRollup) {
        String actId = link.get("id").asText();
        assertEquals("unexp id", id.toString(), actId);

        Set<String> rollupIds = new HashSet<>();
        ArrayNode rollupArray = (ArrayNode) link.get("rollup");

        for (JsonNode n : rollupArray) {
            ObjectNode o = (ObjectNode) n;
            rollupIds.add(o.get("id").asText());
        }

        for (UiLinkId expId : expInRollup) {
            assertTrue("missing exp id: " + expId, rollupIds.contains(expId.toString()));
        }
    }
}
