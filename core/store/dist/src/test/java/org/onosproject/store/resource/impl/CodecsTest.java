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
package org.onosproject.store.resource.impl;

import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.MplsLabel;
import org.onlab.packet.VlanId;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.DeviceId;
import org.onosproject.net.OchSignal;
import org.onosproject.net.PortNumber;
import org.onosproject.net.resource.DiscreteResource;
import org.onosproject.net.resource.Resource;
import org.onosproject.net.resource.Resources;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Unit tests for Codecs.
 */
public class CodecsTest {

    private static final DeviceId DID = DeviceId.deviceId("a");
    private static final PortNumber PN = PortNumber.portNumber(1);
    private static final VlanId VLAN = VlanId.vlanId((short) 1);
    private static final MplsLabel MPLS = MplsLabel.mplsLabel(1);
    private static final OchSignal OCH = OchSignal.newDwdmSlot(ChannelSpacing.CHL_50GHZ, 1);

    private Codecs sut;

    @Before
    public void setUp() {
        sut = Codecs.getInstance();
    }

    /**
     * Checks that it's possible to encode a VLAN ID.
     */
    @Test
    public void isVlanEncodable() {
        DiscreteResource resource = Resources.discrete(DID, PN, VLAN).resource();

        assertThat(sut.isEncodable(resource), is(true));
    }

    /**
     * Checks that it's possible to encode a MPLS label.
     */
    @Test
    public void isMplsEncodable() {
        DiscreteResource resource = Resources.discrete(DID, PN, MPLS).resource();

        assertThat(sut.isEncodable(resource), is(true));
    }

    /**
     * Checks that it's not possible to encode the root resource.
     */
    @Test
    public void isRootNonEncodable() {
        DiscreteResource resource = Resource.ROOT;

        assertThat(sut.isEncodable(resource), is(false));
    }

    /**
     * Checks that it's not possible to encode an Och signal.
     */
    @Test
    public void isOchNonEncodable() {
        DiscreteResource resource = Resources.discrete(DID, PN, OCH).resource();

        assertThat(sut.isEncodable(resource), is(false));
    }
}
