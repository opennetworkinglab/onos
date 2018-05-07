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
package org.onosproject.drivers.netconf;

import static org.junit.Assert.fail;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.IOUtils;
import org.onosproject.core.CoreService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractDriverLoader;
import org.onosproject.net.driver.Behaviour;
import org.onosproject.net.driver.DefaultDriver;
import org.onosproject.net.driver.DefaultDriverData;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.DriverData;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.flow.FlowRuleProgrammable;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.common.io.Resources;

public class MockDriverHandler implements DriverHandler {

    private DriverData mockDriverData;
    private NetconfController ncc;
    private CoreService coreService;
    private DeviceService deviceService;
    private MastershipService mastershipService;

    // Centralize some initialization.
    private void init(Map<Class<? extends Behaviour>, Class<? extends Behaviour>> behaviours, DeviceId mockDeviceId,
            CoreService mockCoreService, DeviceService mockDeviceService) throws NetconfException {
        Map<String, String> properties = new HashMap<String, String>();

        Driver mockDriver = new DefaultDriver("mockDriver", null, "ONOSProject", "1.0.0", "1.0.0", behaviours,
                properties);
        mockDriverData = new DefaultDriverData(mockDriver, mockDeviceId);

        ncc = new MockNetconfController();
        ncc.connectDevice(mockDeviceId);
        coreService = mockCoreService;
        mastershipService = new MockMastershipService();
        deviceService = mockDeviceService;
    }

    @SuppressWarnings("unchecked")
    public MockDriverHandler(Class<? extends AbstractDriverLoader> loaderClass, String behaviorSpec,
            DeviceId mockDeviceId, CoreService mockCoreService, DeviceService mockDeviceService) {

        // Had to split into declaration and initialization to make stylecheck happy
        // else line was considered too long
        // and auto format couldn't be tweak to make it correct
        Map<Class<? extends Behaviour>, Class<? extends Behaviour>> behaviours;
        behaviours = new HashMap<Class<? extends Behaviour>, Class<? extends Behaviour>>();

        try {
            String data = Resources.toString(Resources.getResource(loaderClass, behaviorSpec), StandardCharsets.UTF_8);
            InputStream resp = IOUtils.toInputStream(data, StandardCharsets.UTF_8);
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document document = builder.parse(resp);

            XPath xp = XPathFactory.newInstance().newXPath();
            NodeList list = (NodeList) xp.evaluate("//behaviour", document, XPathConstants.NODESET);
            for (int i = 0; i < list.getLength(); i += 1) {
                Node node = list.item(i);
                NamedNodeMap attrs = node.getAttributes();
                Class<? extends Behaviour> api = (Class<? extends Behaviour>) Class
                        .forName(attrs.getNamedItem("api").getNodeValue());
                Class<? extends Behaviour> impl = (Class<? extends Behaviour>) Class
                        .forName(attrs.getNamedItem("impl").getNodeValue());
                behaviours.put(api, impl);
            }
            init(behaviours, mockDeviceId, mockCoreService, mockDeviceService);
        } catch (Exception e) {
            fail(e.toString());
        }
    }

    public MockDriverHandler() throws NetconfException {
        Map<Class<? extends Behaviour>, Class<? extends Behaviour>> behaviours;
        behaviours = new HashMap<Class<? extends Behaviour>, Class<? extends Behaviour>>();
        behaviours.put(FlowRuleProgrammable.class, FlowRuleProgrammable.class);
        DeviceId mockDeviceId = DeviceId.deviceId("netconf:1.2.3.4:830");
        coreService = new MockCoreService();
        init(behaviours, mockDeviceId, coreService, null);
    }

    @Override
    public Driver driver() {
        return mockDriverData.driver();
    }

    @Override
    public DriverData data() {
        return mockDriverData;
    }

    @Override
    public <T extends Behaviour> T behaviour(Class<T> behaviourClass) {
        // TODO Auto-generated method stub
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(Class<T> serviceClass) {
        if (serviceClass.equals(NetconfController.class)) {
            return (T) ncc;
        } else if (serviceClass.equals(CoreService.class)) {
            return (T) coreService;
        } else if (serviceClass.equals(MastershipService.class)) {
            return (T) mastershipService;
        } else if (serviceClass.equals(DeviceService.class)) {
            return (T) deviceService;
        }

        return null;
    }

}
