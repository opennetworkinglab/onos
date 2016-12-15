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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.UncheckedIOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ip4Address;
import org.onosproject.drivers.microsemi.yang.impl.MseaCfmManager;
import org.onosproject.netconf.NetconfDeviceInfo;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.cfm.rev20160229.MseaCfm;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.cfm.rev20160229.mseacfm.DefaultMefCfm;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.cfm.rev20160229.mseacfm.MefCfm;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.cfm.rev20160229.mseacfm.mefcfm.DefaultMaintenanceDomain;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.cfm.rev20160229.mseacfm.mefcfm.MaintenanceDomain;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.cfm.rev20160229.mseacfm.mefcfm.maintenancedomain.mdnameandtypecombo.DefaultNameCharacterString;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.cfm.rev20160229.mseacfm.mefcfm.maintenancedomain.mdnameandtypecombo.NameCharacterString;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.types.rev20160229.mseatypes.Identifier45;
import org.onosproject.yms.ymsm.YmsService;

public class MseaCfmManagerTest {

    MseaCfmManager mseaCfmService;
    YmsService ymsService;
    NetconfSession session;

    @Before
    public void setUp() throws Exception {
        try {
            mseaCfmService = new MockMseaCfmManager();
            mseaCfmService.activate();
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
    public void testGetConfigMseaCfmEssentials() throws NetconfException {
        MseaCfm mseaCfm = mseaCfmService.getMepEssentials("md-1", "ma-1-1", 1, session);
        assertNotNull(mseaCfm);

        //See SAMPLE_MSEACFM_MD_MA_MEP_REPLY in MockNetconfSessionEa1000
        assertEquals(1, mseaCfm.mefCfm().maintenanceDomain().size());
        assertEquals(2, mseaCfm.mefCfm().maintenanceDomain().get(0).mdLevel().uint8());
    }

    /**
     * Create the Maintenance Domain "md-1".
     * @throws NetconfException
     */
    @Test
    public void testSetMseaCfm() throws NetconfException {
        NameCharacterString mdName = DefaultNameCharacterString.builder().name(Identifier45.fromString("md-1")).build();

        MaintenanceDomain yangMd = DefaultMaintenanceDomain.builder()
                .id((short) 1)
                .mdNameAndTypeCombo(mdName)
                .build();

        MefCfm mefCfm = DefaultMefCfm.builder().addToMaintenanceDomain(yangMd).build();
      //FIXME implement this
//        MseaCfmOpParam mseaCfmOpParam = (MseaCfmOpParam) MseaCfmOpParam.builder().mefCfm(mefCfm).build();
//        mseaCfmService.setMseaCfm(mseaCfmOpParam, session, NcDsType.running);
    }

    @Test
    public void testTransmitLoopback() throws NetconfException {
        try {
            mseaCfmService.transmitLoopback(null, session);
        } catch (UnsupportedOperationException e) {
            assertTrue(e.getMessage().contains("Not yet implemented"));
        }
    }

    @Test
    public void testAbortLoopback() throws NetconfException {
        try {
            mseaCfmService.abortLoopback(null, session);
        } catch (UnsupportedOperationException e) {
            assertTrue(e.getMessage().contains("Not yet implemented"));
        }
    }

    @Test
    public void testTransmitLinktrace() throws NetconfException {
        try {
            mseaCfmService.transmitLinktrace(null, session);
        } catch (UnsupportedOperationException e) {
            assertTrue(e.getMessage().contains("Not yet implemented"));
        }
    }

}
