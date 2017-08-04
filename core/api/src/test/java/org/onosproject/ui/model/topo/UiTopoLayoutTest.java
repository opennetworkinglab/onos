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

package org.onosproject.ui.model.topo;

import org.junit.Test;
import org.onosproject.net.Annotations;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.region.DefaultRegion;
import org.onosproject.net.region.Region;
import org.onosproject.net.region.RegionId;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.onosproject.net.region.Region.Type.CAMPUS;
import static org.onosproject.net.region.RegionId.regionId;
import static org.onosproject.ui.model.topo.UiTopoLayout.E_GEOMAP_SET;
import static org.onosproject.ui.model.topo.UiTopoLayout.E_ROOT_PARENT;
import static org.onosproject.ui.model.topo.UiTopoLayout.E_ROOT_REGION;
import static org.onosproject.ui.model.topo.UiTopoLayout.E_SPRITES_SET;
import static org.onosproject.ui.model.topo.UiTopoLayoutId.layoutId;

/**
 * Unit tests for {@link UiTopoLayout}.
 */
public class UiTopoLayoutTest {

    private static final String AM_NOEX = "no exception thrown";
    private static final String AM_WREXMSG = "wrong exception message";

    private static final double DELTA = Double.MIN_VALUE * 2.0;

    private static final Annotations NO_ANNOTS = DefaultAnnotations.EMPTY;
    private static final UiTopoLayoutId OTHER_ID = layoutId("other-id");
    private static final RegionId REGION_ID = regionId("some-region");
    private static final Region REGION =
            new DefaultRegion(REGION_ID, "Region-1", CAMPUS, NO_ANNOTS, null);
    private static final String GEOMAP = "geo1";
    private static final String SPRITE = "spr1";


    private UiTopoLayout layout;

    private void mkRootLayout() {
        layout = new UiTopoLayout(UiTopoLayoutId.DEFAULT_ID);
    }

    private void mkOtherLayout() {
        layout = new UiTopoLayout(OTHER_ID);
    }

    @Test(expected = NullPointerException.class)
    public void nullIdentifier() {
        layout = new UiTopoLayout(null);
    }

    @Test
    public void rootLayout() {
        mkRootLayout();
        assertEquals("wrong id", UiTopoLayoutId.DEFAULT_ID, layout.id());
        assertEquals("wrong parent (not self)",
                UiTopoLayoutId.DEFAULT_ID, layout.parent());
        assertTrue("should be root", layout.isRoot());

        assertNull("unexpected region", layout.region());
        assertEquals("unexpected region id", UiRegion.NULL_ID, layout.regionId());
    }

    @Test
    public void otherLayout() {
        mkOtherLayout();
        assertEquals("wrong id", OTHER_ID, layout.id());
        assertEquals("not null parent", null, layout.parent());
        assertFalse("should NOT be root", layout.isRoot());

        // check attribute default values...
        assertNull("unexpected region", layout.region());
        assertNull("unexpected region id", layout.regionId());
        assertNull("unexpected geomap", layout.geomap());
        assertNull("unexpected sprites", layout.sprites());
        assertEquals("non-unity scale", 1.0, layout.scale(), DELTA);
        assertEquals("non-zero x-off", 0.0, layout.offsetX(), DELTA);
        assertEquals("non-zero y-off", 0.0, layout.offsetY(), DELTA);
    }

    @Test
    public void setRegionOnRoot() {
        mkRootLayout();
        try {
            layout.region(REGION);
            fail(AM_NOEX);
        } catch (IllegalArgumentException e) {
            assertEquals(AM_WREXMSG, E_ROOT_REGION, e.getMessage());
        }

        try {
            layout.region(null);
            fail(AM_NOEX);
        } catch (IllegalArgumentException e) {
            assertEquals(AM_WREXMSG, E_ROOT_REGION, e.getMessage());
        }
    }

    @Test
    public void setRegionOnOther() {
        mkOtherLayout();
        layout.region(REGION);
        assertEquals("wrong region", REGION, layout.region());
        assertEquals("wrong region id", REGION_ID, layout.regionId());

        layout.region(null);
        assertEquals("non-null region", null, layout.region());
        assertEquals("non-null region id", null, layout.regionId());
    }

    @Test
    public void setParentOnRoot() {
        mkRootLayout();
        try {
            layout.parent(OTHER_ID);
            fail(AM_NOEX);
        } catch (IllegalArgumentException e) {
            assertEquals(AM_WREXMSG, E_ROOT_PARENT, e.getMessage());
        }

        try {
            layout.parent(null);
            fail(AM_NOEX);
        } catch (IllegalArgumentException e) {
            assertEquals(AM_WREXMSG, E_ROOT_PARENT, e.getMessage());
        }
    }

    @Test
    public void setParentOnOther() {
        mkOtherLayout();
        layout.parent(OTHER_ID);
        assertEquals("wrong parent", OTHER_ID, layout.parent());

        layout.parent(null);
        assertEquals("non-null parent", null, layout.parent());
    }

    @Test
    public void setGeomap() {
        mkRootLayout();
        assertEquals("geo to start", null, layout.geomap());
        layout.geomap(GEOMAP);
        assertEquals("wrong geo", GEOMAP, layout.geomap());
    }

    @Test
    public void setGeomapAfterSprites() {
        mkRootLayout();
        layout.sprites(SPRITE);
        assertEquals("geo to start", null, layout.geomap());
        try {
            layout.geomap(GEOMAP);
            fail(AM_NOEX);
        } catch (IllegalArgumentException e) {
            assertEquals(AM_WREXMSG, E_SPRITES_SET, e.getMessage());
        }
    }

    @Test
    public void setSprites() {
        mkRootLayout();
        assertEquals("sprite to start", null, layout.sprites());
        layout.sprites(SPRITE);
        assertEquals("wrong sprite", SPRITE, layout.sprites());
    }

    @Test
    public void setSpritesAfterGeomap() {
        mkRootLayout();
        layout.geomap(GEOMAP);
        assertEquals("sprites to start", null, layout.sprites());
        try {
            layout.sprites(SPRITE);
            fail(AM_NOEX);
        } catch (IllegalArgumentException e) {
            assertEquals(AM_WREXMSG, E_GEOMAP_SET, e.getMessage());
        }
    }

    @Test
    public void setScale() {
        mkRootLayout();
        layout.scale(3.0);
        assertEquals("wrong scale", 3.0, layout.scale(), DELTA);
        layout.scale(0.05);
        assertEquals("wrong scale", 0.05, layout.scale(), DELTA);
    }

    @Test(expected = IllegalArgumentException.class)
    public void scaleTooSmall() {
        mkRootLayout();
        layout.scale(0.0099);
    }

    @Test(expected = IllegalArgumentException.class)
    public void scaleTooBig() {
        mkRootLayout();
        layout.scale(100.009);
    }

    @Test
    public void setXOff() {
        mkOtherLayout();
        layout.offsetX(23.4);
        assertEquals("wrong x-offset", 23.4, layout.offsetX(), DELTA);
    }

    @Test
    public void setYOff() {
        mkOtherLayout();
        layout.offsetY(2.71828);
        assertEquals("wrong y-offset", 2.71828, layout.offsetY(), DELTA);
    }

}
