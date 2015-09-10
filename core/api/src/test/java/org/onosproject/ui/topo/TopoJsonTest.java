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

/**
 * Unit tests for {@link TopoJson}.
 */
public class TopoJsonTest {

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
}
