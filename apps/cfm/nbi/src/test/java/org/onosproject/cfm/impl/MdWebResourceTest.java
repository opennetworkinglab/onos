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
import org.onosproject.cfm.CfmCodecContext;
import org.onosproject.codec.CodecService;
import org.onosproject.incubator.net.l2monitoring.cfm.DefaultMaintenanceDomain;
import org.onosproject.incubator.net.l2monitoring.cfm.MaintenanceDomain;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdId;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdIdCharStr;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static junit.framework.TestCase.fail;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class MdWebResourceTest extends CfmResourceTest {
    private final CfmMdService mdService = createMock(CfmMdService.class);

    private static final MdId MDNAME1 = MdIdCharStr.asMdId("md-1");
    private static final MdId MDNAME2 = MdIdCharStr.asMdId("md-2");

    private List<MaintenanceDomain> mdList;

    @Before
    public void setUpTest() throws CfmConfigException {
        CfmCodecContext context = new CfmCodecContext();
        ServiceDirectory testDirectory = new TestServiceDirectory()
                .add(CfmMdService.class, mdService)
                .add(CodecService.class, context.codecManager());
        setServiceDirectory(testDirectory);

        mdList = new ArrayList<>();

        mdList.add(DefaultMaintenanceDomain.builder(MDNAME1)
                .mdLevel(MaintenanceDomain.MdLevel.LEVEL1).build());
        mdList.add(DefaultMaintenanceDomain.builder(MDNAME2)
                .mdLevel(MaintenanceDomain.MdLevel.LEVEL2).build());
    }

    @Test
    public void testGetMds() {
        expect(mdService.getAllMaintenanceDomain()).andReturn(mdList).anyTimes();
        replay(mdService);

        final WebTarget wt = target();
        final String response = wt.path("md").request().get(String.class);

        assertThat(response, is("{\"mds\":[[" +
                "{\"mdName\":\"md-1\",\"mdNameType\":\"CHARACTERSTRING\"," +
                "\"mdLevel\":\"LEVEL1\",\"maList\":[]}," +
                "{\"mdName\":\"md-2\",\"mdNameType\":\"CHARACTERSTRING\"," +
                "\"mdLevel\":\"LEVEL2\",\"maList\":[]}]]}"));
    }

    @Test
    public void testGetMdsEmpty() {
        expect(mdService.getAllMaintenanceDomain())
                            .andReturn(new ArrayList<>()).anyTimes();
        replay(mdService);

        final WebTarget wt = target();
        final String response = wt.path("md").request().get(String.class);

        assertThat(response, is("{\"mds\":[[]]}"));
    }

    @Test
    public void testGetMd() {
        expect(mdService.getMaintenanceDomain(MDNAME1))
                .andReturn(Optional.ofNullable(mdList.get(0))).anyTimes();
        replay(mdService);

        final WebTarget wt = target();
        final String response = wt.path("md/" + MDNAME1).request().get(String.class);

        assertThat(response, is("{\"md\":" +
                "{\"mdName\":\"md-1\",\"mdNameType\":\"CHARACTERSTRING\"," +
                    "\"mdLevel\":\"LEVEL1\",\"maList\":[]}}"));
    }

    @Test
    public void testGetMdEmpty() throws IOException {
        final MdId mdName3 = MdIdCharStr.asMdId("md-3");
        expect(mdService.getMaintenanceDomain(mdName3))
                .andReturn(Optional.empty()).anyTimes();
        replay(mdService);

        final WebTarget wt = target();
        try {
            final String response = wt.path("md/" + mdName3).request().get(String.class);
            fail("Expected InternalServerErrorException, as MD is unknown");
        } catch (InternalServerErrorException e) {
            ByteArrayInputStream is = (ByteArrayInputStream) e.getResponse().getEntity();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line = null;
            StringBuffer sb = new StringBuffer();
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            assertThat(sb.toString(), is("{ \"failure\":" +
                    "\"java.lang.IllegalArgumentException: MD md-3 not Found\" }"));
        }
    }

    @Test
    public void testDeleteMd() throws CfmConfigException {
        expect(mdService.deleteMaintenanceDomain(MDNAME1))
                .andReturn(true).anyTimes();
        replay(mdService);

        final WebTarget wt = target();
        final Response response = wt.path("md/" + MDNAME1).request().delete();

        assertEquals(200, response.getStatus());
    }

    @Test
    public void testDeleteMdNotPresent() throws CfmConfigException {
        expect(mdService.deleteMaintenanceDomain(MDNAME1))
                .andReturn(false).anyTimes();
        replay(mdService);

        final WebTarget wt = target();
        final Response response = wt.path("md/" + MDNAME1).request().delete();

        assertEquals(304, response.getStatus());
    }

    @Test
    public void testCreateMd() throws CfmConfigException {
        MaintenanceDomain md3 = DefaultMaintenanceDomain
                .builder(MdIdCharStr.asMdId("md-3"))
                .mdLevel(MaintenanceDomain.MdLevel.LEVEL3)
                .mdNumericId((short) 3)
                .build();

        expect(mdService.createMaintenanceDomain(mdList.get(1)))
                .andReturn(false).anyTimes();
        replay(mdService);

        ObjectMapper mapper = new ObjectMapper();
        CfmCodecContext context = new CfmCodecContext();
        ObjectNode node = mapper.createObjectNode();
        node.set("md", context.codec(MaintenanceDomain.class)
                    .encode(mdList.get(1), context));


        final WebTarget wt = target();
        final Response response = wt.path("md")
                .request().post(Entity.json(node.toString()));

        assertEquals(201, response.getStatus());
    }
}
