/*
 * Copyright 2015 Open Networking Laboratory
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

package org.onosproject.ui.topo;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import org.onosproject.ui.JsonUtils;
import org.onosproject.ui.topo.Highlights.Amount;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Unit tests for {@link TopoJson}.
 */
public class TopoJsonTest {

    private static final String DEV1 = "device-1";
    private static final String DEV2 = "device-2";
    private static final String BADGE_MSG = "Hello there";

    private ObjectNode payload;

    private void checkArrayLength(String key, int expLen) {
        ArrayNode a = (ArrayNode) payload.get(key);
        assertEquals("wrong size: " + key, expLen, a.size());
    }

    private void checkEmptyArrays() {
        checkArrayLength(TopoJson.DEVICES, 0);
        checkArrayLength(TopoJson.HOSTS, 0);
        checkArrayLength(TopoJson.LINKS, 0);
    }

    @Test
    public void basicHighlights() {
        Highlights h = new Highlights();
        payload = TopoJson.json(h);
        checkEmptyArrays();
        String subdue = JsonUtils.string(payload, TopoJson.SUBDUE);
        assertEquals("subdue", "", subdue);
    }

    @Test
    public void subdueMinimalHighlights() {
        Highlights h = new Highlights().subdueAllElse(Amount.MINIMALLY);
        payload = TopoJson.json(h);
        checkEmptyArrays();
        String subdue = JsonUtils.string(payload, TopoJson.SUBDUE);
        assertEquals("not min", "min", subdue);
    }

    @Test
    public void subdueMaximalHighlights() {
        Highlights h = new Highlights().subdueAllElse(Amount.MAXIMALLY);
        payload = TopoJson.json(h);
        checkEmptyArrays();
        String subdue = JsonUtils.string(payload, TopoJson.SUBDUE);
        assertEquals("not max", "max", subdue);
    }

    @Test
    public void badgedDevice() {
        Highlights h = new Highlights();
        DeviceHighlight dh = new DeviceHighlight(DEV1);
        dh.setBadge(NodeBadge.info(BADGE_MSG));
        h.add(dh);

        dh = new DeviceHighlight(DEV2);
        dh.setBadge(NodeBadge.number(7));
        h.add(dh);

        payload = TopoJson.json(h);
        System.out.println(payload);

        // dig into the payload, and verify the badges are set on the devices
        ArrayNode a = (ArrayNode) payload.get(TopoJson.DEVICES);

        ObjectNode d = (ObjectNode) a.get(0);
        assertEquals("wrong device id", DEV1, d.get(TopoJson.ID).asText());

        ObjectNode b = (ObjectNode) d.get(TopoJson.BADGE);
        assertNotNull("missing badge", b);
        assertEquals("wrong type code", "i", b.get(TopoJson.TYPE).asText());
        assertEquals("wrong message", BADGE_MSG, b.get(TopoJson.MSG).asText());

        d = (ObjectNode) a.get(1);
        assertEquals("wrong device id", DEV2, d.get(TopoJson.ID).asText());

        b = (ObjectNode) d.get(TopoJson.BADGE);
        assertNotNull("missing badge", b);
        assertEquals("wrong type code", "n", b.get(TopoJson.TYPE).asText());
        assertEquals("wrong message", "7", b.get(TopoJson.MSG).asText());
    }
}
