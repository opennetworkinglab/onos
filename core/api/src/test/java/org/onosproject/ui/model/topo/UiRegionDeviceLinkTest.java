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
import org.onosproject.net.DeviceId;
import org.onosproject.net.region.RegionId;
import org.onosproject.ui.model.AbstractUiModelTest;

import static org.junit.Assert.assertEquals;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.PortNumber.P0;
import static org.onosproject.net.region.RegionId.regionId;

/**
 * Unit tests for {@link UiRegionDeviceLink}.
 */
public class UiRegionDeviceLinkTest extends AbstractUiModelTest {

    private static final RegionId R1 = regionId("r1");
    private static final DeviceId DEV_X = deviceId("device-X");

    @Test
    public void basic() {
        title("basic");
        UiLinkId id = UiLinkId.uiLinkId(R1, DEV_X, P0);
        UiRegionDeviceLink link = new UiRegionDeviceLink(null, id);
        print(link);
        assertEquals("region", R1, link.region());
        assertEquals("device", DEV_X, link.device());
        assertEquals("port", P0, link.port());
    }
}
