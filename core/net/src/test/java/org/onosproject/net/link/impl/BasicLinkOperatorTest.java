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
package org.onosproject.net.link.impl;

import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.PortNumber.portNumber;
import static org.junit.Assert.assertEquals;

import java.time.Duration;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.net.config.ConfigApplyDelegate;
import org.onosproject.net.config.basics.BasicLinkConfig;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.LinkKey;
import org.onosproject.net.PortNumber;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.link.DefaultLinkDescription;
import org.onosproject.net.link.LinkDescription;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

public class BasicLinkOperatorTest {

    private static final DeviceId DID1 = deviceId("of:foo");
    private static final DeviceId DID2 = deviceId("of:bar");
    private static final PortNumber P1 = portNumber(1);

    private static final ConnectPoint SRC = new ConnectPoint(DID1, P1);
    private static final ConnectPoint DST = new ConnectPoint(DID2, P1);
    private static final LinkKey LK = LinkKey.linkKey(SRC, DST);
    private static final long NTIME = 200;

    private static final SparseAnnotations SA = DefaultAnnotations.builder()
            .set(AnnotationKeys.DURABLE, "true").build();
    private static final LinkDescription LD = new DefaultLinkDescription(SRC, DST, Link.Type.DIRECT, SA);
    private final ConfigApplyDelegate delegate = config -> { };
    private final ObjectMapper mapper = new ObjectMapper();

    private static final BasicLinkConfig BLC = new BasicLinkConfig();

    @Before
    public void setUp() {
        BLC.init(LK, "optest", JsonNodeFactory.instance.objectNode(), mapper, delegate);
        BLC.latency(Duration.ofNanos(NTIME));
    }

    @Test
    public void testDescOps() {
        LinkDescription desc = BasicLinkOperator.combine(BLC, LD);
        assertEquals(String.valueOf(NTIME), desc.annotations().value(AnnotationKeys.LATENCY));
        assertEquals("true", desc.annotations().value(AnnotationKeys.DURABLE));
    }
}
