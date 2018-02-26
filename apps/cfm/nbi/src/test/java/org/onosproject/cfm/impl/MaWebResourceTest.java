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
import org.onosproject.incubator.net.l2monitoring.cfm.Component;
import org.onosproject.incubator.net.l2monitoring.cfm.DefaultComponent;
import org.onosproject.incubator.net.l2monitoring.cfm.DefaultMaintenanceAssociation;
import org.onosproject.incubator.net.l2monitoring.cfm.DefaultMaintenanceDomain;
import org.onosproject.incubator.net.l2monitoring.cfm.MaintenanceAssociation;
import org.onosproject.incubator.net.l2monitoring.cfm.MaintenanceDomain;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdCharStr;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdShort;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdId;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdIdCharStr;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmConfigException;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmMdService;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Optional;

import static junit.framework.TestCase.fail;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class MaWebResourceTest extends CfmResourceTest {
    private final CfmMdService mdService = createMock(CfmMdService.class);

    private static final MdId MDNAME1 = MdIdCharStr.asMdId("md-1");
    private static final MaIdShort MANAME1 = MaIdCharStr.asMaId("ma-1-1");

    private MaintenanceAssociation ma1;

    @Before
    public void setUpTest() throws CfmConfigException {
        CfmCodecContext context = new CfmCodecContext();
        ServiceDirectory testDirectory = new TestServiceDirectory()
                .add(CfmMdService.class, mdService)
                .add(CodecService.class, context.codecManager());
        setServiceDirectory(testDirectory);

        ma1 = DefaultMaintenanceAssociation
                .builder(MANAME1, MDNAME1.getNameLength())
                .addToRemoteMepIdList(MepId.valueOf((short) 101))
                .addToRemoteMepIdList(MepId.valueOf((short) 102))
                .ccmInterval(MaintenanceAssociation.CcmInterval.INTERVAL_3MS)
                .maNumericId((short) 1)
                .addToComponentList(
                        DefaultComponent.builder(1)
                                .tagType(Component.TagType.VLAN_STAG)
                                .mhfCreationType(Component.MhfCreationType.NONE)
                                .idPermission(Component.IdPermissionType.MANAGE)
                                .addToVidList(VlanId.vlanId((short) 1010))
                                .build())
                .build();
    }

    @Test
    public void testGetMa() {

        expect(mdService.getMaintenanceAssociation(MDNAME1, MANAME1))
                .andReturn(Optional.ofNullable(ma1)).anyTimes();
        replay(mdService);

        final WebTarget wt = target();
        final String response = wt.path("md/" + MDNAME1.mdName()
                + "/ma/" + MANAME1.maName()).request().get(String.class);

        assertThat(response, is("{\"ma\":" +
                "{\"maName\":\"ma-1-1\"," +
                "\"maNameType\":\"CHARACTERSTRING\"," +
                "\"maNumericId\":1," +
                "\"ccm-interval\":\"INTERVAL_3MS\"," +
                "\"component-list\":[{\"component\":" +
                    "{\"component-id\":1," +
                    "\"vid-list\":[{\"vid\":\"1010\"}]," +
                    "\"mhf-creation-type\":\"NONE\"," +
                    "\"id-permission\":\"MANAGE\"," +
                    "\"tag-type\":\"VLAN_STAG\"}}]," +
                "\"rmep-list\":" +
                    "[{\"rmep\":101}," +
                    "{\"rmep\":102}]}}"));
    }

    @Test
    public void testGetMaEmpty() throws IOException {
        MaIdShort maId2 = MaIdCharStr.asMaId("ma-2");
        expect(mdService
                .getMaintenanceAssociation(MDNAME1, maId2))
                .andReturn(Optional.empty()).anyTimes();
        replay(mdService);

        final WebTarget wt = target();
        try {
            final String response = wt.path("md/" + MDNAME1.mdName()
                    + "/ma/" + maId2.maName()).request().get(String.class);
            fail("Expected InternalServerErrorException, as MA is unknown");
        } catch (InternalServerErrorException e) {
            ByteArrayInputStream is = (ByteArrayInputStream) e.getResponse().getEntity();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line = null;
            StringBuffer sb = new StringBuffer();
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            assertThat(sb.toString(), is("{ \"failure\":" +
                    "\"java.lang.IllegalArgumentException: MA ma-2 not Found\" }"));
        }
    }

    @Test
    public void testDeleteMa() throws CfmConfigException {

        expect(mdService.deleteMaintenanceAssociation(MDNAME1, MANAME1))
                .andReturn(true).anyTimes();
        replay(mdService);

        final WebTarget wt = target();
        final Response response = wt.path("md/" + MDNAME1.mdName()
                + "/ma/" + MANAME1.maName()).request().delete();

        assertEquals(200, response.getStatus());
    }

    @Test
    public void testDeleteMaEmpty() throws CfmConfigException {
        MaIdShort maId2 = MaIdCharStr.asMaId("ma-2");

        expect(mdService.deleteMaintenanceAssociation(MDNAME1, maId2))
                .andReturn(false).anyTimes();
        replay(mdService);

        final WebTarget wt = target();
        final Response response = wt.path("md/" + MDNAME1.mdName()
                + "/ma/" + maId2.maName()).request().delete();

        assertEquals(304, response.getStatus());
    }

    @Test
    public void testCreateMa() throws CfmConfigException {
        MaintenanceDomain md1 = DefaultMaintenanceDomain
                .builder(MDNAME1).mdLevel(MaintenanceDomain.MdLevel.LEVEL2).build();

        expect(mdService.getMaintenanceDomain(MDNAME1))
                .andReturn(Optional.ofNullable(md1)).anyTimes();
        expect(mdService.createMaintenanceAssociation(MDNAME1, ma1))
                .andReturn(false).anyTimes();
        replay(mdService);

        ObjectMapper mapper = new ObjectMapper();
        CfmCodecContext context = new CfmCodecContext();
        ObjectNode node = mapper.createObjectNode();
        node.set("ma", context.codec(MaintenanceAssociation.class)
                .encode(ma1, context));

        final WebTarget wt = target();
        final Response response = wt.path("md/" + MDNAME1.mdName()
                + "/ma").request().post(Entity.json(node.toString()));

        assertEquals(201, response.getStatus());
    }
}
