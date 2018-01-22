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
package org.onosproject.soam.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.TestServiceDirectory;
import org.onosproject.cfm.CfmCodecContext;
import org.onosproject.cfm.impl.CfmResourceTest;
import org.onosproject.codec.CodecService;
import org.onosproject.incubator.net.l2monitoring.cfm.DefaultMepEntry;
import org.onosproject.incubator.net.l2monitoring.cfm.Mep;
import org.onosproject.incubator.net.l2monitoring.cfm.MepEntry;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdCharStr;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdShort;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdId;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdIdCharStr;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmConfigException;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmMepService;
import org.onosproject.incubator.net.l2monitoring.soam.SoamConfigException;
import org.onosproject.incubator.net.l2monitoring.soam.SoamId;
import org.onosproject.incubator.net.l2monitoring.soam.SoamService;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DefaultDelayMeasurementEntry;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DefaultDelayMeasurementStatCurrent;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementCreate;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementEntry;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementStatCurrent;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.fail;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class DmWebResourceTest extends CfmResourceTest {
    private final CfmMepService mepService = createMock(CfmMepService.class);
    private final SoamService soamService = createMock(SoamService.class);

    private static final MdId MDNAME1 = MdIdCharStr.asMdId("md-1");
    private static final MaIdShort MANAME1 = MaIdCharStr.asMaId("ma-1-1");
    private static final MepId MEPID1 = MepId.valueOf((short) 1);
    private static final SoamId DM1 = SoamId.valueOf(1);
    private static final SoamId DM2 = SoamId.valueOf(2);

    private DelayMeasurementEntry dm1;
    private DelayMeasurementEntry dm2;

    private final Instant now = Instant.now();

    @Before
    public void setUpTest() throws CfmConfigException, SoamConfigException {
        CfmCodecContext context = new CfmCodecContext();
        ServiceDirectory testDirectory = new TestServiceDirectory()
                .add(CfmMepService.class, mepService)
                .add(SoamService.class, soamService)
                .add(CodecService.class, context.codecManager());
        setServiceDirectory(testDirectory);

        DelayMeasurementStatCurrent.DmStatCurrentBuilder dmCurrBuilder1 =
                (DelayMeasurementStatCurrent.DmStatCurrentBuilder)
                        DefaultDelayMeasurementStatCurrent
                        .builder(Duration.ofMinutes(1), false)
                        .startTime(now)
                        .frameDelayBackwardAvg(Duration.ofMillis(10))
                        .frameDelayForwardAvg(Duration.ofMillis(11))
                        .frameDelayRangeBackwardAvg(Duration.ofMillis(12));

        dm1 = DefaultDelayMeasurementEntry.builder(DM1,
                    DelayMeasurementCreate.DmType.DMDMM,
                    DelayMeasurementCreate.Version.Y17312008,
                    MepId.valueOf((short) 2),
                    Mep.Priority.PRIO1)
                .sessionStatus(DelayMeasurementEntry.SessionStatus.ACTIVE)
                .frameDelayTwoWay(Duration.ofMillis(40))
                .frameDelayBackward(Duration.ofMillis(30))
                .frameDelayForward(Duration.ofMillis(10))
                .interFrameDelayVariationTwoWay(Duration.ofMillis(8))
                .interFrameDelayVariationBackward(Duration.ofMillis(3))
                .interFrameDelayVariationForward(Duration.ofMillis(5))
                .currentResult((DelayMeasurementStatCurrent) dmCurrBuilder1.build())
                .build();

        dm2 = DefaultDelayMeasurementEntry.builder(DM2,
                    DelayMeasurementCreate.DmType.DMDMM,
                    DelayMeasurementCreate.Version.Y17312011,
                    MepId.valueOf((short) 2),
                    Mep.Priority.PRIO2)
                .build();
    }

    @Test
    public void testGetAllDmsForMep() throws CfmConfigException, SoamConfigException {

        List<DelayMeasurementEntry> dmList = new ArrayList<>();
        dmList.add(dm1);
        dmList.add(dm2);

        expect(soamService.getAllDms(MDNAME1, MANAME1, MEPID1)).andReturn(dmList).anyTimes();
        replay(soamService);

        final WebTarget wt = target();
        final String response = wt.path("md/" + MDNAME1.mdName() + "/ma/" +
                MANAME1.maName() + "/mep/" + MEPID1.value() + "/dm")
                .request().get(String.class);

        assertThat(response, is("{\"dms\":[[" +
                "{" +
                    "\"dmId\":\"1\"," +
                    "\"sessionStatus\":\"ACTIVE\"," +
                    "\"frameDelayTwoWay\":\"PT0.04S\"," +
                    "\"frameDelayForward\":\"PT0.01S\"," +
                    "\"frameDelayBackward\":\"PT0.03S\"," +
                    "\"interFrameDelayVariationTwoWay\":\"PT0.008S\"," +
                    "\"interFrameDelayVariationForward\":\"PT0.005S\"," +
                    "\"interFrameDelayVariationBackward\":\"PT0.003S\"," +
                    "\"dmCfgType\":\"DMDMM\"," +
                    "\"version\":\"Y17312008\"," +
                    "\"remoteMepId\":2," +
                    "\"priority\":\"PRIO1\"," +
                    "\"measurementsEnabled\":[]," +
                    "\"current\":{" +
                        "\"startTime\":\"" + now + "\"," +
                        "\"elapsedTime\":\"PT1M\"," +
                        "\"suspectStatus\":\"false\"," +
                        "\"frameDelayForwardAvg\":\"PT0.011S\"," +
                        "\"frameDelayBackwardAvg\":\"PT0.01S\"," +
                        "\"frameDelayRangeBackwardAvg\":\"PT0.012S\"" +
                    "}," +
                    "\"historic\":[]" +
                "},{" +
                    "\"dmId\":\"2\"," +
                    "\"dmCfgType\":\"DMDMM\"," +
                    "\"version\":\"Y17312011\"," +
                    "\"remoteMepId\":2," +
                    "\"priority\":\"PRIO2\"," +
                    "\"measurementsEnabled\":[]," +
                    "\"historic\":[]}]]" +
                "}"));
    }

    @Test
    public void testGetAllDmsForMepEmpty() throws CfmConfigException, SoamConfigException {

        List<DelayMeasurementEntry> dmListEmpty = new ArrayList<>();

        expect(soamService.getAllDms(MDNAME1, MANAME1, MEPID1)).andReturn(dmListEmpty).anyTimes();
        replay(soamService);

        final WebTarget wt = target();
        final String response = wt.path("md/" + MDNAME1.mdName() + "/ma/" +
                MANAME1.maName() + "/mep/" + MEPID1.value() + "/dm")
                .request().get(String.class);

        assertThat(response, is("{\"dms\":[[]]}"));
    }

    @Test
    public void testGetDm() throws CfmConfigException, SoamConfigException {

        expect(soamService.getDm(MDNAME1, MANAME1, MEPID1, DM1)).andReturn(dm1).anyTimes();
        replay(soamService);

        final WebTarget wt = target();
        final String response = wt.path("md/" + MDNAME1.mdName() + "/ma/" +
                MANAME1.maName() + "/mep/" + MEPID1.value() + "/dm/" + DM1.value())
                .request().get(String.class);

        assertThat(response, is("{\"dm\":" +
                "{" +
                    "\"dmId\":\"1\"," +
                    "\"sessionStatus\":\"ACTIVE\"," +
                    "\"frameDelayTwoWay\":\"PT0.04S\"," +
                    "\"frameDelayForward\":\"PT0.01S\"," +
                    "\"frameDelayBackward\":\"PT0.03S\"," +
                    "\"interFrameDelayVariationTwoWay\":\"PT0.008S\"," +
                    "\"interFrameDelayVariationForward\":\"PT0.005S\"," +
                    "\"interFrameDelayVariationBackward\":\"PT0.003S\"," +
                    "\"dmCfgType\":\"DMDMM\"," +
                    "\"version\":\"Y17312008\"," +
                    "\"remoteMepId\":2," +
                    "\"priority\":\"PRIO1\"," +
                    "\"measurementsEnabled\":[]," +
                    "\"current\":{" +
                        "\"startTime\":\"" + now + "\"," +
                        "\"elapsedTime\":\"PT1M\"," +
                        "\"suspectStatus\":\"false\"," +
                        "\"frameDelayForwardAvg\":\"PT0.011S\"," +
                        "\"frameDelayBackwardAvg\":\"PT0.01S\"," +
                        "\"frameDelayRangeBackwardAvg\":\"PT0.012S\"" +
                    "}," +
                    "\"historic\":[]" +
                "}}"));
    }

    @Test
    public void testGetDmInvalid() throws CfmConfigException, SoamConfigException, IOException {

        SoamId dm3 = SoamId.valueOf(3);

        expect(soamService.getDm(MDNAME1, MANAME1, MEPID1, dm3)).andReturn(null).anyTimes();
        replay(soamService);

        final WebTarget wt = target();
        try {
            final String response = wt.path("md/" + MDNAME1.mdName() + "/ma/" +
                    MANAME1.maName() + "/mep/" + MEPID1.value() + "/dm/" + dm3.value())
                    .request().get(String.class);
            fail("Expecting excpetion");
        } catch (InternalServerErrorException e) {
            ByteArrayInputStream is = (ByteArrayInputStream) e.getResponse().getEntity();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line = null;
            StringBuffer sb = new StringBuffer();
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            assertEquals("{ \"failure\":\"DM md-1/ma-1-1/1/3 not found\" }", sb.toString());
        }
    }

    @Test
    public void testAbortDm() {

        final WebTarget wt = target();
        final Response response = wt.path("md/" + MDNAME1.mdName() + "/ma/" +
                MANAME1.maName() + "/mep/" + MEPID1.value() + "/dm/" + DM1.value())
                .request().delete();

        assertEquals(200, response.getStatus());
    }

    @Test
    public void testCreateDm() throws CfmConfigException, SoamConfigException {
        MepEntry mep1 = DefaultMepEntry.builder(MEPID1, DeviceId.deviceId("netconf:1.2.3.4:830"),
                PortNumber.portNumber(1), Mep.MepDirection.UP_MEP, MDNAME1, MANAME1).buildEntry();

        expect(mepService.getMep(MDNAME1, MANAME1, MEPID1)).andReturn(mep1).anyTimes();
        replay(mepService);

        ObjectMapper mapper = new ObjectMapper();
        CfmCodecContext context = new CfmCodecContext();
        ObjectNode node = mapper.createObjectNode();
        node.set("dm", context.codec(DelayMeasurementCreate.class).encode(dm1, context));

        final WebTarget wt = target();
        final Response response = wt.path("md/" + MDNAME1.mdName() + "/ma/" +
                MANAME1.maName() + "/mep/" + MEPID1.value() + "/dm")
                .request().post(Entity.json(node.toString()));

        assertEquals(201, response.getStatus());
    }

    @Test
    public void testClearDmHistory() {

        final WebTarget wt = target();
        final Response response = wt.path("md/" + MDNAME1.mdName() + "/ma/" +
                MANAME1.maName() + "/mep/" + MEPID1.value() + "/dm/" + DM1.value() +
                "/clear-history")
                .request().put(Entity.json(""));

        assertEquals(200, response.getStatus());
    }

}
