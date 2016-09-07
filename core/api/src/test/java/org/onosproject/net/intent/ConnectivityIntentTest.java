/*
 * Copyright 2014-present Open Networking Laboratory
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

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.TestApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;

/**
 * Base facilities to test various connectivity tests.
 */
public abstract class ConnectivityIntentTest extends IntentTest {

    public static final ApplicationId APPID = new TestApplicationId("foo");
    public static final Key KEY = Key.of(1L, APPID);

    public static final IntentId IID = new IntentId(123);
    public static final TrafficSelector MATCH = DefaultTrafficSelector.emptySelector();
    public static final TrafficTreatment NOP = DefaultTrafficTreatment.emptyTreatment();
    public static final Map<ConnectPoint, TrafficSelector> MATCHES = Collections.emptyMap();
    public static final Map<ConnectPoint, TrafficTreatment> TREATMENTS = Collections.emptyMap();

    public static final ConnectPoint P1 = new ConnectPoint(DeviceId.deviceId("111"), PortNumber.portNumber(0x1));
    public static final ConnectPoint P2 = new ConnectPoint(DeviceId.deviceId("222"), PortNumber.portNumber(0x2));
    public static final ConnectPoint P3 = new ConnectPoint(DeviceId.deviceId("333"), PortNumber.portNumber(0x3));

    public static final Set<ConnectPoint> PS1 = itemSet(new ConnectPoint[]{P1, P3});
    public static final Set<ConnectPoint> PS2 = itemSet(new ConnectPoint[]{P2, P3});

    public static final TrafficSelector VLANMATCH1 = DefaultTrafficSelector.builder()
            .matchVlanId(VlanId.vlanId("2"))
            .build();
    public static final TrafficSelector VLANMATCH2 = DefaultTrafficSelector.builder()
            .matchVlanId(VlanId.vlanId("3"))
            .build();

    public static final Map<ConnectPoint, TrafficSelector> VLANMATCHES = Maps.newHashMap();
    static {
        VLANMATCHES.put(P1, VLANMATCH1);
        VLANMATCHES.put(P2, VLANMATCH2);
    }

    public static final TrafficTreatment VLANACTION1 = DefaultTrafficTreatment.builder()
            .setVlanId(VlanId.vlanId("2"))
            .build();
    public static final TrafficTreatment VLANACTION2 = DefaultTrafficTreatment.builder()
            .setVlanId(VlanId.vlanId("3"))
            .build();

    public static final Map<ConnectPoint, TrafficTreatment> VLANACTIONS = Maps.newHashMap();
    static {
        VLANACTIONS.put(P1, VLANACTION1);
        VLANACTIONS.put(P2, VLANACTION2);
    }

}
