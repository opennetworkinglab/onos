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
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.region.RegionId;
import org.onosproject.ui.model.AbstractUiModelTest;

import static org.junit.Assert.assertEquals;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.PortNumber.P0;
import static org.onosproject.net.region.RegionId.regionId;

/**
 * Unit tests for {@link UiRegionLink}.
 */
public class UiRegionLinkTest extends AbstractUiModelTest {

    private static final RegionId R1 = regionId("r1");
    private static final RegionId R2 = regionId("r2");

    private static final DeviceId DEV_X = deviceId("device-X");
    private static final DeviceId DEV_Y = deviceId("device-Y");

    private static final ConnectPoint CP_X = new ConnectPoint(DEV_X, P0);
    private static final ConnectPoint CP_Y = new ConnectPoint(DEV_Y, P0);

    private static final Link LINK_X_TO_Y = DefaultLink.builder()
            .providerId(ProviderId.NONE)
            .src(CP_X)
            .dst(CP_Y)
            .type(Link.Type.DIRECT)
            .build();


    @Test(expected = NullPointerException.class)
    public void nullPointerRegion() {
        title("nullPointerRegion");
        new UiRegionLink(null, null);
    }

    @Test
    public void regionToRegion() {
        title("regionToRegion");
        UiLinkId id = UiLinkId.uiLinkId(R1, R2);
        UiRegionLink link = new UiRegionLink(null, id);
        print("link: %s", link);
        assertEquals("bad first region", R1, link.regionA());
        assertEquals("bad second region", R2, link.regionB());
    }

    @Test(expected = IllegalArgumentException.class)
    public void wrongLinkType() {
        title("wrongLinkType");
        UiLinkId id = UiLinkId.uiLinkId(LINK_X_TO_Y);
        new UiRegionLink(null, id);
    }
}
