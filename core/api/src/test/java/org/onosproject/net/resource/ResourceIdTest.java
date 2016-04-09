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
package org.onosproject.net.resource;

import org.junit.Test;
import org.onlab.util.Bandwidth;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

import java.util.Arrays;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ResourceIdTest {
    private static final DeviceId D1 = DeviceId.deviceId("a");
    private static final PortNumber P1 = PortNumber.portNumber(1);
    private static final Bandwidth BW1 = Bandwidth.gbps(1);

    @Test
    public void testDiscreteToString() {
        ResourceId resource = Resources.discrete(D1, P1).id();

        assertThat(resource.toString(), is(Arrays.asList(D1, P1).toString()));
    }

    @Test
    public void testContinuousToString() {
        ResourceId resource = Resources.continuous(D1, P1, Bandwidth.class).id();

        assertThat(resource.toString(), is(Arrays.asList(D1, P1, Bandwidth.class.getSimpleName()).toString()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInitWithNonClassInstance() {
        Resources.continuous(D1, P1, BW1).id();
    }
}
