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

package org.onosproject.ui.model.topo;

import org.junit.Test;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.ui.model.AbstractUiModelTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.PortNumber.portNumber;

/**
 * Unit tests for {@link UiLinkId}.
 */
public class UiLinkIdTest extends AbstractUiModelTest {

    private static final DeviceId DEV_X = deviceId("device-X");
    private static final DeviceId DEV_Y = deviceId("device-Y");
    private static final PortNumber P1 = portNumber(1);
    private static final PortNumber P2 = portNumber(2);
    private static final PortNumber P3 = portNumber(3);

    private static final ConnectPoint CP_X1 = new ConnectPoint(DEV_X, P1);
    private static final ConnectPoint CP_Y2 = new ConnectPoint(DEV_Y, P2);
    private static final ConnectPoint CP_Y3 = new ConnectPoint(DEV_Y, P3);

    private static final Link LINK_X1_TO_Y2 = DefaultLink.builder()
            .providerId(ProviderId.NONE)
            .src(CP_X1)
            .dst(CP_Y2)
            .type(Link.Type.DIRECT)
            .build();

    private static final Link LINK_Y2_TO_X1 = DefaultLink.builder()
            .providerId(ProviderId.NONE)
            .src(CP_Y2)
            .dst(CP_X1)
            .type(Link.Type.DIRECT)
            .build();

    private static final Link LINK_X1_TO_Y3 = DefaultLink.builder()
            .providerId(ProviderId.NONE)
            .src(CP_X1)
            .dst(CP_Y3)
            .type(Link.Type.DIRECT)
            .build();


    @Test
    public void canonical() {
        title("canonical");
        UiLinkId one = UiLinkId.uiLinkId(LINK_X1_TO_Y2);
        UiLinkId two = UiLinkId.uiLinkId(LINK_Y2_TO_X1);
        print("link one: %s", one);
        print("link two: %s", two);
        assertEquals("not equiv", one, two);
    }

    @Test
    public void sameDevsDiffPorts() {
        title("sameDevsDiffPorts");
        UiLinkId one = UiLinkId.uiLinkId(LINK_X1_TO_Y2);
        UiLinkId other = UiLinkId.uiLinkId(LINK_X1_TO_Y3);
        print("link one: %s", one);
        print("link other: %s", other);
        assertNotEquals("equiv?", one, other);
    }
}
