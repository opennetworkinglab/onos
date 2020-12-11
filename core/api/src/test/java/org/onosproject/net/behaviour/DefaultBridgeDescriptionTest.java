/*
 * Copyright 2015-present Open Networking Foundation
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

package org.onosproject.net.behaviour;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onosproject.net.DeviceId;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for DefaultBridgeDescription.
 */

public class DefaultBridgeDescriptionTest {
    private final BridgeDescription.Builder build = DefaultBridgeDescription.builder();
    private final String name = "expected";
    private final IpAddress ip = IpAddress.valueOf("0.0.0.0");
    private final IpAddress ip2 = IpAddress.valueOf("0.0.0.1");
    private final IpAddress ip3 = IpAddress.valueOf("0.0.0.2");
    private final ControllerInfo controller1 = new ControllerInfo(ip, 6653, "test");
    private final ControllerInfo controller2 = new ControllerInfo(ip2, 6654, "test");
    private final ControllerInfo controller3 = new ControllerInfo(ip3, 6655, "test");
    private final List<ControllerInfo> controllers = Lists.newArrayList(controller1,
                                                                        controller2,
                                                                        controller3);
    private final BridgeDescription.FailMode mode = BridgeDescription.FailMode.SECURE;
    private final String id = "foo";
    private final String type = "bar";

    /**
     * Tests all the getter methods in DefaultBridgeDescription.
     */
    @Test
    public void testGet() {

        build.name(name);
        build.controllers(controllers);
        build.datapathId(id);
        build.datapathType(type);
        build.failMode(mode);
        build.disableInBand();
        build.mcastSnoopingEnable();
        build.enableLocalController();
        BridgeDescription test;
        test = build.build();

        assertThat(test.name(), is("expected"));
        assertThat(test.controllers(), is(controllers));
        assertThat(test.datapathId().get(), is(id));
        assertThat(test.datapathType().get(), is(type));
        assertThat(test.failMode().get(), is(mode));
        assertThat(test.disableInBand().get(), is(true));
        assertThat(test.mcastSnoopingEnable().get(), is(true));
        assertNull(test.annotations());
        assertTrue(test.enableLocalController());
        assertThat(test.deviceId().get(), is(DeviceId.deviceId("of:" + id)));
    }
}

