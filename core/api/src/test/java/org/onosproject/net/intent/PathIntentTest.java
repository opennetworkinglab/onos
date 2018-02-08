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
package org.onosproject.net.intent;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;
import org.onlab.graph.ScalarWeight;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.DefaultPath;
import org.onosproject.net.DeviceId;
import org.onosproject.net.NetTestTools;
import org.onosproject.net.Path;
import org.onosproject.net.PortNumber;
import org.onosproject.net.provider.ProviderId;

import static org.junit.Assert.assertEquals;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.Link.Type.DIRECT;
import static org.onosproject.net.PortNumber.portNumber;

public class PathIntentTest extends ConnectivityIntentTest {
    // 111:11 --> 222:22
    private static final Path PATH1 = NetTestTools.createPath("111", "222");

    // 111:11 --> 333:33
    private static final Path PATH2 = NetTestTools.createPath("222", "333");

    private final ProviderId provider1 = new ProviderId("of", "1");
    private final DeviceId device1 = deviceId("1");
    private final DeviceId device2 = deviceId("2");
    private final PortNumber port1 = portNumber(1);
    private final PortNumber port2 = portNumber(2);
    private final PortNumber port3 = portNumber(3);
    private final PortNumber port4 = portNumber(4);
    private final ConnectPoint cp1 = new ConnectPoint(device1, port1);
    private final ConnectPoint cp2 = new ConnectPoint(device1, port2);
    private final ConnectPoint cp3 = new ConnectPoint(device2, port3);
    private final ConnectPoint cp4 = new ConnectPoint(device2, port4);
    private final DefaultLink link1 = DefaultLink.builder()
            .providerId(provider1)
            .src(cp1)
            .dst(cp2)
            .type(DIRECT)
            .build();
    private final DefaultLink link2 = DefaultLink.builder()
            .providerId(provider1)
            .src(cp1)
            .dst(cp2)
            .type(DIRECT)
            .build();
    private final double cost = 1;

    @Test
    public void basics() {
        PathIntent intent = createOne();
        assertEquals("incorrect id", APPID, intent.appId());
        assertEquals("incorrect match", MATCH, intent.selector());
        assertEquals("incorrect action", NOP, intent.treatment());
        assertEquals("incorrect path", PATH1, intent.path());
        assertEquals("incorrect key", KEY, intent.key());

        intent = createAnother();
        assertEquals("incorrect id", APPID, intent.appId());
        assertEquals("incorrect match", MATCH, intent.selector());
        assertEquals("incorrect action", NOP, intent.treatment());
        assertEquals("incorrect path", PATH2, intent.path());
        assertEquals("incorrect key", KEY, intent.key());

        intent = createWithResourceGroup();
        assertEquals("incorrect id", APPID, intent.appId());
        assertEquals("incorrect match", MATCH, intent.selector());
        assertEquals("incorrect action", NOP, intent.treatment());
        assertEquals("incorrect path", PATH2, intent.path());
        assertEquals("incorrect key", KEY, intent.key());
        assertEquals("incorrect resource group", RESOURCE_GROUP, intent.resourceGroup());
    }

    @Override
    protected PathIntent createOne() {
        return PathIntent.builder()
                .appId(APPID)
                .key(KEY)
                .selector(MATCH)
                .treatment(NOP)
                .path(PATH1)
                .build();
    }

    @Override
    protected PathIntent createAnother() {
        return PathIntent.builder()
                .appId(APPID)
                .key(KEY)
                .selector(MATCH)
                .treatment(NOP)
                .path(PATH2)
                .build();
    }

    protected PathIntent createWithResourceGroup() {
        return PathIntent.builder()
                .appId(APPID)
                .key(KEY)
                .selector(MATCH)
                .treatment(NOP)
                .path(PATH2)
                .resourceGroup(RESOURCE_GROUP)
                .build();
    }

    /**
     * Tests the constructor raises IllegalArgumentException when the same device is specified in
     * source and destination of a link.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testRaiseExceptionWhenSameDevices() {
        PathIntent.builder()
                .appId(APPID)
                .selector(MATCH)
                .treatment(NOP)
                .path(new DefaultPath(provider1, Collections.singletonList(link1), ScalarWeight.toWeight(cost)))
                .build();
    }

    /**
     * Tests the constructor raises IllegalArgumentException when the different elements are specified
     * in source element of the first link and destination element of the second link.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testRaiseExceptionWhenDifferentDevice() {
        PathIntent.builder()
                .appId(APPID)
                .selector(MATCH)
                .treatment(NOP)
                .path(new DefaultPath(provider1, Arrays.asList(link1, link2), ScalarWeight.toWeight(cost)))
                .build();
    }

}
