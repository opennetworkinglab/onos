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

package org.onosproject.net.config.basics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.net.config.InvalidFieldException;
import org.onosproject.net.region.RegionId;
import org.onosproject.ui.model.topo.UiTopoLayoutId;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.onosproject.net.config.basics.BasicElementConfig.ZERO_THRESHOLD;
import static org.onosproject.net.config.basics.BasicUiTopoLayoutConfig.GEOMAP;
import static org.onosproject.net.config.basics.BasicUiTopoLayoutConfig.OFFSET_X;
import static org.onosproject.net.config.basics.BasicUiTopoLayoutConfig.OFFSET_Y;
import static org.onosproject.net.config.basics.BasicUiTopoLayoutConfig.SCALE;
import static org.onosproject.net.config.basics.BasicUiTopoLayoutConfig.SPRITES;
import static org.onosproject.net.region.RegionId.regionId;
import static org.onosproject.ui.model.topo.UiTopoLayoutId.layoutId;

/**
 * Test class for {@link BasicUiTopoLayoutConfig}.
 */
public class BasicUiTopoLayoutConfigTest extends AbstractConfigTest {

    private static final String LAYOUT_JSON = "configs.layouts.1.json";

    private static final String L_DEFAULT = "root";
    private static final String L1 = "l1";
    private static final String L2 = "l2";
    private static final String L3 = "l3";

    private static final RegionId R1 = regionId("r1");
    private static final RegionId R2 = regionId("r2");
    private static final RegionId R3 = regionId("r3");

    private static final String UK = "uk";
    private static final String UK_BRIGHTON = "uk-brighton";
    private static final String UK_LONDON = "uk-london";
    private static final String UK_LONDON_WEST = "uk-london-westminster";

    private static final RegionId NEW_REGION = regionId("new-region");
    private static final UiTopoLayoutId NEW_PARENT = layoutId("new-parent");
    private static final String NEW_MAP = "new-geomap";
    private static final String NEW_SPR = "new-sprites";


    private JsonNode data;
    private BasicUiTopoLayoutConfig cfg;

    @Before
    public void setUp() {
        data = getTestJson(LAYOUT_JSON);
    }

    private JsonNode getL(String key) {
        return data.get("layouts").get(key).get("basic");
    }

    private void loadLayout(String lid) {
        JsonNode node = getL(lid);
        print(JSON_LOADED, node);

        cfg = new BasicUiTopoLayoutConfig();
        cfg.init(layoutId(lid), lid, node, mapper, delegate);
    }

    private void checkLayout(RegionId expRegion, UiTopoLayoutId expParent,
                             String expGeo, String expSpr) {
        print(CHECKING_S, cfg);
        assertEquals("wrong region", expRegion, cfg.region());
        assertEquals("wrong parent", expParent, cfg.parent());
        assertEquals("wrong geomap", expGeo, cfg.geomap());
        assertEquals("wrong sprites", expSpr, cfg.sprites());
    }

    private void checkScale(double expScale, double expOffx, double expOffy) {
        assertEquals(SCALE, expScale, cfg.scale(), ZERO_THRESHOLD);
        assertEquals(OFFSET_X, expOffx, cfg.offsetX(), ZERO_THRESHOLD);
        assertEquals(OFFSET_Y, expOffy, cfg.offsetY(), ZERO_THRESHOLD);
    }

    @Test
    public void layoutConfigDefault() {
        loadLayout(L_DEFAULT);
        checkLayout(null, UiTopoLayoutId.DEFAULT_ID, UK, null);
        checkScale(1.2, -50, 0);
    }

    @Test
    public void layoutConfigOne() {
        loadLayout(L1);
        checkLayout(R1, UiTopoLayoutId.DEFAULT_ID, UK_BRIGHTON, null);
        checkScale(0.9, 200, -45);
    }

    @Test
    public void layoutConfigTwo() {
        loadLayout(L2);
        checkLayout(R2, UiTopoLayoutId.DEFAULT_ID, UK_LONDON, null);
        checkScale(1, 0, 0);
    }

    @Test
    public void layoutConfigThree() {
        loadLayout(L3);
        checkLayout(R3, layoutId(L2), null, UK_LONDON_WEST);
        checkScale(1, 0, 0);
    }

    private ObjectNode tmpNode(String... props) {
        return new TmpJson().props(props).node();
    }

    private BasicUiTopoLayoutConfig cfgFromJson(ObjectNode json) {
        BasicUiTopoLayoutConfig cfg = new BasicUiTopoLayoutConfig();
        cfg.init(layoutId("foo"), BASIC, json, mapper, delegate);
        return cfg;
    }

    @Test(expected = InvalidFieldException.class)
    public void cantHaveGeoAndSprite() {
        cfg = cfgFromJson(tmpNode(GEOMAP, SPRITES));
        cfg.isValid();
    }

    @Test(expected = InvalidFieldException.class)
    public void cantSetGeoIfSpritesAreSet() {
        cfg = cfgFromJson(tmpNode(SPRITES));
        cfg.geomap("map-name");
    }

    @Test(expected = InvalidFieldException.class)
    public void cantSetSpritesIfGeoIsSet() {
        cfg = cfgFromJson(tmpNode(GEOMAP));
        cfg.sprites("sprites-name");
    }

    @Test
    public void setRegion() {
        loadLayout(L1);
        assertEquals("not region-1", R1, cfg.region());
        cfg.region(NEW_REGION);
        assertEquals("not new region", NEW_REGION, cfg.region());
        cfg.region(null);
        assertNull("region not cleared", cfg.region());
    }

    @Test
    public void setParent() {
        loadLayout(L1);
        assertEquals("parent not default layout", UiTopoLayoutId.DEFAULT_ID, cfg.parent());
        cfg.parent(NEW_PARENT);
        assertEquals("not new parent", NEW_PARENT, cfg.parent());
        cfg.parent(null);
        assertEquals("parent not reset to default", UiTopoLayoutId.DEFAULT_ID, cfg.parent());
    }

    @Test
    public void setGeomap() {
        loadLayout(L1);
        assertEquals("map not brighton", UK_BRIGHTON, cfg.geomap());
        cfg.geomap(NEW_MAP);
        assertEquals("not new map", NEW_MAP, cfg.geomap());
        cfg.geomap(null);
        assertNull("geomap not cleared", cfg.geomap());
    }

    @Test
    public void setSprites() {
        loadLayout(L3);
        assertEquals("sprites not westminster", UK_LONDON_WEST, cfg.sprites());
        cfg.sprites(NEW_SPR);
        assertEquals("not new sprites", NEW_SPR, cfg.sprites());
        cfg.sprites(null);
        assertNull("sprites not cleared", cfg.sprites());
    }

    @Test
    public void setScaleAndOffset() {
        loadLayout(L1);
        assertEquals("wrong init scale", 0.9, cfg.scale(), ZERO_THRESHOLD);
        assertEquals("wrong init x-offset", 200, cfg.offsetX(), ZERO_THRESHOLD);
        assertEquals("wrong init y-offset", -45, cfg.offsetY(), ZERO_THRESHOLD);
        cfg.scale(3.14).offsetX(12.0).offsetY(13.0);
        assertEquals("wrong new scale", 3.14, cfg.scale(), ZERO_THRESHOLD);
        assertEquals("wrong new x-offset", 12, cfg.offsetX(), ZERO_THRESHOLD);
        assertEquals("wrong new y-offset", 13, cfg.offsetY(), ZERO_THRESHOLD);
        cfg.scale(null).offsetX(null).offsetY(null);
        assertEquals("wrong default scale", 1, cfg.scale(), ZERO_THRESHOLD);
        assertEquals("wrong default x-offset", 0, cfg.offsetX(), ZERO_THRESHOLD);
        assertEquals("wrong default y-offset", 0, cfg.offsetY(), ZERO_THRESHOLD);
    }
}
