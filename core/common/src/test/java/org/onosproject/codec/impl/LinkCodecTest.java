/*
 * Copyright 2015 Open Networking Laboratory
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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.onosproject.codec.impl.JsonCodecUtils.assertJsonEncodable;

import org.junit.Test;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.Link;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.DeviceServiceAdapter;

/**
 * Unit test for LinkCodec.
 */
public class LinkCodecTest {

    private final Link link = new DefaultLink(JsonCodecUtils.PID,
                                              JsonCodecUtils.CP1,
                                              JsonCodecUtils.CP2,
                                              Link.Type.DIRECT,
                                              Link.State.ACTIVE,
                                              false,
                                              JsonCodecUtils.A1);

    @Test
    public void linkCodecTest() {
        final MockCodecContext context = new MockCodecContext();
        context.registerService(DeviceService.class, new DeviceServiceAdapter());
        final JsonCodec<Link> codec = context.codec(Link.class);
        assertThat(codec, is(notNullValue()));
        final Link pojoIn = link;

        assertJsonEncodable(context, codec, pojoIn);
    }
}
