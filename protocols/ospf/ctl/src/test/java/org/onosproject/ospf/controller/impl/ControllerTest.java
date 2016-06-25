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
import org.onosproject.net.driver.DriverService;
import org.onosproject.ospf.controller.OspfAgent;
import org.onosproject.ospf.controller.OspfArea;
import org.onosproject.ospf.controller.OspfInterface;
import org.onosproject.ospf.controller.OspfProcess;
import org.onosproject.ospf.controller.OspfRouter;
import org.onosproject.ospf.controller.area.OspfAreaImpl;
import org.onosproject.ospf.controller.area.OspfInterfaceImpl;
import org.onosproject.ospf.controller.area.OspfProcessImpl;

import java.nio.channels.Channel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit test class for Controller.
 */
public class ControllerTest {
    private Controller controller;
    private Map<String, Long> maps;
    private OspfAgent ospfAgent;
    private DriverService driverService;
    private List<Channel> connectedChannels;
    private List<OspfProcess> process;
    private OspfProcess ospfProcess;
    private OspfArea ospfArea;
    private OspfInterface ospfInterface;
    private List<OspfInterface> ospfInterfaces;
    private List<OspfArea> ospfAreas;
    private List<OspfProcess> ospfProcesses;
    private OspfProcess ospfProcess1;
    private OspfArea ospfArea1;
    private OspfRouter ospfRouter;

    @Before
    public void setUp() throws Exception {
        controller = new Controller();
        maps = new HashMap<String, Long>();
        ospfProcess = new OspfProcessImpl();
        ospfArea = new OspfAreaImpl();
        ospfInterface = new OspfInterfaceImpl();
        ospfInterfaces = new ArrayList();
        ospfInterface.setIpAddress(Ip4Address.valueOf("1.1.1.1"));
        ospfInterfaces.add(ospfInterface);
        ospfArea.setAreaId(Ip4Address.valueOf("2.2.2.2"));
        ospfArea.setInterfacesLst(ospfInterfaces);
        ospfProcess.setProcessId("10.10.10.10");
        ospfAreas = new ArrayList();
        ospfAreas.add(ospfArea);
        ospfProcess.setAreas(ospfAreas);
        ospfProcesses = new ArrayList();
        ospfProcesses.add(ospfProcess);
        ospfProcess1 = new OspfProcessImpl();
        ospfProcess1.setProcessId("11.11.11.11");
        ospfArea1 = new OspfAreaImpl();
        ospfArea1.setAreaId(Ip4Address.valueOf("2.2.2.2"));
        ospfArea1.setInterfacesLst(ospfInterfaces);
        ospfAreas.add(ospfArea1);
        ospfProcess1.setAreas(ospfAreas);
        ospfProcesses.add(ospfProcess1);
        connectedChannels = new ArrayList<>();
    }

    @After
    public void tearDown() throws Exception {
        controller = null;
        maps.clear();
        connectedChannels.clear();
        controller = null;
        maps = null;
        ospfAgent = null;
        driverService = null;
        connectedChannels = null;
        process = null;
        ospfProcess = null;
        ospfArea = null;
        ospfInterface = null;
        ospfInterfaces = null;
        ospfAreas = null;
        ospfProcesses = null;
        ospfProcess1 = null;
        ospfArea1 = null;
    }

    /**
     * Tests getAllConfiguredProcesses() method.
     */
    @Test
    public void testGetAllConfiguredProcesses() throws Exception {
        process = controller.getAllConfiguredProcesses();
        assertThat(process, is(nullValue()));
    }

    /**
     * Tests addDeviceDetails() method.
     */
    @Test(expected = Exception.class)
    public void testAddDeviceDetails() throws Exception {
        controller.addDeviceDetails(new OspfRouterImpl());
        assertThat(controller, is(notNullValue()));
    }

    /**
     * Tests removeDeviceDetails() method.
     */
    @Test(expected = Exception.class)
    public void testRemoveDeviceDetails() throws Exception {
        controller.removeDeviceDetails(new OspfRouterImpl());
        assertThat(controller, is(notNullValue()));
    }

    /**
     * Tests init() method.
     */
    @Test
    public void testInit() throws Exception {
        controller.init();
        assertThat(controller.systemStartTime, is(notNullValue()));
    }

    /**
     * Tests start() method.
     */
    @Test
    public void testStart() throws Exception {
        ospfAgent = EasyMock.createMock(OspfAgent.class);
        controller.start(ospfAgent, driverService);
        controller.updateConfig(ospfProcesses);
        assertThat(controller, is(notNullValue()));
    }

    /**
     * Tests stop() method.
     */
    @Test(expected = Exception.class)
    public void testStop() throws Exception {
        controller.start(ospfAgent, driverService);
        controller.stop();
        assertThat(controller, is(notNullValue()));
    }

    /**
     * Tests deleteInterfaceFromArea() method.
     */
    @Test
    public void testDeleteInterfaceFromArea() throws Exception {
        controller.updateConfig(ospfProcesses);
        assertThat(controller.deleteInterfaceFromArea("10.10.10.10", "2.2.2.2", "1.1.1.1"), is(true));
        assertThat(controller.deleteInterfaceFromArea("10.10.10.10", "2.2.2.2", "5.5.5.5"), is(false));
    }

    /**
     * Tests checkArea() method.
     */
    @Test
    public void testCheckArea() throws Exception {
        controller.updateConfig(ospfProcesses);
        assertThat(controller.checkArea("10.10.10.10", "2.2.2.2"), is(true));
    }

    /**
     * Tests checkArea() method.
     */
    @Test
    public void testCheckArea1() throws Exception {
        controller.updateConfig(ospfProcesses);
        assertThat(controller.checkArea("10.10.10.10", "111.111.111.111"), is(false));
    }

    /**
     * Tests checkProcess() method.
     */
    @Test
    public void testCheckProcess() throws Exception {
        controller.updateConfig(ospfProcesses);
        assertThat(controller.checkProcess("3.3.3.3"), is(false));
        assertThat(controller.checkProcess("1.1.1.1"), is(false));
    }

    /**
     * Tests checkInterface() method.
     */
    @Test
    public void testCheckInterface() throws Exception {
        controller.updateConfig(ospfProcesses);
        assertThat(controller.checkInterface("10.10.10.10", "2.2.2.2", "1.1.1.1"), is(true));
    }

    /**
     * Tests updateAreaInProcess() method.
     */
    @Test
    public void testUpdateAreaInProcess() throws Exception {
        controller.updateConfig(ospfProcesses);
        controller.updateAreaInProcess("10.10.10.10", "2.2.2.2", ospfArea);
        assertThat(controller, is(notNullValue()));
    }

    /**
     * Tests updateConfig() method.
     */
    @Test
    public void testUpdateConfig() throws Exception {
        controller.updateConfig(ospfProcesses);
        controller.updateConfig(ospfProcesses);
        controller.updateConfig(ospfProcesses);
        assertThat(controller, is(notNullValue()));
    }

    /**
     * Tests updateConfig() method.
     */
    @Test
    public void testUpdateConfig2() throws Exception {
        controller.updateConfig(ospfProcesses);
        controller.updateConfig(ospfProcesses);
        assertThat(controller, is(notNullValue()));
    }

    /**
     * Tests updateConfig() method.
     */
    @Test
    public void testUpdateConfig1() throws Exception {
        ospfProcess = new OspfProcessImpl();
        ospfArea = new OspfAreaImpl();
        ospfInterface = new OspfInterfaceImpl();
        ospfInterfaces = new ArrayList();
        ospfInterface.setIpAddress(Ip4Address.valueOf("10.10.10.5"));
        ospfInterfaces.add(ospfInterface);
        ospfArea.setAreaId(Ip4Address.valueOf("2.2.2.2"));
        ospfArea.setInterfacesLst(ospfInterfaces);
        ospfProcess.setProcessId("10.10.10.10");
        ospfAreas = new ArrayList();
        ospfAreas.add(ospfArea);
        ospfProcess.setAreas(ospfAreas);
        ospfProcesses = new ArrayList();
        ospfProcesses.add(ospfProcess);
        controller.updateConfig(ospfProcesses);
        assertThat(controller, is(notNullValue()));
    }

    /**
     * Tests deleteConfig() method.
     */
    @Test(expected = Exception.class)
    public void testDeleteConfig() throws Exception {
        controller.updateConfig(ospfProcesses);
        controller.deleteConfig(ospfProcesses, "INTERFACE");
        assertThat(controller, is(notNullValue()));
    }

    /**
     * Tests deleteConfig() method.
     */
    @Test(expected = Exception.class)
    public void testDeleteConfig1() throws Exception {
        controller.updateConfig(ospfProcesses);
        controller.deleteConfig(ospfProcesses, "AREA");
        assertThat(controller, is(notNullValue()));
    }

    /**
     * Tests deleteConfig() method.
     */

    @Test
    public void testDeleteConfig2() throws Exception {
        controller.updateConfig(ospfProcesses);
        controller.deleteConfig(ospfProcesses, "PROCESS");
        assertThat(controller, is(notNullValue()));
    }

    /**
     * Tests deleteConfig() method.
     */
    @Test
    public void testDeleteConfig3() throws Exception {
        ospfProcesses = new ArrayList();
        controller.deleteConfig(ospfProcesses, "PROCESS");
        assertThat(controller, is(notNullValue()));
    }

    /**
     * Tests deleteConfig() method.
     */
    @Test
    public void testDeleteConfig4() throws Exception {
        controller.updateConfig(ospfProcesses);
        controller.deleteConfig(ospfProcesses, "PROCESS");
        controller.updateConfig(ospfProcesses);
        assertThat(controller, is(notNullValue()));
    }

    /**
     * Tests deleteProcessWhenExists() method.
     */
    @Test
    public void testDeleteProcessWhenExists() throws Exception {
        controller.updateConfig(ospfProcesses);
        controller.deleteProcessWhenExists(ospfProcesses, "PROCESS");
    }

    /**
     * Tests addLinkDetails() method.
     */
    @Test
    public void testAddLinkDetails() throws Exception {
        ospfAgent = EasyMock.createMock(OspfAgent.class);
        controller.start(ospfAgent, driverService);
        ospfRouter = new OspfRouterImpl();
        controller.addLinkDetails(ospfRouter, new OspfLinkTedImpl());
        assertThat(controller, is(notNullValue()));
    }

    /**
     * Tests removeLinkDetails() method.
     */
    @Test
    public void testRemoveLinkDetails() throws Exception {
        ospfAgent = EasyMock.createMock(OspfAgent.class);
        controller.start(ospfAgent, driverService);
        ospfRouter = new OspfRouterImpl();
        controller.addLinkDetails(ospfRouter, new OspfLinkTedImpl());
        controller.removeLinkDetails(ospfRouter);
        assertThat(controller, is(notNullValue()));
    }
}