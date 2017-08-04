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
package org.onosproject.cpman.message;

import com.google.common.collect.Sets;
import org.junit.Test;
import org.onosproject.cpman.ControlMessage;
import org.onosproject.cpman.DefaultControlMessage;
import org.onosproject.event.AbstractEventTest;
import org.onosproject.net.DeviceId;

import java.util.Set;

import static org.onosproject.cpman.ControlMessage.Type.INBOUND_PACKET;
import static org.onosproject.cpman.ControlMessage.Type.OUTBOUND_PACKET;

/**
 * Tests of the control message event.
 */
public class ControlMessageEventTest extends AbstractEventTest {

    private ControlMessage createControlMessage(ControlMessage.Type type,
                                                DeviceId deviceId) {
        return new DefaultControlMessage(type, deviceId, 0L, 0L, 0L, 0L);
    }

    private Set<ControlMessage> createControlMessages() {
        final DeviceId deviceId = DeviceId.deviceId("of:0000000000000001");
        Set<ControlMessage> controlMessages = Sets.newConcurrentHashSet();
        controlMessages.add(createControlMessage(INBOUND_PACKET, deviceId));
        controlMessages.add(createControlMessage(OUTBOUND_PACKET, deviceId));
        return controlMessages;
    }

    @Override
    @Test
    public void withoutTime() {
        Set<ControlMessage> cms = createControlMessages();
        long before = System.currentTimeMillis();
        ControlMessageEvent event =
                new ControlMessageEvent(ControlMessageEvent.Type.STATS_UPDATE, cms);
        long after = System.currentTimeMillis();
        validateEvent(event, ControlMessageEvent.Type.STATS_UPDATE, cms, before, after);
    }
}