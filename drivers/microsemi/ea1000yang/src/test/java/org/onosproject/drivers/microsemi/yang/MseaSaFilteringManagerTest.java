/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.drivers.microsemi.yang;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.UncheckedIOException;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ip4Address;
import org.onosproject.drivers.microsemi.yang.impl.MseaSaFilteringManager;
import org.onosproject.netconf.NetconfDeviceInfo;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.onosproject.netconf.TargetConfig;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.sa.filtering.rev20160412.MseaSaFiltering;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.sa.filtering.rev20160412.MseaSaFilteringOpParam;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.sa.filtering.rev20160412.mseasafiltering.DefaultSourceIpaddressFiltering;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.sa.filtering.rev20160412.mseasafiltering.SourceIpaddressFiltering;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.sa.filtering.rev20160412.mseasafiltering.sourceipaddressfiltering.DefaultInterfaceEth0;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.sa.filtering.rev20160412.mseasafiltering.sourceipaddressfiltering.InterfaceEth0;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.sa.filtering.rev20160412.mseasafiltering.sourceipaddressfiltering.interfaceeth0.DefaultSourceAddressRange;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.sa.filtering.rev20160412.mseasafiltering.sourceipaddressfiltering.interfaceeth0.SourceAddressRange;
import org.onosproject.yms.ymsm.YmsService;

public class MseaSaFilteringManagerTest {

    MseaSaFilteringManager mseaSaSvc;
    YmsService ymsService;
    NetconfSession session;

    @Before
    public void setUp() throws Exception {
        try {
            mseaSaSvc = new MockMseaSaFilteringManager();
            mseaSaSvc.activate();
        } catch (UncheckedIOException e) {
            fail(e.getMessage());
        }
        NetconfDeviceInfo deviceInfo = new NetconfDeviceInfo("netconf", "netconf", Ip4Address.valueOf("1.2.3.4"), 830);
        session = new MockNetconfSessionEa1000(deviceInfo);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testGetMseaSaFilteringMseaSaFilteringOpParamNetconfSession() throws NetconfException {
        SourceIpaddressFiltering.SourceIpaddressFilteringBuilder sipBuilder =
                new DefaultSourceIpaddressFiltering.SourceIpaddressFilteringBuilder();

        MseaSaFilteringOpParam.MseaSaFilteringBuilder opBuilder =
                new MseaSaFilteringOpParam.MseaSaFilteringBuilder();
        MseaSaFilteringOpParam mseaSaFilteringFilter =
                (MseaSaFilteringOpParam) opBuilder
                .sourceIpaddressFiltering(sipBuilder.build())
                .build();

        MseaSaFiltering result = mseaSaSvc.getMseaSaFiltering(mseaSaFilteringFilter, session);

        //Results come from MockNetconfSession SAMPLE_MSEASAFILTERING_REPLY_INIT
        assertNotNull(result.sourceIpaddressFiltering().interfaceEth0().sourceAddressRange());
        List<SourceAddressRange> ranges = result.sourceIpaddressFiltering().interfaceEth0().sourceAddressRange();
        assertEquals(2, ranges.size());

        for (SourceAddressRange sa:ranges) {
            if (sa.rangeId() == 1) {
                assertEquals("10.10.10.10/16", sa.ipv4AddressPrefix());

            } else if (sa.rangeId() == 2) {
                assertEquals("20.30.40.50/18", sa.ipv4AddressPrefix());
            }
        }
    }

    @Test
    public void testSetMseaSaFilteringMseaSaFilteringOpParamNetconfSessionNcDsType() {

        MseaSaFilteringOpParam mseaSaFilteringConfig = createConfigForEdit("192.168.60.10/27", (short) 3, "Filter3");

        //Calling on the edit-config just makes the change and hopefully does not throw a Netconf Exception
        try {
            mseaSaSvc.setMseaSaFiltering(mseaSaFilteringConfig, session, TargetConfig.RUNNING);
        } catch (NetconfException e) {
            e.printStackTrace();
            fail("NETCONF Exception: " + e.getMessage());
        }
    }

    /**
     * This is also called from the test case EA1000FlowRuleProgrammableTest().
     * In the ea1000driver project
     * @return
     */
    public static MseaSaFilteringOpParam createConfigForEdit(String ipAddrPrefix, short rangeId, String rangeName) {
        SourceAddressRange.SourceAddressRangeBuilder sarBuilder =
                new DefaultSourceAddressRange.SourceAddressRangeBuilder();
        SourceAddressRange saRange =
                sarBuilder
                .ipv4AddressPrefix(ipAddrPrefix)
                .rangeId(rangeId)
                .name(rangeName)
                .build();

        InterfaceEth0.InterfaceEth0Builder eth0Builder =
                new DefaultInterfaceEth0.InterfaceEth0Builder();
        InterfaceEth0 eth0 = eth0Builder.addToSourceAddressRange(saRange).build();

        SourceIpaddressFiltering.SourceIpaddressFilteringBuilder sipBuilder =
                new DefaultSourceIpaddressFiltering.SourceIpaddressFilteringBuilder();

        SourceIpaddressFiltering sip = sipBuilder.interfaceEth0(eth0).build();

        MseaSaFilteringOpParam.MseaSaFilteringBuilder opBuilder =
                new MseaSaFilteringOpParam.MseaSaFilteringBuilder();
        MseaSaFilteringOpParam mseaSaFilteringConfig =
                (MseaSaFilteringOpParam) opBuilder
                .sourceIpaddressFiltering(sipBuilder.build())
                .build();

        return mseaSaFilteringConfig;
    }
}
