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

package org.onosproject.ui.topo;

import org.junit.Test;

import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.onosproject.ui.topo.LinkHighlight.Flavor.*;

/**
 * Unit tests for {@link LinkHighlight}.
 */
public class LinkHighlightTest {

    private static final String LINK_ID = "link-id-for-testing";
    private static final String LABEL = "some label";
    private static final String EMPTY = "";
    private static final String CUSTOM = "custom";
    private static final String ANIMATED = "animated";
    private static final String OPTICAL = "optical";

    private LinkHighlight lh;

    @Test
    public void basic() {
        lh = new LinkHighlight(LINK_ID, NO_HIGHLIGHT);

        assertEquals("wrong flavor", NO_HIGHLIGHT, lh.flavor());
        assertTrue("unexpected mods", lh.mods().isEmpty());
        assertEquals("wrong css", "plain", lh.cssClasses());
        assertEquals("wrong label", EMPTY, lh.label());
    }

    @Test
    public void primaryOptical() {
        lh = new LinkHighlight(LINK_ID, PRIMARY_HIGHLIGHT)
                .addMod(LinkHighlight.MOD_OPTICAL);

        assertEquals("wrong flavor", PRIMARY_HIGHLIGHT, lh.flavor());
        assertEquals("missing mod", 1, lh.mods().size());
        Mod m = lh.mods().iterator().next();
        assertEquals("wrong mod", LinkHighlight.MOD_OPTICAL, m);
        assertEquals("wrong css", "primary optical", lh.cssClasses());
        assertEquals("wrong label", EMPTY, lh.label());
    }

    @Test
    public void secondaryAnimatedWithLabel() {
        lh = new LinkHighlight(LINK_ID, SECONDARY_HIGHLIGHT)
                .addMod(LinkHighlight.MOD_ANIMATED)
                .setLabel(LABEL);

        assertEquals("wrong flavor", SECONDARY_HIGHLIGHT, lh.flavor());
        assertEquals("missing mod", 1, lh.mods().size());
        Mod m = lh.mods().iterator().next();
        assertEquals("wrong mod", LinkHighlight.MOD_ANIMATED, m);
        assertEquals("wrong css", "secondary animated", lh.cssClasses());
        assertEquals("wrong label", LABEL, lh.label());
    }

    @Test
    public void customMod() {
        lh = new LinkHighlight(LINK_ID, PRIMARY_HIGHLIGHT)
                .addMod(new Mod(CUSTOM));

        assertEquals("missing mod", 1, lh.mods().size());
        Mod m = lh.mods().iterator().next();
        assertEquals("wrong mod", CUSTOM, m.toString());
        assertEquals("wrong css", "primary custom", lh.cssClasses());
    }

    @Test
    public void severalMods() {
        lh = new LinkHighlight(LINK_ID, SECONDARY_HIGHLIGHT)
                .addMod(LinkHighlight.MOD_OPTICAL)
                .addMod(LinkHighlight.MOD_ANIMATED)
                .addMod(new Mod(CUSTOM));

        assertEquals("missing mods", 3, lh.mods().size());
        Iterator<Mod> iter = lh.mods().iterator();
        // NOTE: we know we are using TreeSet as backing => sorted order
        assertEquals("wrong mod", ANIMATED, iter.next().toString());
        assertEquals("wrong mod", CUSTOM, iter.next().toString());
        assertEquals("wrong mod", OPTICAL, iter.next().toString());
        assertEquals("wrong css", "secondary animated custom optical", lh.cssClasses());
    }

    @Test(expected = NullPointerException.class)
    public void noFlavor() {
        new LinkHighlight(LINK_ID, null);
    }

    @Test(expected = NullPointerException.class)
    public void noIdentity() {
        new LinkHighlight(null, PRIMARY_HIGHLIGHT);
    }

}
