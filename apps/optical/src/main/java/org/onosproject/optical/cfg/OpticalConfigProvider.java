/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.optical.cfg;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceProvider;
import org.onosproject.net.device.DeviceProviderRegistry;
import org.onosproject.net.device.DeviceProviderService;
import org.onosproject.net.link.DefaultLinkDescription;
import org.onosproject.net.link.LinkProvider;
import org.onosproject.net.link.LinkProviderRegistry;
import org.onosproject.net.link.LinkProviderService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onlab.packet.ChassisId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.onosproject.net.DeviceId.deviceId;

/**
 * OpticalConfigProvider emulates the SB network provider for optical switches,
 * optical links and any other state that needs to be configured for correct network
 * operations.
 *
 * @deprecated in Cardinal Release
 */
@Deprecated
@JsonIgnoreProperties(ignoreUnknown = true)
//@Component(immediate = true)
public class OpticalConfigProvider extends AbstractProvider implements DeviceProvider, LinkProvider {

    protected static final Logger log = LoggerFactory
            .getLogger(OpticalConfigProvider.class);

    // TODO: fix hard coded file path later.
    private static final String DEFAULT_CONFIG_FILE =
            "config/demo-3-roadm-2-ps.json";
    private String configFileName = DEFAULT_CONFIG_FILE;

//    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkProviderRegistry linkProviderRegistry;

//    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceProviderRegistry deviceProviderRegistry;

    private static final String OPTICAL_ANNOTATION = "optical.";

    private LinkProviderService linkProviderService;
    private DeviceProviderService deviceProviderService;

    private static final List<Roadm> RAW_ROADMS = new ArrayList<>();
    private static final List<WdmLink> RAW_WDMLINKS = new ArrayList<>();
    private static final List<PktOptLink> RAW_PKTOPTLINKS = new ArrayList<>();

    private static final String ROADM = "Roadm";
    private static final String WDM_LINK = "wdmLink";
    private static final String PKT_OPT_LINK = "pktOptLink";

    protected OpticalNetworkConfig opticalNetworkConfig;

    public OpticalConfigProvider() {
        super(new ProviderId("optical", "org.onosproject.provider" +
                ".opticalConfig"));
    }

//    @Activate
    protected void activate() {
        linkProviderService = linkProviderRegistry.register(this);
        deviceProviderService = deviceProviderRegistry.register(this);
        log.info("Starting optical network configuration process...");
        log.info("Optical config file set to {}", configFileName);

        loadOpticalConfig();
        parseOpticalConfig();
        publishOpticalConfig();
    }

//    @Deactivate
    protected void deactivate() {
        linkProviderRegistry.unregister(this);
        linkProviderService = null;
        deviceProviderRegistry.unregister(this);
        deviceProviderService = null;
        RAW_ROADMS.clear();
        RAW_WDMLINKS.clear();
        RAW_PKTOPTLINKS.clear();
        log.info("Stopped");
    }

    private void loadOpticalConfig() {
        ObjectMapper mapper = new ObjectMapper();
        opticalNetworkConfig = new OpticalNetworkConfig();
        try {
            opticalNetworkConfig = mapper.readValue(new File(configFileName), OpticalNetworkConfig.class);
        } catch (JsonParseException e) {
            String err = String.format("JsonParseException while loading network "
                    + "config from file: %s: %s", configFileName, e.getMessage());
            log.error(err, e);
        } catch (JsonMappingException e) {
            String err = String.format(
                    "JsonMappingException while loading network config "
                            + "from file: %s: %s", configFileName, e.getMessage());
            log.error(err, e);
        } catch (IOException e) {
            String err = String.format("IOException while loading network config "
                    + "from file: %s %s", configFileName, e.getMessage());
            log.error(err, e);
        }
    }

    private void parseOpticalConfig() {
        List<OpticalSwitchDescription> swList = opticalNetworkConfig.getOpticalSwitches();
        List<OpticalLinkDescription> lkList = opticalNetworkConfig.getOpticalLinks();

        for (OpticalSwitchDescription sw : swList) {
            String swtype = sw.getType();
            boolean allow = sw.isAllowed();
           if (swtype.equals(ROADM) && allow) {
               int regNum = 0;
               Set<Map.Entry<String, JsonNode>> m = sw.params.entrySet();
               for (Map.Entry<String, JsonNode> e : m) {
                       String key = e.getKey();
                       JsonNode j = e.getValue();
                       if (key.equals("numRegen")) {
                           regNum = j.asInt();
                       }
                }

                Roadm newRoadm = new Roadm();
                newRoadm.setName(sw.name);
                newRoadm.setNodeId(sw.nodeDpid);
                newRoadm.setLongtitude(sw.longitude);
                newRoadm.setLatitude(sw.latitude);
                newRoadm.setRegenNum(regNum);

                RAW_ROADMS.add(newRoadm);
                log.info(newRoadm.toString());
            }
           }

        for (OpticalLinkDescription lk : lkList) {
            String lktype = lk.getType();
            switch (lktype) {
            case WDM_LINK:
                WdmLink newWdmLink = new WdmLink();
                newWdmLink.setSrcNodeId(lk.getNodeDpid1());
                newWdmLink.setSnkNodeId(lk.getNodeDpid2());
                newWdmLink.setAdminWeight(1000); // default weight for each WDM link.
                Set<Map.Entry<String, JsonNode>> m = lk.params.entrySet();
                for (Map.Entry<String, JsonNode> e : m) {
                    String key = e.getKey();
                    JsonNode j = e.getValue();
                    if (key.equals("nodeName1")) {
                        newWdmLink.setSrcNodeName(j.asText());
                    } else if (key.equals("nodeName2")) {
                        newWdmLink.setSnkNodeName(j.asText());
                    } else if (key.equals("port1")) {
                        newWdmLink.setSrcPort(j.asInt());
                    } else if (key.equals("port2")) {
                        newWdmLink.setSnkPort(j.asInt());
                    } else if (key.equals("distKms")) {
                        newWdmLink.setDistance(j.asDouble());
                    } else if (key.equals("numWaves")) {
                        newWdmLink.setWavelengthNumber(j.asInt());
                    } else {
                        log.error("error found");
                        // TODO add exception processing;
                    }
                }
                RAW_WDMLINKS.add(newWdmLink);
                log.info(newWdmLink.toString());

                break;

            case PKT_OPT_LINK:
                PktOptLink newPktOptLink = new PktOptLink();
                newPktOptLink.setSrcNodeId(lk.getNodeDpid1());
                newPktOptLink.setSnkNodeId(lk.getNodeDpid2());
                newPktOptLink.setAdminWeight(10); // default weight for each packet-optical link.
                Set<Map.Entry<String, JsonNode>> ptm = lk.params.entrySet();
                for (Map.Entry<String, JsonNode> e : ptm) {
                    String key = e.getKey();
                    JsonNode j = e.getValue();
                    if (key.equals("nodeName1")) {
                        newPktOptLink.setSrcNodeName(j.asText());
                    } else if (key.equals("nodeName2")) {
                        newPktOptLink.setSnkNodeName(j.asText());
                    } else if (key.equals("port1")) {
                        newPktOptLink.setSrcPort(j.asInt());
                    } else if (key.equals("port2")) {
                        newPktOptLink.setSnkPort(j.asInt());
                    } else if (key.equals("bandWidth")) {
                        newPktOptLink.setBandwdith(j.asDouble());
                    } else {
                        log.error("error found");
                        // TODO add exception processing;
                    }
                }

                RAW_PKTOPTLINKS.add(newPktOptLink);
                log.info(newPktOptLink.toString());
                break;
            default:
            }
        }
    }

    private void publishOpticalConfig() {
        if (deviceProviderService == null || linkProviderService == null) {
            return;
        }

        // Discover the optical ROADM objects
        Iterator<Roadm> iterWdmNode = RAW_ROADMS.iterator();
        while (iterWdmNode.hasNext()) {
            Roadm value = iterWdmNode.next();
            DeviceId did = deviceId("of:" + value.getNodeId().replace(":", ""));
            ChassisId cid = new ChassisId();
            DefaultAnnotations extendedAttributes = DefaultAnnotations.builder()
                    .set(OPTICAL_ANNOTATION + "switchType", "ROADM")
                    .set(OPTICAL_ANNOTATION + "switchName", value.getName())
                    .set(OPTICAL_ANNOTATION + "latitude", Double.toString(value.getLatitude()))
                    .set(OPTICAL_ANNOTATION + "longtitude", Double.toString(value.getLongtitude()))
                    .set(OPTICAL_ANNOTATION + "regNum", Integer.toString(value.getRegenNum()))
                    .build();

            DeviceDescription description =
                    new DefaultDeviceDescription(did.uri(),
                                                 Device.Type.SWITCH,
                                                 "",
                                                 "",
                                                 "",
                                                 "",
                                                 cid,
                                                 extendedAttributes);
            deviceProviderService.deviceConnected(did, description);
        }

        // Discover the optical WDM link objects
        Iterator<WdmLink> iterWdmlink = RAW_WDMLINKS.iterator();
        while (iterWdmlink.hasNext()) {
            WdmLink value = iterWdmlink.next();

            DeviceId srcNodeId = deviceId("of:" + value.getSrcNodeId().replace(":", ""));
            DeviceId snkNodeId = deviceId("of:" + value.getSnkNodeId().replace(":", ""));

            PortNumber srcPort = PortNumber.portNumber(value.getSrcPort());
            PortNumber snkPort = PortNumber.portNumber(value.getSnkPort());

            ConnectPoint srcPoint = new ConnectPoint(srcNodeId, srcPort);
            ConnectPoint snkPoint = new ConnectPoint(snkNodeId, snkPort);

            DefaultAnnotations extendedAttributes = DefaultAnnotations.builder()
                    .set(OPTICAL_ANNOTATION + "linkType", "WDM")
                    .set(OPTICAL_ANNOTATION + "distance", Double.toString(value.getDistance()))
                    .set(OPTICAL_ANNOTATION + "cost", Double.toString(value.getDistance()))
                    .set(OPTICAL_ANNOTATION + "adminWeight", Double.toString(value.getAdminWeight()))
                    .set(OPTICAL_ANNOTATION + "wavelengthNum", Integer.toString(value.getWavelengthNumber()))
                    .build();

            DefaultLinkDescription linkDescription =
                    new DefaultLinkDescription(srcPoint,
                                                 snkPoint,
                                                 Link.Type.OPTICAL,
                                                 extendedAttributes);

            linkProviderService.linkDetected(linkDescription);
            log.info(String.format("WDM link: %s : %s",
                    linkDescription.src().toString(), linkDescription.dst().toString()));


            DefaultLinkDescription linkDescriptionReverse =
                    new DefaultLinkDescription(snkPoint,
                                                 srcPoint,
                                                 Link.Type.OPTICAL,
                                                 extendedAttributes);

            linkProviderService.linkDetected(linkDescriptionReverse);
            log.info(String.format("WDM link: %s : %s",
                    linkDescriptionReverse.src().toString(), linkDescriptionReverse.dst().toString()));
        }

        // Discover the packet optical link objects
        Iterator<PktOptLink> iterPktOptlink = RAW_PKTOPTLINKS.iterator();
        while (iterPktOptlink.hasNext()) {
            PktOptLink value = iterPktOptlink.next();
            DeviceId srcNodeId = deviceId("of:" + value.getSrcNodeId().replace(":", ""));
            DeviceId snkNodeId = deviceId("of:" + value.getSnkNodeId().replace(":", ""));

            PortNumber srcPort = PortNumber.portNumber(value.getSrcPort());
            PortNumber snkPort = PortNumber.portNumber(value.getSnkPort());

            ConnectPoint srcPoint = new ConnectPoint(srcNodeId, srcPort);
            ConnectPoint snkPoint = new ConnectPoint(snkNodeId, snkPort);

            DefaultAnnotations extendedAttributes = DefaultAnnotations.builder()
                    .set(OPTICAL_ANNOTATION + "linkType", "PktOptLink")
                    .set(OPTICAL_ANNOTATION + "bandwidth", Double.toString(value.getBandwidth()))
                    .set(OPTICAL_ANNOTATION + "cost", Double.toString(value.getBandwidth()))
                    .set(OPTICAL_ANNOTATION + "adminWeight", Double.toString(value.getAdminWeight()))
                    .build();

            DefaultLinkDescription linkDescription =
                    new DefaultLinkDescription(srcPoint,
                                                 snkPoint,
                                                 Link.Type.OPTICAL,
                                                 extendedAttributes);

            linkProviderService.linkDetected(linkDescription);
            log.info(String.format("Packet-optical link: %s : %s",
                    linkDescription.src().toString(), linkDescription.dst().toString()));

            DefaultLinkDescription linkDescriptionReverse =
                    new DefaultLinkDescription(snkPoint,
                                                 srcPoint,
                                                 Link.Type.OPTICAL,
                                                 extendedAttributes);

            linkProviderService.linkDetected(linkDescriptionReverse);
            log.info(String.format("Packet-optical link: %s : %s",
                    linkDescriptionReverse.src().toString(), linkDescriptionReverse.dst().toString()));
        }

    }

    @Override
    public void triggerProbe(DeviceId deviceId) {
        // TODO We may want to consider re-reading config files and publishing them based on this event.
    }

    @Override
    public void roleChanged(DeviceId device, MastershipRole newRole) {
    }

    @Override
    public boolean isReachable(DeviceId device) {
        return false;
    }
}
