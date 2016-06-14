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
package org.onosproject.ospf.controller.impl;


import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ip4Address;
import org.onosproject.ospf.controller.OspfArea;
import org.onosproject.ospf.controller.OspfInterface;
import org.onosproject.ospf.controller.OspfLinkListener;
import org.onosproject.ospf.controller.OspfProcess;
import org.onosproject.ospf.controller.OspfRouter;
import org.onosproject.ospf.controller.OspfRouterListener;
import org.onosproject.ospf.controller.area.OspfAreaImpl;
import org.onosproject.ospf.controller.area.OspfInterfaceImpl;
import org.onosproject.ospf.controller.area.OspfProcessImpl;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit test class for OspfRouterId.
 */
public class OspfControllerImplTest {

    private OspfControllerImpl ospfController;
    private OspfRouterListener ospfRouterListener;
    private OspfLinkListener ospfLinkListener;
    private List<OspfProcess> ospfProcesses;
    private OspfProcess process1;
    private List<OspfArea> areas;
    private OspfAreaImpl ospfArea;
    private List<OspfInterface> ospfInterfaces;
    private OspfInterfaceImpl ospfInterface;
    private OspfProcess ospfProcess;
    private OspfArea ospfArea1;
    private OspfRouter ospfRouter;

    @Before
    public void setUp() throws Exception {
        ospfController = new OspfControllerImpl();
    }

    @After
    public void tearDown() throws Exception {
        ospfController = null;
        ospfRouterListener = null;
        ospfLinkListener = null;
        ospfProcesses = null;
        areas = null;
        ospfArea = null;
        ospfInterfaces = null;
        ospfInterface = null;
        ospfProcess = null;
        ospfProcess = null;
        ospfArea1 = null;
        ospfRouter = null;
    }

    /**
     * Tests activate() method.
     */
    @Test
    public void testActivate() throws Exception {
        ospfController.activate();
        assertThat(ospfController, is(notNullValue()));
    }

    @Test(expected = Exception.class)
    public void testDeactivate() throws Exception {
        ospfController.activate();
        ospfController.deactivate();
        assertThat(ospfController, is(notNullValue()));
    }

    /**
     * Tests addRouterListener() method.
     */
    @Test
    public void testAddRouterListener() throws Exception {
        ospfRouterListener = EasyMock.createMock(OspfRouterListener.class);
        ospfController.addRouterListener(ospfRouterListener);
        assertThat(ospfController, is(notNullValue()));
    }

    /**
     * Tests removeRouterListener() method.
     */
    @Test
    public void testRemoveRouterListener() throws Exception {
        ospfRouterListener = EasyMock.createMock(OspfRouterListener.class);
        ospfController.addRouterListener(ospfRouterListener);
        ospfController.removeRouterListener(ospfRouterListener);
        assertThat(ospfController, is(notNullValue()));
    }

    /**
     * Tests addLinkListener() method.
     */
    @Test
    public void testAddLinkListener() throws Exception {
        ospfLinkListener = EasyMock.createMock(OspfLinkListener.class);
        ospfController.addLinkListener(ospfLinkListener);
        assertThat(ospfController, is(notNullValue()));
    }

    /**
     * Tests removeLinkListener() method.
     */
    @Test
    public void testRemoveLinkListener() throws Exception {
        ospfLinkListener = EasyMock.createMock(OspfLinkListener.class);
        ospfController.addLinkListener(ospfLinkListener);
        ospfController.removeLinkListener(ospfLinkListener);
        assertThat(ospfController, is(notNullValue()));
    }

    /**
     * Tests deleteConfig() method.
     */
    @Test
    public void testDeleteConfig() throws Exception {
        ospfProcess = new OspfProcessImpl();
        ospfArea = new OspfAreaImpl();
        ospfInterface = new OspfInterfaceImpl();
        ospfInterfaces = new ArrayList();
        ospfInterface.setIpAddress(Ip4Address.valueOf("10.10.10.5"));
        ospfInterfaces.add(ospfInterface);
        ospfArea.setAreaId(Ip4Address.valueOf("2.2.2.2"));
        ospfArea.setOspfInterfaceList(ospfInterfaces);
        ospfProcess.setProcessId("10.10.10.10");
        areas = new ArrayList();
        areas.add(ospfArea);
        ospfProcess.setAreas(areas);
        ospfProcesses = new ArrayList();
        ospfProcesses.add(ospfProcess);
        process1 = new OspfProcessImpl();
        process1.setProcessId("11.11.11.11");
        ospfArea1 = new OspfAreaImpl();
        ospfArea1.setAreaId(Ip4Address.valueOf("2.2.2.2"));
        ospfArea1.setOspfInterfaceList(ospfInterfaces);
        areas.add(ospfArea1);
        process1.setAreas(areas);
        ospfProcesses.add(process1);
        ospfController.deleteConfig(ospfProcesses, "INTERFACE");
        assertThat(ospfController, is(notNullValue()));
    }

    /**
     * Tests addLink() method.
     */
    @Test
    public void testAddLink() throws Exception {
        ospfRouter = new OspfRouterImpl();

        ospfController.agent.addLink(ospfRouter, new OspfLinkTedImpl());
        assertThat(ospfController, is(notNullValue()));
    }

    /**
     * Tests deleteLink() method.
     */
    @Test
    public void testDeleteLink() throws Exception {
        ospfRouter = new OspfRouterImpl();

        ospfController.agent.addLink(ospfRouter, new OspfLinkTedImpl());
        ospfController.agent.deleteLink(ospfRouter, new OspfLinkTedImpl());
        assertThat(ospfController, is(notNullValue()));
    }

    /**
     * Tests listener() method.
     */
    @Test
    public void testListener() throws Exception {
        assertThat(ospfController.listener().size(), is(0));
    }

    /**
     * Tests linkListener() method.
     */
    @Test
    public void testLinkListener() throws Exception {
        assertThat(ospfController.linkListener().size(), is(0));
    }

    /**
     * Tests addConnectedRouter() method.
     */
    @Test
    public void testaddConnectedRouter() throws Exception {
        ospfRouter = new OspfRouterImpl();

        ospfController.agent.addConnectedRouter(ospfRouter);
        assertThat(ospfController, is(notNullValue()));
    }

    /**
     * Tests removeConnectedRouter() method.
     */
    @Test
    public void testRemoveConnectedRouter() throws Exception {
        ospfRouter = new OspfRouterImpl();

        ospfController.agent.addConnectedRouter(ospfRouter);
        ospfController.agent.removeConnectedRouter(ospfRouter);
        assertThat(ospfController, is(notNullValue()));
    }

    /**
     * Tests getAllConfiguredProcesses() method.
     */
    @Test(expected = Exception.class)
    public void testGetAllConfiguredProcesses() throws Exception {
        assertThat(ospfController.getAllConfiguredProcesses().size(), is(0));
    }
}