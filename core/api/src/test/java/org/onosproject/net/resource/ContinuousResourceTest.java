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
package org.onosproject.net.resource;

import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.onlab.packet.VlanId;
import org.onlab.util.Bandwidth;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

/**
 * Unit test for ContinuousResource.
 */
public class ContinuousResourceTest {

    private static final DeviceId D1 = DeviceId.deviceId("of:001");
    private static final PortNumber P1 = PortNumber.portNumber(1);
    private static final Bandwidth BW1 = Bandwidth.gbps(2);

    @Test
    public void testEquals() {
        ContinuousResource resource1 = Resources.continuous(D1, P1, Bandwidth.class)
                .resource(BW1.bps());
        ContinuousResource sameAsResource1 = Resources.continuous(D1, P1, Bandwidth.class)
                .resource(BW1.bps());

        new EqualsTester()
                .addEqualityGroup(resource1, sameAsResource1)
                .testEquals();
    }

    @Test
    public void testTypeOf() {
        ContinuousResource continuous = Resources.continuous(D1, P1, Bandwidth.class)
                .resource(BW1.bps());

        assertThat(continuous.isTypeOf(DeviceId.class), is(false));
        assertThat(continuous.isTypeOf(PortNumber.class), is(false));
        assertThat(continuous.isTypeOf(Bandwidth.class), is(true));
    }

    @Test
    public void testSubTypeOf() {
        ContinuousResource continuous = Resources.continuous(D1, P1, Bandwidth.class)
                .resource(BW1.bps());

        assertThat(continuous.isSubTypeOf(DeviceId.class), is(true));
        assertThat(continuous.isSubTypeOf(PortNumber.class), is(true));
        assertThat(continuous.isSubTypeOf(Bandwidth.class), is(true));
        assertThat(continuous.isSubTypeOf(VlanId.class), is(false));
    }

    @Test
    public void testSubTypeOfObject() {
        ContinuousResource continuous = Resources.continuous(D1, P1, Bandwidth.class)
                .resource(BW1.bps());

        assertThat(continuous.isSubTypeOf(Object.class), is(true));
    }

    @Test
    public void testValueAsPrimitiveDouble() {
        ContinuousResource resource = Resources.continuous(D1, P1, Bandwidth.class)
                .resource(BW1.bps());

        Optional<Double> volume = resource.valueAs(double.class);
        assertThat(volume.get(), is(BW1.bps()));
    }

    @Test
    public void testValueAsDouble() {
        ContinuousResource resource = Resources.continuous(D1, P1, Bandwidth.class)
                .resource(BW1.bps());

        Optional<Double> value = resource.valueAs(Double.class);
        assertThat(value.get(), is(BW1.bps()));
    }

    @Test
    public void testValueAsObject() {
        ContinuousResource resource = Resources.continuous(D1, P1, Bandwidth.class)
                .resource(BW1.bps());

        Optional<Double> value = resource.valueAs(Double.class);
        assertThat(value.get(), is(BW1.bps()));
    }

    @Test
    public void testValueAsIncompatibleType() {
        ContinuousResource resource = Resources.continuous(D1, P1, Bandwidth.class)
                .resource(BW1.bps());

        Optional<VlanId> value = resource.valueAs(VlanId.class);
        assertThat(value, is(Optional.empty()));
    }
}
