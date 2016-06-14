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

import com.fasterxml.jackson.databind.JsonNode;
import org.onlab.packet.Ip4Address;
import org.onosproject.ospf.controller.OspfArea;
import org.onosproject.ospf.controller.OspfInterface;
import org.onosproject.ospf.controller.OspfProcess;
import org.onosproject.ospf.controller.area.OspfAreaImpl;
import org.onosproject.ospf.controller.area.OspfInterfaceImpl;
import org.onosproject.ospf.controller.area.OspfProcessImpl;
import org.onosproject.ospf.protocol.util.OspfUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Representation of OSPF network configuration parsing util.
 */
public final class OspfConfigUtil {
    public static final String PROCESSID = "processId";
    public static final String AREAS = "areas";
    public static final String INTERFACEINDEX = "interfaceIndex";
    public static final String AREAID = "areaId";
    public static final String ROUTERID = "routerId";
    public static final String INTERFACE = "interface";
    public static final String HELLOINTERVAL = "helloIntervalTime";
    public static final String ROUTERDEADINTERVAL = "routerDeadIntervalTime";
    public static final String INTERFACETYPE = "interfaceType";
    public static final String EXTERNALROUTINGCAPABILITY = "externalRoutingCapability";
    protected static final Logger log = LoggerFactory.getLogger(OspfConfigUtil.class);
    private static final String ISOPAQUE = "isOpaqueEnable";

    /**
     * Creates an instance of this.
     */
    private OspfConfigUtil() {

    }

    /**
     * Returns list of OSPF process from the json nodes.
     *
     * @param jsonNodes represents one or more OSPF process configuration
     * @return list of OSPF processes.
     */
    public static List<OspfProcess> processes(JsonNode jsonNodes) {
        List<OspfProcess> ospfProcesses = new ArrayList<>();
        if (jsonNodes == null) {
            return ospfProcesses;
        }
        //From each Process nodes, get area and related interface details.
        jsonNodes.forEach(jsonNode -> {
            List<OspfArea> areas = new ArrayList<>();
            //Get configured areas for the process.
            for (JsonNode areaNode : jsonNode.path(AREAS)) {
                List<OspfInterface> interfaceList = new ArrayList<>();
                for (JsonNode interfaceNode : areaNode.path(INTERFACE)) {
                    OspfInterface ospfInterface = interfaceDetails(interfaceNode);
                    if (ospfInterface != null) {
                        interfaceList.add(ospfInterface);
                    }
                }
                //Get the area details
                OspfArea area = areaDetails(areaNode);
                if (area != null) {
                    area.setOspfInterfaceList(interfaceList);
                    areas.add(area);
                }
            }
            OspfProcess process = new OspfProcessImpl();
            process.setProcessId(jsonNode.path(PROCESSID).asText());
            process.setAreas(areas);
            ospfProcesses.add(process);
        });

        return ospfProcesses;
    }

    /**
     * Returns interface IP by index.
     *
     * @param interfaceIndex interface index
     * @return interface IP by index
     */
    private static Ip4Address getInterfaceIp(int interfaceIndex) {
        Ip4Address ipAddress = null;
        try {
            NetworkInterface networkInterface = NetworkInterface.getByIndex(interfaceIndex);
            Enumeration ipAddresses = networkInterface.getInetAddresses();
            while (ipAddresses.hasMoreElements()) {
                InetAddress address = (InetAddress) ipAddresses.nextElement();
                if (!address.isLinkLocalAddress()) {
                    ipAddress = Ip4Address.valueOf(address.getAddress());
                    break;
                }
            }
        } catch (Exception e) {
            log.debug("Error while getting Interface IP by index");
            return OspfUtil.DEFAULTIP;
        }
        return ipAddress;
    }

    /**
     * Returns interface MAC by index.
     *
     * @param interfaceIndex interface index
     * @return interface IP by index
     */
    private static String getInterfaceMask(int interfaceIndex) {
        String subnetMask = null;
        try {
            Ip4Address ipAddress = getInterfaceIp(interfaceIndex);
            NetworkInterface networkInterface = NetworkInterface.getByInetAddress(
                    InetAddress.getByName(ipAddress.toString()));
            Enumeration ipAddresses = networkInterface.getInetAddresses();
            int index = 0;
            while (ipAddresses.hasMoreElements()) {
                InetAddress address = (InetAddress) ipAddresses.nextElement();
                if (!address.isLinkLocalAddress()) {
                    break;
                }
                index++;
            }
            int prfLen = networkInterface.getInterfaceAddresses().get(index).getNetworkPrefixLength();
            int shft = 0xffffffff << (32 - prfLen);
            int oct1 = ((byte) ((shft & 0xff000000) >> 24)) & 0xff;
            int oct2 = ((byte) ((shft & 0x00ff0000) >> 16)) & 0xff;
            int oct3 = ((byte) ((shft & 0x0000ff00) >> 8)) & 0xff;
            int oct4 = ((byte) (shft & 0x000000ff)) & 0xff;
            subnetMask = oct1 + "." + oct2 + "." + oct3 + "." + oct4;
        } catch (Exception e) {
            log.debug("Error while getting Interface network mask by index");
            return subnetMask;
        }
        return subnetMask;
    }

    /**
     * Checks if valid digit or not.
     *
     * @param strInput input value
     * @return true if valid else false
     */
    private static boolean isValidDigit(String strInput) {
        boolean isValid = true;
        if (isPrimitive(strInput)) {
            int input = Integer.parseInt(strInput);
            if (input < 1 || input > 255) {
                log.debug("Wrong config input value: {}", strInput);
                isValid = false;
            } else {
                isValid = true;
            }

        } else {
            isValid = false;
        }

        return isValid;
    }

    /**
     * Checks if primitive or not.
     *
     * @param value input value
     * @return true if number else false
     */
    private static boolean isPrimitive(String value) {
        boolean status = true;
        value = value.trim();
        if (value.length() < 1) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!Character.isDigit(c)) {
                status = false;
                break;
            }
        }

        return status;
    }

    /**
     * Checks if boolean or not.
     *
     * @param value input value
     * @return true if boolean else false
     */
    private static boolean isBoolean(String value) {
        boolean status = false;
        value = value.trim();
        if (value.equals("true") || value.equals("false")) {
            return true;
        }

        return status;
    }

    /**
     * Checks if given id is valid or not.
     *
     * @param value input value
     * @return true if valid else false
     */
    private static boolean isValidIpAddress(String value) {
        boolean status = true;
        try {
            Ip4Address ipAddress = Ip4Address.valueOf(value);
        } catch (Exception e) {
            log.debug("Invalid IP address string: {}", value);
            return false;
        }

        return status;
    }

    /**
     * Returns OSPF area instance from configuration.
     *
     * @param areaNode area configuration
     * @return OSPF area instance
     */
    private static OspfArea areaDetails(JsonNode areaNode) {
        OspfArea area = new OspfAreaImpl();
        String areaId = areaNode.path(AREAID).asText();
        if (isValidIpAddress(areaId)) {
            area.setAreaId(Ip4Address.valueOf(areaId));
        } else {
            log.debug("Wrong areaId: {}", areaId);
            return null;
        }
        String routerId = areaNode.path(ROUTERID).asText();
        if (isValidIpAddress(routerId)) {
            area.setRouterId(Ip4Address.valueOf(routerId));
        } else {
            log.debug("Wrong routerId: {}", routerId);
            return null;
        }
        String routingCapability = areaNode.path(EXTERNALROUTINGCAPABILITY).asText();
        if (isBoolean(routingCapability)) {
            area.setExternalRoutingCapability(Boolean.valueOf(routingCapability));
        } else {
            log.debug("Wrong routingCapability: {}", routingCapability);
            return null;
        }
        String isOpaqueEnabled = areaNode.path(ISOPAQUE).asText();
        if (isBoolean(isOpaqueEnabled)) {
            area.setIsOpaqueEnabled(Boolean.valueOf(isOpaqueEnabled));
        } else {
            log.debug("Wrong isOpaqueEnabled: {}", isOpaqueEnabled);
            return null;
        }
        area.setOptions(OspfUtil.HELLO_PACKET_OPTIONS);

        return area;
    }

    /**
     * Returns OSPF interface instance from configuration.
     *
     * @param interfaceNode interface configuration
     * @return OSPF interface instance
     */
    private static OspfInterface interfaceDetails(JsonNode interfaceNode) {
        OspfInterface ospfInterface = new OspfInterfaceImpl();
        String index = interfaceNode.path(INTERFACEINDEX).asText();
        if (isValidDigit(index)) {
            ospfInterface.setInterfaceIndex(Integer.parseInt(index));
        } else {
            log.debug("Wrong interface index: {}", index);
            return null;
        }
        Ip4Address interfaceIp = getInterfaceIp(ospfInterface.interfaceIndex());
        if (interfaceIp.equals(OspfUtil.DEFAULTIP)) {
            return null;
        }
        ospfInterface.setIpAddress(interfaceIp);
        ospfInterface.setIpNetworkMask(Ip4Address.valueOf(getInterfaceMask(
                ospfInterface.interfaceIndex())));
        ospfInterface.setBdr(OspfUtil.DEFAULTIP);
        ospfInterface.setDr(OspfUtil.DEFAULTIP);
        String helloInterval = interfaceNode.path(HELLOINTERVAL).asText();
        if (isValidDigit(helloInterval)) {
            ospfInterface.setHelloIntervalTime(Integer.parseInt(helloInterval));
        } else {
            log.debug("Wrong hello interval: {}", helloInterval);
            return null;
        }
        String routerDeadInterval = interfaceNode.path(ROUTERDEADINTERVAL).asText();
        if (isValidDigit(routerDeadInterval)) {
            ospfInterface.setRouterDeadIntervalTime(Integer.parseInt(routerDeadInterval));
        } else {
            log.debug("Wrong routerDeadInterval: {}", routerDeadInterval);
            return null;
        }
        String interfaceType = interfaceNode.path(INTERFACETYPE).asText();
        if (isValidDigit(interfaceType)) {
            ospfInterface.setInterfaceType(Integer.parseInt(interfaceType));
        } else {
            log.debug("Wrong interfaceType: {}", interfaceType);
            return null;
        }
        ospfInterface.setReTransmitInterval(OspfUtil.RETRANSMITINTERVAL);
        ospfInterface.setMtu(OspfUtil.MTU);
        ospfInterface.setRouterPriority(OspfUtil.ROUTER_PRIORITY);

        return ospfInterface;
    }
}
