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

package org.onosproject.drivers.fujitsu;

import com.google.common.collect.ImmutableList;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.onosproject.drivers.utilities.XmlConfigParser;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.ConfigurationNode;
import org.onlab.packet.IpAddress;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.Annotations;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.ControllerConfig;
import org.onosproject.net.behaviour.ControllerInfo;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfDevice;
import org.onosproject.netconf.NetconfException;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.drivers.fujitsu.FujitsuVoltXmlUtility.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation to get and set parameters available in VOLT NE
 * through the Netconf protocol.
 */
public class FujitsuVoltControllerConfig extends AbstractHandlerBehaviour
        implements ControllerConfig {

    private final Logger log = getLogger(FujitsuVoltControllerConfig.class);
    private static final String RESOURCE_XML = "voltcontrollers.xml";

    private static final String DOT = ".";
    private static final String DATA = "data";
    private static final String VOLT_OFCONFIG = "volt-ofconfig";
    private static final String OF_CONTROLLERS = "of-controllers";
    private static final String OF_CONTROLLER = "of-controller";
    private static final String CONTROLLER_INFO = "controller-info";
    private static final String IP_ADDRESS = "ip-address";
    private static final String PORT = "port";
    private static final String PROTOCOL = "protocol";
    private static final String CONFIG = "config";
    private static final String OFCONFIG_ID = "ofconfig-id";
    private static final String TARGET = "target";
    private static final String MERGE = "merge";
    private static final String DEFAULT_OPERATION = "default-operation";

    private static final String END_LICENSE_HEADER = "-->";

    private static final String VOLT_DATACONFIG = DATA + DOT + VOLT_NE + DOT +
            VOLT_OFCONFIG + DOT + OF_CONTROLLERS + DOT + OF_CONTROLLER;

    private static final String EDIT_CONFIG_TG = EDIT_CONFIG + DOT + TARGET;
    private static final String EDIT_CONFIG_DO = EDIT_CONFIG + DOT + DEFAULT_OPERATION;
    private static final String CONTROLLER_INFO_ID = CONTROLLER_INFO + DOT + "id";
    private static final String CONTROLLER_INFO_IP = CONTROLLER_INFO + DOT + IP_ADDRESS;
    private static final String CONTROLLER_INFO_PORT = CONTROLLER_INFO + DOT + PORT;
    private static final String CONTROLLER_INFO_PROTOCOL = CONTROLLER_INFO + DOT + PROTOCOL;
    private static final String VOLT_EDITCONFIG = EDIT_CONFIG + DOT +
            CONFIG + DOT + VOLT_NE + DOT + VOLT_OFCONFIG + DOT + OF_CONTROLLERS;

    @Override
    public List<ControllerInfo> getControllers() {
        DriverHandler handler = handler();
        NetconfController controller = handler.get(NetconfController.class);
        MastershipService mastershipService = handler.get(MastershipService.class);
        DeviceId ncDeviceId = handler.data().deviceId();
        checkNotNull(controller, "Netconf controller is null");
        List<ControllerInfo> controllers = new ArrayList<>();
        if (mastershipService.isLocalMaster(ncDeviceId)) {
            try {
                StringBuilder request = new StringBuilder();
                request.append(VOLT_NE_OPEN + VOLT_NE_NAMESPACE + ">\n");
                request.append(buildEmptyTag(VOLT_OFCONFIG));
                request.append(VOLT_NE_CLOSE);

                String reply;
                reply = controller
                            .getDevicesMap()
                            .get(ncDeviceId)
                            .getSession()
                            .get(request.toString(), REPORT_ALL);
                log.debug("Reply XML {}", reply);
                controllers.addAll(parseStreamVoltControllers(XmlConfigParser.
                        loadXml(new ByteArrayInputStream(reply.getBytes(StandardCharsets.UTF_8)))));
            } catch (IOException e) {
                log.error("Cannot communicate to device {} ", ncDeviceId);
            }
        } else {
            log.warn("I'm not master for {} please use master, {} to execute command",
                     ncDeviceId,
                     mastershipService.getMasterFor(ncDeviceId));
        }
        return ImmutableList.copyOf(controllers);
    }

    @Override
    public void setControllers(List<ControllerInfo> controllers) {
        DriverHandler handler = handler();
        NetconfController controller = handler.get(NetconfController.class);
        MastershipService mastershipService = handler.get(MastershipService.class);
        DeviceId ncdeviceId = handler.data().deviceId();
        checkNotNull(controller, "Netconf controller is null");
        if (mastershipService.isLocalMaster(ncdeviceId)) {
            try {
                NetconfDevice device = controller.getNetconfDevice(ncdeviceId);
                String config = createVoltControllersConfig(
                        XmlConfigParser.loadXml(getClass().
                                getResourceAsStream(RESOURCE_XML)),
                        RUNNING, MERGE, controllers);
                device.getSession().editConfig(config.substring(
                        config.indexOf(END_LICENSE_HEADER) + END_LICENSE_HEADER.length()));
            } catch (NetconfException e) {
                log.error("Cannot communicate to device {} , exception {}", ncdeviceId, e);
            }
        } else {
            log.warn("I'm not master for {} please use master, {} to execute command",
                     ncdeviceId,
                     mastershipService.getMasterFor(ncdeviceId));
        }
    }


    /**
     * Parses XML string to get controller information.
     *
     * @param cfg a hierarchical configuration
     * @return a list of controllers
     */
    private List<ControllerInfo> parseStreamVoltControllers(HierarchicalConfiguration cfg) {
        List<ControllerInfo> controllers = new ArrayList<>();
        List<HierarchicalConfiguration> fields =
                cfg.configurationsAt(VOLT_DATACONFIG);

        for (HierarchicalConfiguration sub : fields) {
            List<HierarchicalConfiguration> childFields =
                    sub.configurationsAt(CONTROLLER_INFO);

            for (HierarchicalConfiguration child : childFields) {
                Annotations annotations = DefaultAnnotations.builder()
                        .set(OFCONFIG_ID, sub.getString(OFCONFIG_ID)).build();
                ControllerInfo controller = new ControllerInfo(
                        IpAddress.valueOf(child.getString(IP_ADDRESS)),
                        Integer.parseInt(child.getString(PORT)),
                        child.getString(PROTOCOL), annotations);

                log.debug("VOLT: OFCONTROLLER: PROTOCOL={}, IP={}, PORT={}, ID={} ",
                          controller.type(), controller.ip(),
                          controller.port(), controller.annotations().value(OFCONFIG_ID));
                controllers.add(controller);
            }
        }
        return controllers;
    }

    /**
     * Forms XML string to change controller information.
     *
     * @param cfg a hierarchical configuration
     * @param target the type of configuration
     * @param netconfOperation operation type
     * @param controllers list of controllers
     * @return XML string
     */
    private String createVoltControllersConfig(HierarchicalConfiguration cfg,
                                                     String target, String netconfOperation,
                                                     List<ControllerInfo> controllers) {
        XMLConfiguration editcfg = null;

        cfg.setProperty(EDIT_CONFIG_TG, target);
        cfg.setProperty(EDIT_CONFIG_DO, netconfOperation);

        List<ConfigurationNode> newControllers = new ArrayList<>();
        for (ControllerInfo ci : controllers) {
            XMLConfiguration controller = new XMLConfiguration();
            controller.setRoot(new HierarchicalConfiguration.Node(OF_CONTROLLER));
            controller.setProperty(OFCONFIG_ID, ci.annotations().value(OFCONFIG_ID));
            controller.setProperty(CONTROLLER_INFO_ID, ci.annotations().value(OFCONFIG_ID));
            controller.setProperty(CONTROLLER_INFO_IP, ci.ip());
            controller.setProperty(CONTROLLER_INFO_PORT, ci.port());
            controller.setProperty(CONTROLLER_INFO_PROTOCOL, ci.type());
            newControllers.add(controller.getRootNode());
        }
        cfg.addNodes(VOLT_EDITCONFIG, newControllers);

        try {
             editcfg = (XMLConfiguration) cfg;
        } catch (ClassCastException e) {
            e.printStackTrace();
        }
        StringWriter stringWriter = new StringWriter();
        try {
            editcfg.save(stringWriter);
        } catch (ConfigurationException e) {
            e.printStackTrace();
        }
        String s = stringWriter.toString();
        String fromStr = buildStartTag(TARGET, false) + target +
                   buildEndTag(TARGET, false);
        String toStr = buildStartTag(TARGET, false) +
                   buildEmptyTag(target, false) + buildEndTag(TARGET, false);
        s = s.replace(fromStr, toStr);
        return s;
    }

}
