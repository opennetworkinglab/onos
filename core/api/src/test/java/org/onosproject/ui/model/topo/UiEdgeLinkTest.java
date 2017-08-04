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
import org.onosproject.net.DefaultEdgeLink;
import org.onosproject.net.DeviceId;
import org.onosproject.net.EdgeLink;
import org.onosproject.net.PortNumber;
import org.onosproject.ui.model.AbstractUiModelTest;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link UiEdgeLink}.
 */
public class UiEdgeLinkTest extends AbstractUiModelTest {

    private static final String PHANTOM_HOST_ID = "00:00:00:00:00:00/None";
    private static final String S_D1 = "dev-1";
    private static final String S_P8 = "8";

    private static final DeviceId DEV = DeviceId.deviceId("dev-1");
    private static final PortNumber P8 = PortNumber.portNumber(8);
    private static final ConnectPoint CP = new ConnectPoint(DEV, P8);

    private static final EdgeLink EDGE_LINK =
            DefaultEdgeLink.createEdgeLink(CP, true);

    @Test
    public void basic() {
        title("basic");
        UiLinkId id = UiLinkId.uiLinkId(EDGE_LINK);
        UiEdgeLink link = new UiEdgeLink(null, id);
        print(link);
        print(link.endPointA());
        print(link.endPortA());
        print(link.endPointB());
        print(link.endPortB());

        assertEquals("bad end point A", PHANTOM_HOST_ID, link.endPointA());
        assertEquals("bad end port A", null, link.endPortA());

        assertEquals("bad end point B", S_D1, link.endPointB());
        assertEquals("bad end port B", S_P8, link.endPortB());
    }
}
