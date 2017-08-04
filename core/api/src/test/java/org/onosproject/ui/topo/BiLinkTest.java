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
import org.onosproject.net.region.RegionId;
import org.onosproject.ui.model.topo.UiLinkId;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Unit tests for {@link BiLink}.
 */
public class BiLinkTest extends BiLinkTestBase {

    private static final String EXP_ID_AB = "device-a/1-device-b/2";

    private static final RegionId RA = RegionId.regionId("rA");
    private static final RegionId RB = RegionId.regionId("rB");
    private static final String EXP_RA_RB = "rA~rB";

    private BiLink blink;

    @Test
    public void basic() {
        blink = new ConcreteLink(KEY_AB, LINK_AB);
        assertEquals("wrong id", EXP_ID_AB, blink.linkId());
        assertEquals("wrong key", KEY_AB, blink.key());
        assertEquals("wrong link one", LINK_AB, blink.one());
        assertNull("what?", blink.two());

        blink.setOther(LINK_BA);
        assertEquals("wrong link two", LINK_BA, blink.two());
    }

    @Test(expected = NullPointerException.class)
    public void nullKey() {
        new ConcreteLink(null, LINK_AB);
    }

    @Test(expected = NullPointerException.class)
    public void nullLink() {
        new ConcreteLink(KEY_AB, null);
    }

    @Test(expected = NullPointerException.class)
    public void nullOther() {
        blink = new ConcreteLink(KEY_AB, LINK_AB);
        blink.setOther(null);
    }

    @Test
    public void canonIdentifiers() {
        // FIRST: an assumption that the LinkKey used is canonicalized
        //        ( See TopoUtils.canonicalLinkKey(Link) )
        //  so in both the following cases, KEY_AB is used...
        String expected = CP_A1 + "-" + CP_B2;

        // let's assume that link [A -> B] was dealt with first...
        blink = new ConcreteLink(KEY_AB, LINK_AB);
        blink.setOther(LINK_BA);
        print(blink);
        assertEquals("non-canon AB", expected, blink.linkId());

        // let's assume that link [B -> A] was dealt with first...
        blink = new ConcreteLink(KEY_AB, LINK_BA);
        blink.setOther(LINK_AB);
        print(blink);
        assertEquals("non-canon BA", expected, blink.linkId());
    }

    @Test
    public void uiLinkId() {
        blink = new ConcreteLink(UiLinkId.uiLinkId(RA, RB));
        print(blink);
        assertEquals("non-canon AB", EXP_RA_RB, blink.linkId());

        assertNull("key not null", blink.key());
        assertNull("one not null", blink.one());
        assertNull("two not null", blink.two());
    }

}

