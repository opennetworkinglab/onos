/*
 * Copyright 2015-present Open Networking Laboratory
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

package org.onosproject.codec.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.is;
import static org.onosproject.codec.impl.JsonCodecUtils.assertJsonEncodable;

import org.junit.Test;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.Device;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.DeviceServiceAdapter;

/**
 * Unit test for DeviceCodec.
 */
public class DeviceCodecTest {

    private Device device = new DefaultDevice(JsonCodecUtils.PID,
                                              JsonCodecUtils.DID1,
                                              Device.Type.SWITCH,
                                              JsonCodecUtils.MFR,
                                              JsonCodecUtils.HW,
                                              JsonCodecUtils.SW1,
                                              JsonCodecUtils.SN,
                                              JsonCodecUtils.CID,
                                              JsonCodecUtils.A1);



    @Test
    public void deviceCodecTest() {
        final MockCodecContext context = new MockCodecContext();
        context.registerService(DeviceService.class, new DeviceServiceAdapter());
        final JsonCodec<Device> codec = context.codec(Device.class);
        assertThat(codec, is(notNullValue()));
        final Device pojoIn = device;

        assertJsonEncodable(context, codec, pojoIn);
    }

}
