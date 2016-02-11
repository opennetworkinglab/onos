/*
 * Copyright 2016 Open Networking Laboratory
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

import org.junit.Test;
import org.onosproject.cpman.ControlMessage;
import org.onosproject.cpman.DefaultControlMessage;
import org.onosproject.event.AbstractEventTest;

import java.util.ArrayList;
import java.util.Collection;

import static org.onosproject.cpman.ControlMessage.Type.*;

/**
 * Tests of the control message event.
 */
public class ControlMessageEventTest extends AbstractEventTest {

    private ControlMessage createControlMessage(ControlMessage.Type type) {
        return new DefaultControlMessage(type, 0L, 0L, 0L, 0L);
    }

    private Collection<ControlMessage> createControlMessages() {
        Collection<ControlMessage> controlMessages = new ArrayList<>();
        controlMessages.add(createControlMessage(INBOUND_PACKET));
        controlMessages.add(createControlMessage(OUTBOUND_PACKET));
        return controlMessages;
    }

    @Override
    @Test
    public void withoutTime() {
        Collection<ControlMessage> cms = createControlMessages();
        long before = System.currentTimeMillis();
        ControlMessageEvent event =
                new ControlMessageEvent(ControlMessageEvent.Type.STATS_UPDATE, cms);
        long after = System.currentTimeMillis();
        validateEvent(event, ControlMessageEvent.Type.STATS_UPDATE, cms, before, after);
    }
}