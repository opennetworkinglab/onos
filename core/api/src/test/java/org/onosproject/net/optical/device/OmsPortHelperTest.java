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
package org.onosproject.net.optical.device;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.Optional;

import org.junit.Test;
import org.onlab.packet.ChassisId;
import org.onlab.util.Frequency;
import org.onosproject.net.Annotations;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.DefaultPort;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.optical.OmsPort;
import org.onosproject.net.Device.Type;
import org.onosproject.net.provider.ProviderId;

/**
 * Tests for {@link OmsPortHelper}.
 */
public class OmsPortHelperTest {

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
    public void testOmsPortDescriptionCanBeConvertedToOmsPort() {
        PortNumber pn = PortNumber.portNumber(4900);

        boolean isEnabled = true;
        String anKey = "Base";
        String anValue = "value";
        SparseAnnotations an = DefaultAnnotations.builder()
                .set(anKey, anValue)
                .build();

        Frequency minF = Frequency.ofGHz(3);
        Frequency maxF = Frequency.ofGHz(33);
        Frequency grid = Frequency.ofGHz(2);

        PortDescription portDescription = OmsPortHelper.omsPortDescription(pn, isEnabled, minF, maxF, grid, an);
        Port port = new DefaultPort(DEV,
                                    portDescription.portNumber(),
                                    portDescription.isEnabled(),
                                    portDescription.type(),
                                    portDescription.portSpeed(),
                                    portDescription.annotations());

        Optional<OmsPort> maybeOms = OmsPortHelper.asOmsPort(port);
        assertTrue(maybeOms.isPresent());

        OmsPort oms = maybeOms.get();

        assertThat(oms.isEnabled(), is(isEnabled));
        assertThat(oms.number(), is(pn));
        assertThat(oms.annotations().value(anKey), is(anValue));

        assertThat("type is always OMS", oms.type(), is(Port.Type.OMS));
        assertThat("port speed is undefined", oms.portSpeed(), is(equalTo(0L)));

        assertThat(oms.maxFrequency(), is(maxF));
        assertThat(oms.minFrequency(), is(minF));
        assertThat(oms.grid(), is(grid));
        assertThat("((33-3)/2)+1 = 16", oms.totalChannels(), is((short) 16));
    }

}
