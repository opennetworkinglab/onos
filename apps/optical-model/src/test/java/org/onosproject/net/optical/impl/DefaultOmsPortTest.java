/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.net.optical.impl;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.onosproject.net.PortNumber.portNumber;

import org.junit.Test;
import org.onlab.packet.ChassisId;
import org.onlab.util.Frequency;
import org.onosproject.net.Annotations;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.DefaultPort;
import org.onosproject.net.Device;
import org.onosproject.net.Device.Type;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.optical.OmsPort;
import org.onosproject.net.provider.ProviderId;

import com.google.common.testing.EqualsTester;

/**
 * Tests for {@link DefaultOmsPort}.
 */
public class DefaultOmsPortTest {

    private static final ProviderId PID = new ProviderId("test", "id");
    private static final DeviceId DID = DeviceId.deviceId("test:00123");
    private static final String MFC = "MFC";
    private static final String HW = "HW V";
    private static final String SW = "SW V";
    private static final String SER = "SER";
    private static final ChassisId CHS = new ChassisId(42);
    private static final Annotations DEV_ANON = DefaultAnnotations.EMPTY;
    private static final Device DEV = new DefaultDevice(PID, DID, Type.ROADM, MFC, HW, SW, SER, CHS, DEV_ANON);

    @Test
    public void testEquality() {
        PortNumber pn = PortNumber.portNumber(4900);
        Annotations an = DefaultAnnotations.builder()
                .set("Base", "value")
                .build();
        Annotations an2 = DefaultAnnotations.builder()
                .set("Base", "value2")
                .build();

        Port base = new DefaultPort(DEV, pn, true, Port.Type.VIRTUAL, 2, an);
        Frequency minF = Frequency.ofGHz(3);
        Frequency maxF = Frequency.ofGHz(33);
        Frequency grid = Frequency.ofGHz(2);

        // reference OMS port
        OmsPort oms = new DefaultOmsPort(base, minF, maxF, grid);

        new EqualsTester()
          .addEqualityGroup(oms,
                            // different base port type or portspeed is ignored
                            new DefaultOmsPort(new DefaultPort(DEV, pn, true, an), minF, maxF, grid))
          // different port number
          .addEqualityGroup(new DefaultOmsPort(new DefaultPort(DEV, portNumber(1), true, an), minF, maxF, grid))
          // different isEnabled
          .addEqualityGroup(new DefaultOmsPort(new DefaultPort(DEV, pn, false, an), minF, maxF, grid))
          // different annotation
          .addEqualityGroup(new DefaultOmsPort(new DefaultPort(DEV, pn, true, an2), minF, maxF, grid))
          // different minFreq
          .addEqualityGroup(new DefaultOmsPort(base, Frequency.ofKHz(3), maxF, grid))
          // different maxFreq
          .addEqualityGroup(new DefaultOmsPort(base, minF, Frequency.ofKHz(33), grid))
          // different grid
          .addEqualityGroup(new DefaultOmsPort(base, minF, maxF, Frequency.ofKHz(2)))
          .testEquals();

    }

    @Test
    public void basicTests() {
        PortNumber pn = PortNumber.portNumber(4900);
        Annotations annotations = DefaultAnnotations.builder()
                                .set("Base", "value")
                                .build();

        boolean isEnabled = true;
        Port base = new DefaultPort(DEV, pn, isEnabled, Port.Type.VIRTUAL, 2, annotations);
        Frequency minFrequency = Frequency.ofGHz(3);
        Frequency maxFrequency = Frequency.ofGHz(33);
        Frequency grid = Frequency.ofGHz(2);
        OmsPort oms = new DefaultOmsPort(base, minFrequency, maxFrequency, grid);

        // basic attributes and annotations are inherited from base
        assertThat(oms.element(), is(DEV));
        assertThat(oms.isEnabled(), is(isEnabled));
        assertThat(oms.number(), is(pn));
        assertThat(oms.annotations(), is(annotations));

        assertThat("type is always OMS", oms.type(), is(Port.Type.OMS));
        assertThat("port speed is undefined", oms.portSpeed(), is(equalTo(0L)));

        assertThat(oms.maxFrequency(), is(maxFrequency));
        assertThat(oms.minFrequency(), is(minFrequency));
        assertThat(oms.grid(), is(grid));
        assertThat("((33-3)/2)+1 = 16", oms.totalChannels(), is((short) 16));
    }

}
