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

package org.onosproject.ui.topo;

import org.junit.Test;
import org.onosproject.ui.topo.NodeBadge.Status;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link NodeBadge}.
 */
public class NodeBadgeTest {

    private static final String MSG = "a msg";
    private static final String TXT = "text";
    private static final String GID = "glyph-id";
    private static final int NUM = 42;
    private static final String NUM_STR = Integer.toString(NUM);

    private static final String WR_S = "wrong status";
    private static final String WR_B = "wrong boolean";
    private static final String WR_T = "wrong text";
    private static final String WR_M = "wrong message";
    private static final String WR_SF = "wrong string format";

    private NodeBadge badge;

    private void checkFields(NodeBadge b, Status s, boolean g,
                             String txt, String msg) {
        assertEquals(WR_S, s, b.status());
        assertEquals(WR_B, g, b.isGlyph());
        assertEquals(WR_T, txt, b.text());
        assertEquals(WR_M, msg, b.message());
    }

    @Test
    public void badgeTypes() {
        assertEquals(WR_SF, "i", Status.INFO.code());
        assertEquals(WR_SF, "w", Status.WARN.code());
        assertEquals(WR_SF, "e", Status.ERROR.code());
        assertEquals("unexpected size", 3, Status.values().length);
    }

    @Test
    public void textOnly() {
        badge = NodeBadge.text(TXT);
        checkFields(badge, Status.INFO, false, TXT, null);
    }

    @Test
    public void glyphOnly() {
        badge = NodeBadge.glyph(GID);
        checkFields(badge, Status.INFO, true, GID, null);
    }

    @Test
    public void numberOnly() {
        badge = NodeBadge.number(NUM);
        checkFields(badge, Status.INFO, false, NUM_STR, null);
    }

    @Test
    public void textInfo() {
        badge = NodeBadge.text(Status.INFO, TXT);
        checkFields(badge, Status.INFO, false, TXT, null);
    }

    @Test
    public void glyphWarn() {
        badge = NodeBadge.glyph(Status.WARN, GID);
        checkFields(badge, Status.WARN, true, GID, null);
    }

    @Test
    public void numberError() {
        badge = NodeBadge.number(Status.ERROR, NUM);
        checkFields(badge, Status.ERROR, false, NUM_STR, null);
    }

    @Test
    public void textInfoMsg() {
        badge = NodeBadge.text(Status.INFO, TXT, MSG);
        checkFields(badge, Status.INFO, false, TXT, MSG);
    }

    @Test
    public void glyphWarnMsg() {
        badge = NodeBadge.glyph(Status.WARN, GID, MSG);
        checkFields(badge, Status.WARN, true, GID, MSG);
    }

    @Test
    public void numberErrorMsg() {
        badge = NodeBadge.number(Status.ERROR, NUM, MSG);
        checkFields(badge, Status.ERROR, false, NUM_STR, MSG);
    }
}
