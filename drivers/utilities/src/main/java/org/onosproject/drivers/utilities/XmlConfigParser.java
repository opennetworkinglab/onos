/*
 * Copyright 2015 Open Networking Laboratory
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

package org.onosproject.drivers.utilities;

import com.google.common.collect.Lists;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.ConfigurationNode;
import org.onlab.packet.IpAddress;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.CltSignalType;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.GridType;
import org.onosproject.net.OchSignal;
import org.onosproject.net.OduSignalType;
import org.onosproject.net.PortNumber;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.behaviour.ControllerInfo;
import org.onosproject.net.device.OchPortDescription;
import org.onosproject.net.device.OduCltPortDescription;
import org.onosproject.net.device.PortDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Parser for Netconf XML configurations and replys.
 */
public final class XmlConfigParser {
    public static final Logger log = LoggerFactory
            .getLogger(XmlConfigParser.class);

    private XmlConfigParser() {
        //not called, preventing any allocation
    }


    public static HierarchicalConfiguration loadXml(InputStream xmlStream) {
        XMLConfiguration cfg = new XMLConfiguration();
        try {
            cfg.load(xmlStream);
            return cfg;
        } catch (ConfigurationException e) {
            throw new IllegalArgumentException("Cannot load xml from Stream", e);
        }
    }

    public static List<ControllerInfo> parseStreamControllers(HierarchicalConfiguration cfg) {
        List<ControllerInfo> controllers = new ArrayList<>();
        List<HierarchicalConfiguration> fields =
                cfg.configurationsAt("data.capable-switch." +
                                             "logical-switches." +
                                             "switch.controllers.controller");
        for (HierarchicalConfiguration sub : fields) {
            controllers.add(new ControllerInfo(
                    IpAddress.valueOf(sub.getString("ip-address")),
                    Integer.parseInt(sub.getString("port")),
                    sub.getString("protocol")));
        }
        return controllers;
    }

    /**
     * Parses a configuration and returns a set of ports for the fujitsu T100.
     * @param cfg a hierarchical configuration
     * @return a list of port descriptions
     */
    public static List<PortDescription> parseFujitsuT100Ports(HierarchicalConfiguration cfg) {
        AtomicInteger counter = new AtomicInteger(1);
        List<PortDescription> portDescriptions = Lists.newArrayList();
        List<HierarchicalConfiguration> subtrees =
                cfg.configurationsAt("data.interfaces.interface");
        for (HierarchicalConfiguration portConfig : subtrees) {
            if (!portConfig.getString("name").contains("LCN") &&
                    !portConfig.getString("name").contains("LMP") &&
                    portConfig.getString("type").equals("ianaift:ethernetCsmacd")) {
                portDescriptions.add(parseT100OduPort(portConfig, counter.getAndIncrement()));
            } else if (portConfig.getString("type").equals("ianaift:otnOtu")) {
                portDescriptions.add(parseT100OchPort(portConfig, counter.getAndIncrement()));
            }
        }
        return portDescriptions;
    }

    private static OchPortDescription parseT100OchPort(HierarchicalConfiguration cfg, long count) {
        PortNumber portNumber = PortNumber.portNumber(count);
        HierarchicalConfiguration otuConfig = cfg.configurationAt("otu");
        boolean enabled = otuConfig.getString("administrative-state").equals("up");
        OduSignalType signalType = otuConfig.getString("rate").equals("OTU4") ? OduSignalType.ODU4 : null;
        //Unsure how to retreive, outside knowledge it is tunable.
        boolean isTunable = true;
        OchSignal lambda = new OchSignal(GridType.DWDM, ChannelSpacing.CHL_50GHZ, 0, 4);
        DefaultAnnotations annotations = DefaultAnnotations.builder().
                set(AnnotationKeys.PORT_NAME, cfg.getString("name")).
                build();
        return new OchPortDescription(portNumber, enabled, signalType, isTunable, lambda, annotations);
    }

    private static OduCltPortDescription parseT100OduPort(HierarchicalConfiguration cfg, long count) {
        PortNumber portNumber = PortNumber.portNumber(count);
        HierarchicalConfiguration ethernetConfig = cfg.configurationAt("ethernet");
        boolean enabled = ethernetConfig.getString("administrative-state").equals("up");
        //Rate is in kbps
        CltSignalType signalType = ethernetConfig.getString("rate").equals("100000000") ?
                CltSignalType.CLT_100GBE : null;
        DefaultAnnotations annotations = DefaultAnnotations.builder().
                set(AnnotationKeys.PORT_NAME, cfg.getString("name")).
                build();
        return new OduCltPortDescription(portNumber, enabled, signalType, annotations);
    }

    protected static String parseSwitchId(HierarchicalConfiguration cfg) {
        HierarchicalConfiguration field =
                cfg.configurationAt("data.capable-switch." +
                                            "logical-switches." +
                                            "switch");
        return field.getProperty("id").toString();
    }

    public static String parseCapableSwitchId(HierarchicalConfiguration cfg) {
        HierarchicalConfiguration field =
                cfg.configurationAt("data.capable-switch");
        return field.getProperty("id").toString();
    }

    public static String createControllersConfig(HierarchicalConfiguration cfg,
                                                 HierarchicalConfiguration actualCfg,
                                                 String target, String netconfOperation,
                                                 String controllerOperation,
                                                 List<ControllerInfo> controllers) {
        //cfg.getKeys().forEachRemaining(key -> System.out.println(key));
        cfg.setProperty("edit-config.target", target);
        cfg.setProperty("edit-config.default-operation", netconfOperation);
        cfg.setProperty("edit-config.config.capable-switch.id",
                        parseCapableSwitchId(actualCfg));
        cfg.setProperty("edit-config.config.capable-switch." +
                                "logical-switches.switch.id", parseSwitchId(actualCfg));
        List<ConfigurationNode> newControllers = new ArrayList<>();
        for (ControllerInfo ci : controllers) {
            XMLConfiguration controller = new XMLConfiguration();
            controller.setRoot(new HierarchicalConfiguration.Node("controller"));
            String id = ci.type() + ":" + ci.ip() + ":" + ci.port();
            controller.setProperty("id", id);
            controller.setProperty("ip-address", ci.ip());
            controller.setProperty("port", ci.port());
            controller.setProperty("protocol", ci.type());
            newControllers.add(controller.getRootNode());
        }
        cfg.addNodes("edit-config.config.capable-switch.logical-switches." +
                             "switch.controllers", newControllers);
        XMLConfiguration editcfg = (XMLConfiguration) cfg;
        StringWriter stringWriter = new StringWriter();
        try {
            editcfg.save(stringWriter);
        } catch (ConfigurationException e) {
            log.error("createControllersConfig()", e);
        }
        String s = stringWriter.toString()
                .replaceAll("<controller>",
                            "<controller nc:operation=\"" + controllerOperation + "\">");
        s = s.replace("<target>" + target + "</target>",
                      "<target><" + target + "/></target>");
        return s;

    }

    public static List<HierarchicalConfiguration> parseWaveServerCienaPorts(HierarchicalConfiguration cfg) {
        return cfg.configurationsAt("ws-ports.port-interface");
    }

    public static PortDescription parseWaveServerCienaOchPorts(long portNumber, long oduPortSpeed,
                                                               HierarchicalConfiguration config,
                                                               SparseAnnotations annotations) {
        final List<String> tunableType = Lists.newArrayList("Performance-Optimized", "Accelerated");
        final String transmitterPath = "ptp-config.transmitter-state";
        final String tunablePath = "ptp-config.adv-config.tx-tuning-mode";
        final String gridTypePath = "ptp-config.adv-config.wl-spacing";
        final String frequencyPath = "ptp-config.adv-config.frequency";

        boolean isEnabled = config.getString(transmitterPath).equals("enabled");
        boolean isTunable = tunableType.contains(config.getString(tunablePath));

        //FIXME change when all optical types have two way information methods, see jira tickets
        final int speed100GbpsinMbps = 100000;
        OduSignalType oduSignalType = oduPortSpeed == speed100GbpsinMbps ? OduSignalType.ODU4 : null;
        GridType gridType = config.getString(gridTypePath).equals("FlexGrid") ? GridType.FLEX : null;
        ChannelSpacing chSpacing = gridType == GridType.FLEX ? ChannelSpacing.CHL_6P25GHZ : null;

        //Working in Ghz //(Nominal central frequency - 193.1)/channelSpacing = spacingMultiplier
        final int baseFrequency = 193100;
        int spacingMult = (int) (toGbps((Integer.parseInt(config.getString(frequencyPath)) -
                baseFrequency)) / toGbpsFromHz(chSpacing.frequency().asHz())); //FIXME is there a better way ?

        return new OchPortDescription(PortNumber.portNumber(portNumber), isEnabled, oduSignalType, isTunable,
                                      new OchSignal(gridType, chSpacing, spacingMult, 1), annotations);
    }

    //FIXME remove when all optical types have two way information methods, see jira tickets
    private static long toGbps(long speed) {
        return speed * 1000;
    }

    private static long toGbpsFromHz(long speed) {
        return speed / 1000;
    }
    //TODO implement mor methods for parsing configuration when you need them

    /**
     * Parses a config reply and returns the result.
     * @param reply a tree-like source
     * @return the configuration result
     */
    public static boolean configSuccess(HierarchicalConfiguration reply) {
        if (reply != null) {
            if (reply.containsKey("ok")) {
                return true;
            }
        }
        return false;
    }
}
