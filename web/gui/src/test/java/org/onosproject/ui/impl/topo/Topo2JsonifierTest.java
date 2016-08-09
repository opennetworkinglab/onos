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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.onosproject.net.Annotated;
import org.onosproject.net.Annotations;
import org.onosproject.ui.impl.AbstractUiImplTest;
import org.onosproject.ui.model.topo.UiNode;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
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
        print("threeLayers()");

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
        print("twoLayers()");

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
        print("oneLayer()");

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
        print("annotValues()");
        verifyValues(t2.getAnnotValues(THING, K1), V1);
        verifyValues(t2.getAnnotValues(THING, K3, K1), V3, V1);
        verifyValues(t2.getAnnotValues(THING, K1, K2, K3), V1, V2, V3);
        verifyValues(t2.getAnnotValues(THING, K1, K4));
    }
}
