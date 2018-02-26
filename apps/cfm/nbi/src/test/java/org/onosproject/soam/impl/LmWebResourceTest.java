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
import org.onosproject.incubator.net.l2monitoring.soam.MilliPct;
import org.onosproject.incubator.net.l2monitoring.soam.SoamConfigException;
import org.onosproject.incubator.net.l2monitoring.soam.SoamId;
import org.onosproject.incubator.net.l2monitoring.soam.SoamService;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementCreate;
import org.onosproject.incubator.net.l2monitoring.soam.loss.DefaultLmEntry;
import org.onosproject.incubator.net.l2monitoring.soam.loss.LossMeasurementCreate;
import org.onosproject.incubator.net.l2monitoring.soam.loss.LossMeasurementEntry;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class LmWebResourceTest extends CfmResourceTest {
    private final CfmMepService mepService = createMock(CfmMepService.class);
    private final SoamService soamService = createMock(SoamService.class);

    private static final MdId MDNAME1 = MdIdCharStr.asMdId("md-1");
    private static final MaIdShort MANAME1 = MaIdCharStr.asMaId("ma-1-1");
    private static final MepId MEPID1 = MepId.valueOf((short) 1);
    private static final SoamId LMID1 = SoamId.valueOf(1);
    private static final SoamId LMID2 = SoamId.valueOf(2);

    private LossMeasurementEntry lm1;
    private LossMeasurementEntry lm2;

    private final Instant now = Instant.now();

    @Before
    public void setUpTest() throws CfmConfigException, SoamConfigException {
        CfmCodecContext context = new CfmCodecContext();
        ServiceDirectory testDirectory = new TestServiceDirectory()
                .add(CfmMepService.class, mepService)
                .add(SoamService.class, soamService)
                .add(CodecService.class, context.codecManager());
        setServiceDirectory(testDirectory);

        lm1 = DefaultLmEntry.builder(
                    DelayMeasurementCreate.Version.Y17312008,
                    MepId.valueOf((short) 10),
                    Mep.Priority.PRIO1,
                    LossMeasurementCreate.LmType.LMLMM,
                LMID1)
                .build();
        lm2 = DefaultLmEntry.builder(
                    DelayMeasurementCreate.Version.Y17312011,
                    MepId.valueOf((short) 10),
                    Mep.Priority.PRIO2,
                    LossMeasurementCreate.LmType.LMLMM,
                LMID2)
                .measuredAvailabilityBackwardStatus(LossMeasurementEntry.AvailabilityType.AVAILABLE)
                .measuredAvailabilityForwardStatus(LossMeasurementEntry.AvailabilityType.UNKNOWN)
                .measuredBackwardFlr(MilliPct.ofPercent(49.9f))
                .measuredForwardFlr(MilliPct.ofRatio(0.51f))
                .measuredBackwardLastTransitionTime(now)
                .measuredForwardLastTransitionTime(now)
                .build();

    }

    @Test
    public void testGetAllLmsForMep() throws CfmConfigException, SoamConfigException {
        List<LossMeasurementEntry> lmList = new ArrayList<>();
        lmList.add(lm1);
        lmList.add(lm2);

        expect(soamService.getAllLms(MDNAME1, MANAME1, MEPID1)).andReturn(lmList).anyTimes();
        replay(soamService);

        final WebTarget wt = target();
        final String response = wt.path("md/" + MDNAME1.mdName() + "/ma/" +
                MANAME1.maName() + "/mep/" + MEPID1.value() + "/lm")
                .request().get(String.class);

        assertThat(response, is("{\"lms\":[[" +
                "{" +
                    "\"lmId\":\"1\"," +
                    "\"lmCfgType\":\"LMLMM\"," +
                    "\"version\":\"Y17312008\"," +
                    "\"remoteMepId\":10," +
                    "\"priority\":\"PRIO1\"," +
                    "\"countersEnabled\":[]," +
                    "\"measurementHistories\":[]," +
                    "\"availabilityHistories\":[]" +
                "},{" +
                    "\"lmId\":\"2\"," +
                    "\"measuredForwardFlr\":51.0," +
                    "\"measuredBackwardFlr\":49.9," +
                    "\"measuredAvailabilityForwardStatus\":\"UNKNOWN\"," +
                    "\"measuredAvailabilityBackwardStatus\":\"AVAILABLE\"," +
                    "\"measuredForwardLastTransitionTime\":\"" + now + "\"," +
                    "\"measuredBackwardLastTransitionTime\":\"" + now + "\"," +
                    "\"lmCfgType\":\"LMLMM\"," +
                    "\"version\":\"Y17312011\"," +
                    "\"remoteMepId\":10," +
                    "\"priority\":\"PRIO2\"," +
                    "\"countersEnabled\":[]," +
                    "\"measurementHistories\":[]," +
                    "\"availabilityHistories\":[]" +
                "}]]}"));
    }

    @Test
    public void testGetAllLmsForMepEmpty() throws CfmConfigException, SoamConfigException {
        List<LossMeasurementEntry> lmList = new ArrayList<>();

        expect(soamService.getAllLms(MDNAME1, MANAME1, MEPID1)).andReturn(lmList).anyTimes();
        replay(soamService);

        final WebTarget wt = target();
        final String response = wt.path("md/" + MDNAME1.mdName() + "/ma/" +
                MANAME1.maName() + "/mep/" + MEPID1.value() + "/lm")
                .request().get(String.class);

        assertThat(response, is("{\"lms\":[[]]}"));
    }

    @Test
    public void testGetLm() throws CfmConfigException, SoamConfigException {

        expect(soamService.getLm(MDNAME1, MANAME1, MEPID1, LMID1)).andReturn(lm1).anyTimes();
        replay(soamService);

        final WebTarget wt = target();
        final String response = wt.path("md/" + MDNAME1.mdName() + "/ma/" +
                MANAME1.maName() + "/mep/" + MEPID1.value() + "/lm/" + LMID1.value())
                .request().get(String.class);

        assertThat(response, is("{\"lm\":" +
                "{" +
                "\"lmId\":\"1\"," +
                "\"lmCfgType\":\"LMLMM\"," +
                "\"version\":\"Y17312008\"," +
                "\"remoteMepId\":10," +
                "\"priority\":\"PRIO1\"," +
                "\"countersEnabled\":[]," +
                "\"measurementHistories\":[]," +
                "\"availabilityHistories\":[]" +
                "}}"));
    }

    @Test
    public void testGetLmEmpty() throws CfmConfigException, SoamConfigException, IOException {
        SoamId lmId3 = SoamId.valueOf(3);
        expect(soamService.getLm(MDNAME1, MANAME1, MEPID1, lmId3))
                .andReturn(null).anyTimes();
        replay(soamService);

        final WebTarget wt = target();
        try {
            final String response = wt.path("md/" + MDNAME1.mdName() + "/ma/" +
                    MANAME1.maName() + "/mep/" + MEPID1.value() + "/lm/" + lmId3.value())
                    .request().get(String.class);
        } catch (InternalServerErrorException e) {
            ByteArrayInputStream is = (ByteArrayInputStream) e.getResponse().getEntity();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line = null;
            StringBuffer sb = new StringBuffer();
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            assertEquals("{ \"failure\":\"LM md-1/ma-1-1/1/3 not found\" }",
                    sb.toString());
        }
    }

    @Test
    public void testAbortLm() {

        final WebTarget wt = target();
        final Response response = wt.path("md/" + MDNAME1.mdName() + "/ma/" +
                MANAME1.maName() + "/mep/" + MEPID1.value() + "/lm/" + LMID1.value())
                .request().delete();
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testCreateLm() throws CfmConfigException, SoamConfigException {
        MepEntry mep1 = DefaultMepEntry.builder(MEPID1, DeviceId.deviceId("netconf:1.2.3.4:830"),
                PortNumber.portNumber(1), Mep.MepDirection.UP_MEP, MDNAME1, MANAME1).buildEntry();

        expect(mepService.getMep(MDNAME1, MANAME1, MEPID1)).andReturn(mep1).anyTimes();
        replay(mepService);

        ObjectMapper mapper = new ObjectMapper();
        CfmCodecContext context = new CfmCodecContext();
        ObjectNode node = mapper.createObjectNode();
        node.set("lm", context.codec(LossMeasurementCreate.class).encode(lm1, context));

        final WebTarget wt = target();
        final Response response = wt.path("md/" + MDNAME1.mdName() + "/ma/" +
                MANAME1.maName() + "/mep/" + MEPID1.value() + "/lm")
                .request().post(Entity.json(node.toString()));
        assertEquals(201, response.getStatus());
    }

    @Test
    public void testClearLmHistory() {

        final WebTarget wt = target();
        final Response response = wt.path("md/" + MDNAME1.mdName() + "/ma/" +
                MANAME1.maName() + "/mep/" + MEPID1.value() + "/lm/" + LMID1.value() +
                "/clear-history")
                .request().put(Entity.json(""));

        assertEquals(200, response.getStatus());
    }
}
