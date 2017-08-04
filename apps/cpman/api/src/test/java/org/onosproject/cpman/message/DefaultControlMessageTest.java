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

import org.junit.Test;
import org.onosproject.cpman.DefaultControlMessage;
import org.onosproject.net.DeviceId;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutableBaseClass;
import static org.onosproject.cpman.ControlMessage.Type.INBOUND_PACKET;

/**
 * Unit tests for the default control message class.
 */
public class DefaultControlMessageTest {

    /**
     * Checks that the DefaultControlMessage class is immutable but can be
     * inherited from.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutableBaseClass(DefaultControlMessage.class);
    }

    /**
     * Tests creation of a DefaultControlMessage using a regular constructor.
     */
    @Test
    public void testBasic() {
        final DeviceId deviceId = DeviceId.deviceId("of:0000000000000001");
        final DefaultControlMessage cm =
                new DefaultControlMessage(INBOUND_PACKET, deviceId, 0L, 1L, 2L, 3L);
        assertThat(cm.type(), is(INBOUND_PACKET));
        assertThat(cm.load(), is(0L));
        assertThat(cm.rate(), is(1L));
        assertThat(cm.count(), is(2L));
        assertThat(cm.timestamp(), is(3L));
    }
}
