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
package org.onosproject.cpman.impl.message;

import org.junit.Test;
import org.onosproject.cpman.ControlMessage;
import org.onosproject.cpman.ControlMetricType;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.onosproject.cpman.impl.ControlMessageMetricMapper.lookupControlMessageType;
import static org.onosproject.cpman.impl.ControlMessageMetricMapper.lookupControlMetricType;

/**
 * Unit test for control message metric mapper.
 */
public final class ControlMessageMetricMapperTest {

    /**
     * Tests whether control message metric mapper returns right control metric type.
     */
    @Test
    public void testLookupControlMetricType() {
        assertThat(lookupControlMetricType(ControlMessage.Type.INBOUND_PACKET),
                    is(ControlMetricType.INBOUND_PACKET));

        assertThat(lookupControlMetricType(ControlMessage.Type.OUTBOUND_PACKET),
                is(ControlMetricType.OUTBOUND_PACKET));

        assertThat(lookupControlMetricType(ControlMessage.Type.FLOW_MOD_PACKET),
                is(ControlMetricType.FLOW_MOD_PACKET));

        assertThat(lookupControlMetricType(ControlMessage.Type.FLOW_REMOVED_PACKET),
                is(ControlMetricType.FLOW_REMOVED_PACKET));

        assertThat(lookupControlMetricType(ControlMessage.Type.REQUEST_PACKET),
                is(ControlMetricType.REQUEST_PACKET));

        assertThat(lookupControlMetricType(ControlMessage.Type.REPLY_PACKET),
                is(ControlMetricType.REPLY_PACKET));
    }

    /**
     * Tests whether control message metric mapper returns right control message type.
     */
    @Test
    public void testLookupControlMessageType() {
        assertThat(lookupControlMessageType(ControlMetricType.INBOUND_PACKET),
                    is(ControlMessage.Type.INBOUND_PACKET));

        assertThat(lookupControlMessageType(ControlMetricType.OUTBOUND_PACKET),
                is(ControlMessage.Type.OUTBOUND_PACKET));

        assertThat(lookupControlMessageType(ControlMetricType.FLOW_MOD_PACKET),
                is(ControlMessage.Type.FLOW_MOD_PACKET));

        assertThat(lookupControlMessageType(ControlMetricType.FLOW_REMOVED_PACKET),
                is(ControlMessage.Type.FLOW_REMOVED_PACKET));

        assertThat(lookupControlMessageType(ControlMetricType.REQUEST_PACKET),
                is(ControlMessage.Type.REQUEST_PACKET));

        assertThat(lookupControlMessageType(ControlMetricType.REPLY_PACKET),
                is(ControlMessage.Type.REPLY_PACKET));
    }
}
