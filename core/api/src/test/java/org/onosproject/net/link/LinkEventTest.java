/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onosproject.net.link;

import org.junit.Test;
import org.onosproject.event.AbstractEventTest;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.Link;
import org.onosproject.net.provider.ProviderId;

import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.PortNumber.portNumber;

/**
 * Tests of the device event.
 */
public class LinkEventTest extends AbstractEventTest {

    private Link createLink() {
        return DefaultLink.builder()
                .providerId(new ProviderId("of", "foo"))
                .src(new ConnectPoint(deviceId("of:foo"), portNumber(1)))
                .dst(new ConnectPoint(deviceId("of:bar"), portNumber(2)))
                .type(Link.Type.INDIRECT)
                .build();
    }

    @Test
    public void withTime() {
        Link link = createLink();
        LinkEvent event = new LinkEvent(LinkEvent.Type.LINK_ADDED, link, 123L);
        validateEvent(event, LinkEvent.Type.LINK_ADDED, link, 123L);
    }

    @Test
    public void withoutTime() {
        Link link = createLink();
        long before = System.currentTimeMillis();
        LinkEvent event = new LinkEvent(LinkEvent.Type.LINK_ADDED, link);
        long after = System.currentTimeMillis();
        validateEvent(event, LinkEvent.Type.LINK_ADDED, link, before, after);
    }

}
