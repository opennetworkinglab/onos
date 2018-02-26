/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.cfm.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.TestServiceDirectory;
import org.onlab.packet.VlanId;
import org.onosproject.cfm.CfmCodecContext;
import org.onosproject.codec.CodecService;
import org.onosproject.incubator.net.l2monitoring.cfm.DefaultMaintenanceAssociation;
import org.onosproject.incubator.net.l2monitoring.cfm.DefaultMaintenanceDomain;
import org.onosproject.incubator.net.l2monitoring.cfm.DefaultMep;
import org.onosproject.incubator.net.l2monitoring.cfm.DefaultMepEntry;
import org.onosproject.incubator.net.l2monitoring.cfm.DefaultMepLbCreate;
import org.onosproject.incubator.net.l2monitoring.cfm.DefaultMepLtCreate;
import org.onosproject.incubator.net.l2monitoring.cfm.MaintenanceAssociation;
import org.onosproject.incubator.net.l2monitoring.cfm.MaintenanceDomain;
import org.onosproject.incubator.net.l2monitoring.cfm.Mep;
import org.onosproject.incubator.net.l2monitoring.cfm.MepEntry;
import org.onosproject.incubator.net.l2monitoring.cfm.MepLbCreate;
import org.onosproject.incubator.net.l2monitoring.cfm.MepLtCreate;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdCharStr;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdShort;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdId;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdIdCharStr;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmConfigException;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmMdService;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmMepService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Optional;

import static junit.framework.TestCase.fail;
import static org.easymock.EasyMock.*;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class MepWebResourceTest extends CfmResourceTest {
    private final CfmMepService mepService = createMock(CfmMepService.class);
    private final CfmMdService mdService = createMock(CfmMdService.class);

    private static final MdId MDNAME1 = MdIdCharStr.asMdId("md-1");
    private static final MaIdShort MANAME1 = MaIdCharStr.asMaId("ma-1-1");
    private static final MepId MEPID1 = MepId.valueOf((short) 1);

    private MepEntry mepEntry1 = null;

    @Before
    public void setUpTest() throws CfmConfigException {
        CfmCodecContext context = new CfmCodecContext();
        ServiceDirectory testDirectory = new TestServiceDirectory()
                .add(CfmMepService.class, mepService)
                .add(CfmMdService.class, mdService)
                .add(CodecService.class, context.codecManager());
        setServiceDirectory(testDirectory);

        mepEntry1 = DefaultMepEntry.builder(
                    MEPID1,
                    DeviceId.deviceId("netconf:1.2.3.4:830"),
                    PortNumber.portNumber(1),
                    Mep.MepDirection.UP_MEP, MDNAME1, MANAME1)
                .buildEntry();

    }

    @Test
    public void testGetAllMepsForMaEmpty() throws CfmConfigException {

        expect(mepService.getAllMeps(MDNAME1, MANAME1)).andReturn(null).anyTimes();
        replay(mepService);

        final WebTarget wt = target();
        final String response = wt.path("md/" + MDNAME1.mdName() + "/ma/" +
                MANAME1.maName() + "/mep").request().get(String.class);
        assertThat(response, is("{\"meps\":[[]]}"));
    }

    @Test
    public void testGetAllMepsForMa1Mep() throws CfmConfigException {
        Collection<MepEntry> meps = new ArrayList<>();
        meps.add(mepEntry1);

        expect(mepService.getAllMeps(MDNAME1, MANAME1)).andReturn(meps).anyTimes();
        replay(mepService);

        final WebTarget wt = target();
        final String response = wt.path("md/" + MDNAME1.mdName() + "/ma/" +
                MANAME1.maName() + "/mep").request().get(String.class);

        assertThat(response, is("{\"meps\":" +
                "[[{" +
                "\"mepId\":" + MEPID1.value() + "," +
                "\"deviceId\":\"netconf:1.2.3.4:830\"," +
                "\"port\":1," +
                "\"direction\":\"UP_MEP\"," +
                "\"mdName\":\"" + MDNAME1.mdName() + "\"," +
                "\"maName\":\"" + MANAME1.maName() + "\"," +
                "\"administrative-state\":false," +
                "\"cci-enabled\":false," +
                "\"remoteMeps\":[]}]]}"));
    }

    @Test
    public void testGetMepValid() throws CfmConfigException {

        expect(mepService.getMep(MDNAME1, MANAME1, MepId.valueOf((short) 1)))
                .andReturn(mepEntry1).anyTimes();
        replay(mepService);

        final WebTarget wt = target();
        final String response = wt.path("md/" + MDNAME1.mdName() + "/ma/" +
                MANAME1.maName() + "/mep/" + MEPID1.value()).request().get(String.class);

        assertThat(response, is("{\"mep\":" +
                "{" +
                "\"mepId\":" + MEPID1.value() + "," +
                "\"deviceId\":\"netconf:1.2.3.4:830\"," +
                "\"port\":1," +
                "\"direction\":\"UP_MEP\"," +
                "\"mdName\":\"" + MDNAME1.mdName() + "\"," +
                "\"maName\":\"" + MANAME1.maName() + "\"," +
                "\"administrative-state\":false," +
                "\"cci-enabled\":false," +
                "\"remoteMeps\":[]}}"));
    }

    @Test
    public void testGetMepNotFound() throws CfmConfigException, IOException {

        expect(mepService.getMep(MDNAME1, MANAME1, MepId.valueOf((short) 2)))
                .andReturn(null).anyTimes();
        replay(mepService);

        final WebTarget wt = target();

        try {
            final String response = wt.path("md/" + MDNAME1.mdName() + "/ma/" +
                    MANAME1.maName() + "/mep/" + 2).request().get(String.class);
            fail("Expected exception to be thrown");
        } catch (InternalServerErrorException e) {
            ByteArrayInputStream is = (ByteArrayInputStream) e.getResponse().getEntity();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line = null;
            StringBuffer sb = new StringBuffer();
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            assertEquals("{ \"failure\":\"MEP md-1/ma-1-1/2 not found\" }", sb.toString());
        }
    }

    @Test
    public void testDeleteMepValid() throws CfmConfigException {

        expect(mepService.deleteMep(MDNAME1, MANAME1, MepId.valueOf((short) 1), Optional.empty()))
                .andReturn(true).anyTimes();
        replay(mepService);

        final WebTarget wt = target();
        final Response response = wt.path("md/" + MDNAME1.mdName() + "/ma/" +
                MANAME1.maName() + "/mep/" + MEPID1.value()).request().delete();

        assertEquals("Expecting 200", 200, response.getStatus());
    }

    @Test
    public void testDeleteMepNotFound() throws CfmConfigException {

        expect(mepService.deleteMep(MDNAME1, MANAME1, MepId.valueOf((short) 2), Optional.empty()))
                .andReturn(false).anyTimes();
        replay(mepService);

        final WebTarget wt = target();
        final Response response = wt.path("md/" + MDNAME1.mdName() + "/ma/" +
                MANAME1.maName() + "/mep/2").request().delete();

        assertEquals("Expecting 304", 304, response.getStatus());
    }

    @Test
    public void testCreateMep() throws CfmConfigException, IOException {
        MepId mepId2 = MepId.valueOf((short) 2);
        Mep mep2 = DefaultMep.builder(
                mepId2,
                DeviceId.deviceId("netconf:2.2.3.4:830"),
                PortNumber.portNumber(2),
                Mep.MepDirection.UP_MEP, MDNAME1, MANAME1).build();

        MaintenanceAssociation ma1 = DefaultMaintenanceAssociation
                .builder(MANAME1, MDNAME1.getNameLength()).build();

        expect(mdService.getMaintenanceAssociation(MDNAME1, MANAME1))
                                .andReturn(Optional.ofNullable(ma1)).anyTimes();
        replay(mdService);
        expect(mepService.createMep(MDNAME1, MANAME1, mep2))
                                .andReturn(true).anyTimes();
        replay(mepService);

        ObjectMapper mapper = new ObjectMapper();
        CfmCodecContext context = new CfmCodecContext();
        ObjectNode node = mapper.createObjectNode();
        node.set("mep", context.codec(Mep.class).encode(mep2, context));

        final WebTarget wt = target();
        final Response response = wt.path("md/" + MDNAME1.mdName() + "/ma/" +
                MANAME1.maName() + "/mep")
                .request()
                .post(Entity.json(node.toString()));

        assertEquals("Expecting 201", 201, response.getStatus());
    }

    @Test
    public void testCreateMepAlreadyExists() throws CfmConfigException, IOException {
        MepId mepId3 = MepId.valueOf((short) 3);
        Mep mep3 = DefaultMep.builder(
                    mepId3,
                    DeviceId.deviceId("netconf:3.2.3.4:830"),
                    PortNumber.portNumber(3),
                    Mep.MepDirection.UP_MEP, MDNAME1, MANAME1)
                .cciEnabled(true)
                .ccmLtmPriority(Mep.Priority.PRIO3)
                .administrativeState(false)
                .primaryVid(VlanId.vlanId((short) 3))
                .defectAbsentTime(Duration.ofMinutes(2))
                .defectPresentTime(Duration.ofMinutes(3))
                .fngAddress(Mep.FngAddress.notSpecified())
                .lowestFaultPriorityDefect(Mep.LowestFaultDefect.ALL_DEFECTS)
                .build();

        MaintenanceAssociation ma1 = DefaultMaintenanceAssociation
                .builder(MANAME1, MDNAME1.getNameLength()).build();

        expect(mdService.getMaintenanceAssociation(MDNAME1, MANAME1))
                .andReturn(Optional.ofNullable(ma1)).anyTimes();
        replay(mdService);
        expect(mepService.createMep(MDNAME1, MANAME1, mep3))
                .andReturn(false).anyTimes();
        replay(mepService);

        ObjectMapper mapper = new ObjectMapper();
        CfmCodecContext context = new CfmCodecContext();
        ObjectNode node = mapper.createObjectNode();
        node.set("mep", context.codec(Mep.class).encode(mep3, context));

        final WebTarget wt = target();
        final Response response = wt.path("md/" + MDNAME1.mdName() + "/ma/" +
                MANAME1.maName() + "/mep")
                .request()
                .post(Entity.json(node.toString()));

        assertEquals("Expecting 304", 304, response.getStatus());
    }

    @Test
    public void testTransmitLoopback() throws CfmConfigException {
        MepLbCreate mepLbCreate1 = DefaultMepLbCreate
                .builder(MEPID1)
                .numberMessages(20)
                .dataTlvHex("AA:BB:CC:DD")
                .vlanDropEligible(true)
                .build();

        MaintenanceAssociation ma1 = DefaultMaintenanceAssociation
                .builder(MANAME1, MDNAME1.getNameLength()).build();
        MaintenanceDomain md1 = DefaultMaintenanceDomain.builder(MDNAME1)
                .addToMaList(ma1).build();
        expect(mdService.getMaintenanceDomain(MDNAME1))
                        .andReturn(Optional.ofNullable(md1)).anyTimes();
        expect(mdService.getMaintenanceAssociation(MDNAME1, MANAME1))
                        .andReturn(Optional.ofNullable(ma1)).anyTimes();
        replay(mdService);

        ObjectMapper mapper = new ObjectMapper();
        CfmCodecContext context = new CfmCodecContext();
        ObjectNode node = mapper.createObjectNode();
        node.set("loopback", context.codec(MepLbCreate.class).encode(mepLbCreate1, context));

        final WebTarget wt = target();
        final Response response = wt.path("md/" + MDNAME1.mdName() + "/ma/" +
                MANAME1.maName() + "/mep/" + MEPID1.value() + "/transmit-loopback")
                .request()
                .put(Entity.json(node.toString()));

        assertEquals("Expecting 202", 202, response.getStatus());
    }

    @Test
    public void testAbortLoopback() throws CfmConfigException {

        MepLbCreate mepLbCreate1 = DefaultMepLbCreate.builder(MEPID1).build();

        MaintenanceAssociation ma1 = DefaultMaintenanceAssociation
                .builder(MANAME1, MDNAME1.getNameLength()).build();
        MaintenanceDomain md1 = DefaultMaintenanceDomain.builder(MDNAME1)
                .addToMaList(ma1).build();
        expect(mdService.getMaintenanceDomain(MDNAME1))
                .andReturn(Optional.ofNullable(md1)).anyTimes();
        expect(mdService.getMaintenanceAssociation(MDNAME1, MANAME1))
                .andReturn(Optional.ofNullable(ma1)).anyTimes();
        replay(mdService);

        final WebTarget wt = target();
        final Response response = wt.path("md/" + MDNAME1.mdName() + "/ma/" +
                MANAME1.maName() + "/mep/" + MEPID1.value() + "/abort-loopback")
                .request()
                .put(Entity.json(""));

        assertEquals("Expecting 202", 202, response.getStatus());

    }

    @Test
    public void testTransmitLinktrace() throws CfmConfigException {
        MepLtCreate mepLtCreate1 = DefaultMepLtCreate
                .builder(MEPID1)
                .defaultTtl((short) 20)
                .transmitLtmFlags(BitSet.valueOf(new byte[]{1}))
                .build();

        MaintenanceAssociation ma1 = DefaultMaintenanceAssociation
                .builder(MANAME1, MDNAME1.getNameLength()).build();
        MaintenanceDomain md1 = DefaultMaintenanceDomain.builder(MDNAME1)
                .addToMaList(ma1).build();
        expect(mdService.getMaintenanceDomain(MDNAME1))
                .andReturn(Optional.ofNullable(md1)).anyTimes();
        expect(mdService.getMaintenanceAssociation(MDNAME1, MANAME1))
                .andReturn(Optional.ofNullable(ma1)).anyTimes();
        replay(mdService);

        ObjectMapper mapper = new ObjectMapper();
        CfmCodecContext context = new CfmCodecContext();
        ObjectNode node = mapper.createObjectNode();
        node.set("linktrace", context.codec(MepLtCreate.class).encode(mepLtCreate1, context));

        final WebTarget wt = target();
        final Response response = wt.path("md/" + MDNAME1.mdName() + "/ma/" +
                MANAME1.maName() + "/mep/" + MEPID1.value() + "/transmit-linktrace")
                .request()
                .put(Entity.json(node.toString()));

        assertEquals("Expecting 202", 202, response.getStatus());
    }
}
