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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.config.NetworkConfigRegistryAdapter;
import org.onosproject.net.region.DefaultRegion;
import org.onosproject.net.region.Region;
import org.onosproject.ui.UiTopoLayoutService;
import org.onosproject.ui.model.topo.UiTopoLayout;
import org.onosproject.ui.model.topo.UiTopoLayoutId;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.onosproject.net.region.Region.Type.CAMPUS;
import static org.onosproject.net.region.RegionId.regionId;
import static org.onosproject.ui.model.topo.UiTopoLayoutId.layoutId;

/**
 * Suite of unit tests for the UI topology layout manager.
 */
public class UiTopoLayoutManagerTest {

    private static class MockConfigService extends NetworkConfigRegistryAdapter {
    }

    private static Region region(String id, String name, Region.Type type) {
        return new DefaultRegion(regionId(id), name, type,
                DefaultAnnotations.EMPTY, null);
    }

    private static UiTopoLayout layout(String id, Region region, String parentId) {
        UiTopoLayoutId parent = parentId == null ? null : layoutId(parentId);
        UiTopoLayout layout = new UiTopoLayout(layoutId(id));
        // TODO: set region and parent
        return layout;
    }

    private static final Region R1 = region("r1", "R1", CAMPUS);
    private static final Region R2 = region("r2", "R2", CAMPUS);

    private static final UiTopoLayout L1 = layout("l1", R1, null);
    private static final UiTopoLayout L2 = layout("l2", R2, null);


    private UiTopoLayoutService svc;
    private UiTopoLayoutManager mgr;


    @Before
    public void setUp() {
        mgr = new UiTopoLayoutManager();
        svc = mgr;

        mgr.cfgService = new MockConfigService();
        mgr.activate();
    }

    @After
    public void tearDown() {
        mgr.deactivate();
        mgr.cfgService = null;
    }

    @Test
    public void basics() {
        assertEquals("should be just default layout", 1, svc.getLayouts().size());
        svc.addLayout(L1);
        svc.addLayout(L2);
        assertEquals("incorrect number of layouts", 3, svc.getLayouts().size());
        assertEquals("incorrect layout", L1.id(), svc.getLayout(L1.id()).id());
        assertEquals("incorrect layout", L2.id(), svc.getLayout(L2.id()).id());
        svc.removeLayout(L1);
        assertEquals("incorrect number of layouts", 2, svc.getLayouts().size());
        assertNull("layout should be gone", svc.getLayout(L1.id()));
        assertEquals("incorrect layout", L2.id(), svc.getLayout(L2.id()).id());
    }


}