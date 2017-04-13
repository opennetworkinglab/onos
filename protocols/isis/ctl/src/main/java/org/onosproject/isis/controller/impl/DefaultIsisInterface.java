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
package org.onosproject.isis.controller.impl;

import org.jboss.netty.channel.Channel;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.MacAddress;
import org.onosproject.isis.controller.IsisInterface;
import org.onosproject.isis.controller.IsisInterfaceState;
import org.onosproject.isis.controller.IsisLsdb;
import org.onosproject.isis.controller.IsisMessage;
import org.onosproject.isis.controller.IsisNeighbor;
import org.onosproject.isis.controller.IsisNetworkType;
import org.onosproject.isis.controller.IsisPduType;
import org.onosproject.isis.controller.IsisRouterType;
import org.onosproject.isis.controller.LspWrapper;
import org.onosproject.isis.io.isispacket.IsisHeader;
import org.onosproject.isis.io.isispacket.pdu.Csnp;
import org.onosproject.isis.io.isispacket.pdu.HelloPdu;
import org.onosproject.isis.io.isispacket.pdu.L1L2HelloPdu;
import org.onosproject.isis.io.isispacket.pdu.LsPdu;
import org.onosproject.isis.io.isispacket.pdu.P2PHelloPdu;
import org.onosproject.isis.io.isispacket.pdu.Psnp;
import org.onosproject.isis.io.isispacket.tlv.AdjacencyStateTlv;
import org.onosproject.isis.io.isispacket.tlv.IsisTlv;
import org.onosproject.isis.io.isispacket.tlv.LspEntriesTlv;
import org.onosproject.isis.io.isispacket.tlv.LspEntry;
import org.onosproject.isis.io.isispacket.tlv.TlvHeader;
import org.onosproject.isis.io.isispacket.tlv.TlvType;
import org.onosproject.isis.io.util.IsisConstants;
import org.onosproject.isis.io.util.IsisUtil;
import org.onosproject.isis.io.util.LspGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Representation of an ISIS interface.
 */
public class DefaultIsisInterface implements IsisInterface {
    private static final Logger log = LoggerFactory.getLogger(DefaultIsisInterface.class);
    boolean flagValue = false;
    private int interfaceIndex;
    private Ip4Address interfaceIpAddress;
    private byte[] networkMask;
    private MacAddress interfaceMacAddress;
    private String intermediateSystemName;
    private String systemId;
    private String l1LanId = IsisConstants.DEFAULTLANID;
    private String l2LanId = IsisConstants.DEFAULTLANID;
    private int idLength;
    private int maxAreaAddresses;
    private int reservedPacketCircuitType;
    private IsisNetworkType networkType;
    private String areaAddress;
    private int areaLength;
    private int holdingTime;
    private int priority;
    private String circuitId;
    private int helloInterval;
    private Map<MacAddress, IsisNeighbor> neighborList = new ConcurrentHashMap<>();
    private IsisHelloPduSender isisHelloPduSender = null;
    private ScheduledExecutorService exServiceHello = null;
    private IsisInterfaceState interfaceState = IsisInterfaceState.DOWN;
    private IsisLsdb isisLsdb = null;
    private List<Ip4Address> allConfiguredInterfaceIps = null;
    private Channel channel;
    private boolean helloSenderStarted = false;

    /**
     * Returns ISIS LSDB instance.
     *
     * @return ISIS LSDB instance
     */
    public IsisLsdb isisLsdb() {
        return isisLsdb;
    }

    /**
     * Sets all configured interface IPs.
     *
     * @param allConfiguredInterfaces all configured interface IPs
     */
    public void setAllConfiguredInterfaceIps(List<Ip4Address> allConfiguredInterfaces) {
        allConfiguredInterfaceIps = allConfiguredInterfaces;
    }

    /**
     * Removes neighbor from the interface neighbor map.
     *
     * @param isisNeighbor ISIS neighbor instance
     */
    public void removeNeighbor(IsisNeighbor isisNeighbor) {
        log.debug("Neighbor removed - {}", isisNeighbor.neighborMacAddress());
        isisNeighbor.stopHoldingTimeCheck();
        isisNeighbor.stopInactivityTimeCheck();
        neighborList.remove(isisNeighbor.neighborMacAddress());
    }

    /**
     * Removes all the neighbors.
     */
    public void removeNeighbors() {
        Set<MacAddress> neighbors = neighbors();
        for (MacAddress mac : neighbors) {
            removeNeighbor(lookup(mac));
            log.debug("Neighbor removed - {}", mac);
        }
        neighborList.clear();
    }

    /**
     * Returns the ISIS neighbor instance if exists.
     *
     * @param isisNeighborMac mac address of the neighbor router
     * @return ISIS neighbor instance if exists else null
     */
    public IsisNeighbor lookup(MacAddress isisNeighborMac) {
        return neighborList.get(isisNeighborMac);
    }

    /**
     * Returns the neighbors list.
     *
     * @return neighbors list
     */
    public Set<MacAddress> neighbors() {
        return neighborList.keySet();
    }

    /**
     * Returns channel instance.
     *
     * @return channel instance
     */
    public Channel channel() {
        return channel;
    }

    /**
     * Returns interface index.
     *
     * @return interface index
     */
    public int interfaceIndex() {
        return interfaceIndex;
    }

    /**
     * Set interface index.
     *
     * @param interfaceIndex interface index
     */
    public void setInterfaceIndex(int interfaceIndex) {
        this.interfaceIndex = interfaceIndex;
    }

    /**
     * Returns the interface IP address.
     *
     * @return interface IP address
     */
    public Ip4Address interfaceIpAddress() {
        return interfaceIpAddress;
    }

    /**
     * Sets the interface IP address.
     *
     * @param interfaceIpAddress interfaceIpAddress interface IP address
     */
    public void setInterfaceIpAddress(Ip4Address interfaceIpAddress) {
        this.interfaceIpAddress = interfaceIpAddress;
    }

    /**
     * Returns the network mask.
     *
     * @return network mask
     */
    public byte[] networkMask() {
        return networkMask;
    }

    /**
     * Sets the network mask.
     *
     * @param networkMask network mask
     */
    public void setNetworkMask(byte[] networkMask) {
        this.networkMask = networkMask;
    }

    /**
     * Returns the interface mac address.
     *
     * @return interface mac address
     */
    public MacAddress getInterfaceMacAddress() {
        return interfaceMacAddress;
    }

    /**
     * Sets the interface mac address.
     *
     * @param interfaceMacAddress interface mac address
     */
    public void setInterfaceMacAddress(MacAddress interfaceMacAddress) {
        this.interfaceMacAddress = interfaceMacAddress;
    }

    /**
     * Returns intermediate system name.
     *
     * @return intermediate system name
     */
    public String intermediateSystemName() {
        return intermediateSystemName;
    }

    /**
     * Sets intermediate system name.
     *
     * @param intermediateSystemName intermediate system name
     */
    public void setIntermediateSystemName(String intermediateSystemName) {
        this.intermediateSystemName = intermediateSystemName;
    }

    /**
     * Returns system ID.
     *
     * @return system ID
     */
    public String systemId() {
        return systemId;
    }

    /**
     * Sets system ID.
     *
     * @param systemId system ID
     */
    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    /**
     * Returns LAN ID.
     *
     * @return LAN ID
     */
    public String l1LanId() {
        return l1LanId;
    }

    /**
     * Sets LAN ID.
     *
     * @param l1LanId LAN ID
     */
    public void setL1LanId(String l1LanId) {
        this.l1LanId = l1LanId;
    }

    /**
     * Returns LAN ID.
     *
     * @return LAN ID
     */
    public String l2LanId() {
        return l2LanId;
    }

    /**
     * Sets LAN ID.
     *
     * @param l2LanId LAN ID
     */
    public void setL2LanId(String l2LanId) {
        this.l2LanId = l2LanId;
    }

    /**
     * Returns ID length.
     *
     * @return ID length
     */
    public int getIdLength() {

        return ((idLength == 0) ? 6 : idLength);
    }

    /**
     * Sets ID length.
     *
     * @param idLength ID length
     */
    public void setIdLength(int idLength) {
        this.idLength = idLength;
    }

    /**
     * Returns max area addresses.
     *
     * @return max area addresses
     */
    public int getMaxAreaAddresses() {

        return maxAreaAddresses;
    }

    /**
     * Sets area max addresses.
     *
     * @param maxAreaAddresses max area addresses
     */
    public void setMaxAreaAddresses(int maxAreaAddresses) {
        this.maxAreaAddresses = maxAreaAddresses;
    }

    /**
     * Returns reserved packet circuit type.
     *
     * @return reserved packet circuit type
     */
    public int reservedPacketCircuitType() {
        return reservedPacketCircuitType;
    }

    /**
     * Sets reserved packet circuit type.
     *
     * @param reservedPacketCircuitType reserved packet circuit type
     */
    public void setReservedPacketCircuitType(int reservedPacketCircuitType) {
        this.reservedPacketCircuitType = reservedPacketCircuitType;
    }

    /**
     * Returns point to point.
     *
     * @return point to point
     */
    public IsisNetworkType networkType() {
        return networkType;
    }

    /**
     * Sets point to point or broadcast.
     *
     * @param networkType point to point or broadcast
     */
    public void setNetworkType(IsisNetworkType networkType) {
        this.networkType = networkType;
    }

    /**
     * Returns area address.
     *
     * @return area address
     */
    public String areaAddress() {
        return areaAddress;
    }

    /**
     * Sets area address.
     *
     * @param areaAddress area address
     */
    public void setAreaAddress(String areaAddress) {
        this.areaAddress = areaAddress;
    }

    /**
     * Returns area length.
     *
     * @return area length
     */
    public int getAreaLength() {
        return areaLength;
    }

    /**
     * Sets area length.
     *
     * @param areaLength area length
     */
    public void setAreaLength(int areaLength) {
        this.areaLength = areaLength;
    }

    /**
     * Returns holding time.
     *
     * @return holding time
     */
    public int holdingTime() {
        return holdingTime;
    }

    /**
     * Sets holding time.
     *
     * @param holdingTime holding time
     */
    public void setHoldingTime(int holdingTime) {
        this.holdingTime = holdingTime;
    }

    /**
     * Returns priority.
     *
     * @return priority
     */
    public int priority() {
        return priority;
    }

    /**
     * Sets priority.
     *
     * @param priority priority
     */
    public void setPriority(int priority) {
        this.priority = priority;
    }

    /**
     * Returns hello interval.
     *
     * @return hello interval
     */
    public int helloInterval() {
        return helloInterval;
    }

    /**
     * Sets hello interval.
     *
     * @param helloInterval hello interval
     */
    public void setHelloInterval(int helloInterval) {
        this.helloInterval = helloInterval;
    }

    /**
     * Returns the interface state.
     *
     * @return interface state
     */
    public IsisInterfaceState interfaceState() {
        return interfaceState;
    }

    /**
     * Sets the interface state.
     *
     * @param interfaceState the interface state
     */
    public void setInterfaceState(IsisInterfaceState interfaceState) {
        this.interfaceState = interfaceState;
    }

    /**
     * Returns the circuit ID.
     *
     * @return circuit ID
     */
    public String circuitId() {
        return circuitId;
    }

    /**
     * Sets the circuit ID.
     *
     * @param circuitId circuit ID
     */
    public void setCircuitId(String circuitId) {
        this.circuitId = circuitId;
    }

    /**
     * Processes received ISIS message.
     * When an ISIS message received it is handed over to this method.
     * Based on the type of the ISIS message received it will be handed over
     * to corresponding message handler methods.
     *
     * @param isisMessage received ISIS message
     * @param isisLsdb    ISIS LSDB instance
     * @param channel     channel
     */
    public void processIsisMessage(IsisMessage isisMessage, IsisLsdb isisLsdb, Channel channel) {
        log.debug("IsisInterfaceImpl::processIsisMessage...!!!");
        this.channel = channel;

        if (isisMessage.sourceMac().equals(interfaceMacAddress)) {
            log.debug("Received our own message {}...!!!", isisMessage.isisPduType());
            return;
        }

        if (isisMessage.isisPduType() == IsisPduType.P2PHELLOPDU && networkType.equals(IsisNetworkType.BROADCAST)) {
            return;
        } else if ((isisMessage.isisPduType() == IsisPduType.L1HELLOPDU ||
                isisMessage.isisPduType() == IsisPduType.L2HELLOPDU)
                && networkType.equals(IsisNetworkType.P2P)) {
            return;
        }

        if (this.isisLsdb == null) {
            this.isisLsdb = isisLsdb;
        }

        switch (isisMessage.isisPduType()) {
            case L1HELLOPDU:
            case L2HELLOPDU:
                processL1L2HelloPduMessage(isisMessage, channel);
                break;
            case P2PHELLOPDU:
                processP2pHelloPduMessage(isisMessage, channel);
                break;
            case L1LSPDU:
            case L2LSPDU:
                processLsPduMessage(isisMessage, channel);
                break;
            case L1CSNP:
            case L2CSNP:
                processCsnPduMessage(isisMessage, channel);
                break;
            case L1PSNP:
            case L2PSNP:
                processPsnPduMessage(isisMessage, channel);
                break;
            default:
                log.debug("Unknown packet to process...!!!");
                break;
        }
    }

    /**
     * Validates the received message.
     *
     * @param helloPdu ISIS message instance
     * @return true if valid ISIS message else false
     */
    public boolean validateHelloMessage(HelloPdu helloPdu) {
        boolean isValid = false;

        if ((helloPdu.circuitType() == IsisRouterType.L1.value() &&
                reservedPacketCircuitType == IsisRouterType.L2.value()) ||
                (helloPdu.circuitType() == IsisRouterType.L2.value() &&
                        reservedPacketCircuitType == IsisRouterType.L1.value())) {
            return false;
        }

        //Local InterfaceAddress TLV and compare with the IP Interface address and check if they are in same subnet
        List<Ip4Address> interfaceIpAddresses = helloPdu.interfaceIpAddresses();
        Ip4Address neighborIp = (helloPdu.interfaceIpAddresses() != null) ?
                interfaceIpAddresses.get(0) : Ip4Address.valueOf("0.0.0.0");
        if (!IsisUtil.sameNetwork(interfaceIpAddress, neighborIp, networkMask)) {
            return false;
        }

        //Verify if it's in same area, Areas which the router belongs to
        if (helloPdu.circuitType() == IsisRouterType.L1.value()) {
            List<String> areas = helloPdu.areaAddress();
            for (String area : areas) {
                if (areaAddress.equals(area)) {
                    isValid = true;
                }
            }
        } else if (helloPdu.circuitType() == IsisRouterType.L2.value() ||
                helloPdu.circuitType() == IsisRouterType.L1L2.value()) {
            isValid = true;
        }

        return isValid;
    }

    /**
     * Checks neighbor presents in the list or not.
     *
     * @param neighborMac neighbor MAc address
     * @return true if neighbor exist else false
     */

    private boolean isNeighborInList(MacAddress neighborMac) {
        return neighborList.containsKey(neighborMac);
    }

    /**
     * Adds neighbor in the list.
     *
     * @param neighbor neighbor MAC address
     * @return true if neighbor exist else false
     */
    private void addNeighbouringRouter(IsisNeighbor neighbor) {
        neighborList.put(neighbor.neighborMacAddress(), neighbor);
    }

    /**
     * Returns neighbor presents in the list.
     *
     * @param neighborMac neighbor MAc address
     * @return neighbor instance
     */
    private IsisNeighbor neighbouringRouter(MacAddress neighborMac) {
        return neighborList.get(neighborMac);
    }

    /**
     * Processes the L1 or L2 hello message.
     *
     * @param isisMessage hello message instance
     * @param channel     channel instance
     */
    public void processL1L2HelloPduMessage(IsisMessage isisMessage, Channel channel) {
        log.debug("Enters processL1L2HelloPduMessage ...!!!");
        log.debug("IsisInterfaceImpl::processHelloMessage...!!!");

        L1L2HelloPdu helloPacket = (L1L2HelloPdu) isisMessage;
        log.debug("IsisInterfaceImpl::processHelloMessage::Interface Type {} ISISInterfaceState {} ",
                  networkType, interfaceState);

        //If validate the area, network and max address
        if (!validateHelloMessage(helloPacket)) {
            return;
        }

        //Get the neighbor
        IsisNeighbor neighbor = neighbouringRouter(isisMessage.sourceMac());
        //Neighbor is not in list
        if (!isNeighborInList(isisMessage.sourceMac())) {
            neighbor = new DefaultIsisNeighbor(helloPacket, this);
            addNeighbouringRouter(neighbor);
        }

        neighbor.setHoldingTime(helloPacket.holdingTime());
        neighbor.stopInactivityTimeCheck();
        neighbor.startInactivityTimeCheck();

        //Assign the DIS
        String lanId = helloPacket.lanId();

        if (IsisPduType.L1HELLOPDU == helloPacket.isisPduType()) {
            buildUpdateAndSendSelfGeneratedLspIfDisChange(l1LanId, lanId, channel,
                                                          IsisRouterType.get(helloPacket.circuitType()));
            l1LanId = lanId;
            neighbor.setL1LanId(lanId);
            //if a change in lanid
        } else if (IsisPduType.L2HELLOPDU == helloPacket.isisPduType()) {
            buildUpdateAndSendSelfGeneratedLspIfDisChange(l2LanId, lanId, channel,
                                                          IsisRouterType.get(helloPacket.circuitType()));
            l2LanId = lanId;
            neighbor.setL2LanId(lanId);
        }

        //Check in neighbors list our MAC address present
        List<MacAddress> neighbors = helloPacket.neighborList();
        if (neighbors != null) {
            for (MacAddress macAddress : neighbors) {
                if (interfaceMacAddress.equals(macAddress)) {
                    neighbor.setNeighborState(IsisInterfaceState.UP);
                    //Build Self LSP add in LSDB and sent it.
                    buildStoreAndSendSelfGeneratedLspIfNotExistInDb(channel,
                                                                    IsisRouterType.get(helloPacket.circuitType()));
                    break;
                }
            }
        }
    }

    /**
     * Builds and store and send self generated LSP.
     *
     * @param channel netty channel instance
     */
    private void buildStoreAndSendSelfGeneratedLspIfNotExistInDb(Channel channel, IsisRouterType neighborRouterType) {
        this.channel = channel;
        //Check our LSP is present in DB. else create a self LSP and store it and sent it
        String lspKey = isisLsdb.lspKey(systemId);
        LspWrapper wrapper = null;
        if (reservedPacketCircuitType == IsisRouterType.L1.value()) {
            wrapper = isisLsdb.findLsp(IsisPduType.L1LSPDU, lspKey);
            if (wrapper == null) {
                LsPdu lsp = new LspGenerator().getLsp(this, lspKey, IsisPduType.L1LSPDU, allConfiguredInterfaceIps);
                isisLsdb.addLsp(lsp, true, this);
                sendLsp(lsp, channel);
            }
        } else if (reservedPacketCircuitType == IsisRouterType.L2.value()) {
            wrapper = isisLsdb.findLsp(IsisPduType.L2LSPDU, lspKey);
            if (wrapper == null) {
                LsPdu lsp = new LspGenerator().getLsp(this, lspKey, IsisPduType.L2LSPDU, allConfiguredInterfaceIps);
                isisLsdb.addLsp(lsp, true, this);
                sendLsp(lsp, channel);
            }
        } else if (reservedPacketCircuitType == IsisRouterType.L1L2.value()) {
            if ((neighborRouterType == IsisRouterType.L1 || neighborRouterType == IsisRouterType.L1L2)) {

                wrapper = isisLsdb.findLsp(IsisPduType.L1LSPDU, lspKey);
                if (wrapper == null) {
                    LsPdu lsp = new LspGenerator().getLsp(this, lspKey, IsisPduType.L1LSPDU,
                                                          allConfiguredInterfaceIps);
                    isisLsdb.addLsp(lsp, true, this);
                    sendLsp(lsp, channel);
                }
            }

            if ((neighborRouterType == IsisRouterType.L2 || neighborRouterType == IsisRouterType.L1L2)) {
                wrapper = isisLsdb.findLsp(IsisPduType.L2LSPDU, lspKey);
                if (wrapper == null) {
                    LsPdu lsp = new LspGenerator().getLsp(this, lspKey, IsisPduType.L2LSPDU,
                                                          allConfiguredInterfaceIps);
                    isisLsdb.addLsp(lsp, true, this);
                    sendLsp(lsp, channel);
                }
            }
        }
    }

    /**
     * Builds and update in DB and send self generated LSP.
     *
     * @param previousLanId previous DIS ID
     * @param latestLanId   latest DIS ID
     * @param channel       netty channel instance
     */
    private void buildUpdateAndSendSelfGeneratedLspIfDisChange(String previousLanId,
                                                               String latestLanId, Channel channel,
                                                               IsisRouterType neighborRouterType) {
        this.channel = channel;
        //If DIS change then build and sent LSP
        if (!previousLanId.equals(latestLanId)) {
            //Create a self LSP and Update it in DB and sent it
            String lspKey = isisLsdb.lspKey(systemId);
            if (reservedPacketCircuitType == IsisRouterType.L1.value()) {
                LsPdu lsp = new LspGenerator().getLsp(this, lspKey, IsisPduType.L1LSPDU, allConfiguredInterfaceIps);
                isisLsdb.addLsp(lsp, true, this);
                sendLsp(lsp, channel);
            } else if (reservedPacketCircuitType == IsisRouterType.L2.value() &&
                    (neighborRouterType == IsisRouterType.L2 || neighborRouterType == IsisRouterType.L1L2)) {
                LsPdu lsp = new LspGenerator().getLsp(this, lspKey, IsisPduType.L2LSPDU, allConfiguredInterfaceIps);
                isisLsdb.addLsp(lsp, true, this);
                sendLsp(lsp, channel);
            } else if (reservedPacketCircuitType == IsisRouterType.L1L2.value()) {
                //L1 LSPDU
                if (neighborRouterType == IsisRouterType.L1 || neighborRouterType == IsisRouterType.L1L2) {
                    LsPdu lsp = new LspGenerator().getLsp(this, lspKey, IsisPduType.L1LSPDU, allConfiguredInterfaceIps);
                    isisLsdb.addLsp(lsp, true, this);
                    sendLsp(lsp, channel);
                }
                //L1 LSPDU
                if (neighborRouterType == IsisRouterType.L2 || neighborRouterType == IsisRouterType.L1L2) {
                    LsPdu lsp = new LspGenerator().getLsp(this, lspKey, IsisPduType.L2LSPDU, allConfiguredInterfaceIps);
                    isisLsdb.addLsp(lsp, true, this);
                    sendLsp(lsp, channel);
                }
            }
        }

    }

    /**
     * Sends LS PDU message to channel.
     *
     * @param lsp     LS PDU message instance
     * @param channel channel instance
     */
    private void sendLsp(LsPdu lsp, Channel channel) {
        byte[] lspBytes = lsp.asBytes();
        lspBytes = IsisUtil.addLengthAndMarkItInReserved(lspBytes, IsisConstants.LENGTHPOSITION,
                                                         IsisConstants.LENGTHPOSITION + 1,
                                                         IsisConstants.RESERVEDPOSITION);
        lspBytes = IsisUtil.addChecksum(lspBytes, IsisConstants.CHECKSUMPOSITION,
                                        IsisConstants.CHECKSUMPOSITION + 1);
        //write to the channel
        if (channel != null && channel.isConnected() && channel.isOpen()) {
            channel.write(IsisUtil.framePacket(lspBytes, interfaceIndex));
        }
    }

    /**
     * Processes P2P hello message.
     *
     * @param isisMessage hello message instance
     * @param channel     channel instance
     */
    public void processP2pHelloPduMessage(IsisMessage isisMessage, Channel channel) {
        log.debug("Enters processP2pHelloPduMessage ...!!!");
        P2PHelloPdu helloPacket = (P2PHelloPdu) isisMessage;

        log.debug("IsisInterfaceImpl::processHelloMessage::Interface Type {} OSPFInterfaceState {} ",
                  networkType, interfaceState);

        //validate the area, network and max address
        if (!validateHelloMessage(helloPacket)) {
            return;
        }

        IsisNeighbor neighbor = null;
        List<IsisTlv> tlvs = ((P2PHelloPdu) isisMessage).tlvs();
        AdjacencyStateTlv stateTlv = null;
        for (IsisTlv tlv : tlvs) {
            if (tlv instanceof AdjacencyStateTlv) {
                stateTlv = (AdjacencyStateTlv) tlv;
                break;
            }
        }

        if (stateTlv == null) {
            neighbor = neighbouringRouter(isisMessage.sourceMac());
            if (neighbor == null) {
                neighbor = new DefaultIsisNeighbor(helloPacket, this);
                addNeighbouringRouter(neighbor);
            }
            neighbor.setNeighborState(IsisInterfaceState.DOWN);
            buildStoreAndSendSelfGeneratedLspIfNotExistInDb(channel, IsisRouterType.get(helloPacket.circuitType()));
        } else if (stateTlv.adjacencyType() == IsisInterfaceState.DOWN.value()) {
            neighbor = neighbouringRouter(isisMessage.sourceMac());
            if (neighbor == null) {
                neighbor = new DefaultIsisNeighbor(helloPacket, this);
                addNeighbouringRouter(neighbor);
            }
            neighbor.setLocalExtendedCircuitId(stateTlv.localCircuitId());
        } else if (stateTlv.adjacencyType() == IsisInterfaceState.INITIAL.value()) {
            //Neighbor already present in the list
            neighbor = neighbouringRouter(isisMessage.sourceMac());
            if (neighbor == null) {
                neighbor = new DefaultIsisNeighbor(helloPacket, this);
                addNeighbouringRouter(neighbor);
            }
            neighbor.setNeighborState(IsisInterfaceState.INITIAL);
            neighbor.setLocalExtendedCircuitId(stateTlv.localCircuitId());
            //interfaceState = IsisInterfaceState.UP;
        } else if (stateTlv.adjacencyType() == IsisInterfaceState.UP.value()) {
            //Build Self LSP add in LSDB and sent it.
            neighbor = neighbouringRouter(isisMessage.sourceMac());
            neighbor.setNeighborState(IsisInterfaceState.UP);
            neighbor.setLocalExtendedCircuitId(stateTlv.localCircuitId());
            buildStoreAndSendSelfGeneratedLspIfNotExistInDb(channel, IsisRouterType.get(helloPacket.circuitType()));
        }
        if (neighbor == null) {
            log.debug("neighbor object is null!!!!");
            return;
        }

        neighbor.setHoldingTime(helloPacket.holdingTime());
        neighbor.stopInactivityTimeCheck();
        neighbor.startInactivityTimeCheck();
    }

    /**
     * Processes LS PDU message.
     *
     * @param isisMessage LS pdu message instance
     * @param channel     channel instance
     */
    public void processLsPduMessage(IsisMessage isisMessage, Channel channel) {
        log.debug("Enters processLsPduMessage ...!!!");
        IsisNeighbor neighbor = neighbouringRouter(isisMessage.sourceMac());
        if (networkType == IsisNetworkType.BROADCAST && neighbor == null) {
            return;
        }

        LsPdu lsPdu = (LsPdu) isisMessage;
        LspWrapper wrapper = isisLsdb.findLsp(lsPdu.isisPduType(), lsPdu.lspId());
        if (wrapper == null || isisLsdb.isNewerOrSameLsp(lsPdu, wrapper.lsPdu()).equalsIgnoreCase("latest")) {
            if (wrapper != null) {               // verify if the LSA - is your own LSA - get system ID and compare LSP
                String lspKey = isisLsdb.lspKey(systemId);
                if (lsPdu.lspId().equals(lspKey)) {
                    lsPdu.setSequenceNumber(lsPdu.sequenceNumber() + 1);
                    if (lsPdu.pduType() == IsisPduType.L1LSPDU.value()) {
                        // setting the ls sequence number
                        isisLsdb.setL1LspSeqNo(lsPdu.sequenceNumber());
                    } else if (lsPdu.pduType() == IsisPduType.L2LSPDU.value()) {
                        // setting the ls sequence number
                        isisLsdb.setL2LspSeqNo(lsPdu.sequenceNumber());
                    }
                    isisLsdb.addLsp(lsPdu, true, this);
                    sendLsp(lsPdu, channel);
                } else {
                    isisLsdb.addLsp(lsPdu, false, this);
                }


            } else {
                //not exist in the database or latest, then add it in database
                isisLsdb.addLsp(lsPdu, false, this);
            }
        }

        //If network type is P2P, acknowledge with a PSNP
        if (networkType() == IsisNetworkType.P2P) {
            IsisPduType psnpType = null;
            if (IsisPduType.get(lsPdu.pduType()) == IsisPduType.L1LSPDU) {
                psnpType = IsisPduType.L1PSNP;
            } else if (IsisPduType.get(lsPdu.pduType()) == IsisPduType.L2LSPDU) {
                psnpType = IsisPduType.L2PSNP;
            }
            IsisHeader isisHeader = new LspGenerator().getHeader(psnpType);
            Psnp psnp = new Psnp(isisHeader);
            psnp.setSourceId(lspKeyP2P(this.systemId));
            TlvHeader tlvHeader = new TlvHeader();
            tlvHeader.setTlvType(TlvType.LSPENTRY.value());
            tlvHeader.setTlvLength(0);
            LspEntriesTlv lspEntriesTlv = new LspEntriesTlv(tlvHeader);
            LspEntry lspEntry = new LspEntry();
            lspEntry.setLspChecksum(lsPdu.checkSum());
            lspEntry.setLspId(lsPdu.lspId());
            lspEntry.setLspSequenceNumber(lsPdu.sequenceNumber());
            lspEntry.setRemainingTime(lsPdu.remainingLifeTime());
            lspEntriesTlv.addLspEntry(lspEntry);
            psnp.addTlv(lspEntriesTlv);

            //write it to channel buffer.
            byte[] psnpBytes = psnp.asBytes();
            psnpBytes = IsisUtil.addLengthAndMarkItInReserved(psnpBytes, IsisConstants.LENGTHPOSITION,
                                                              IsisConstants.LENGTHPOSITION + 1,
                                                              IsisConstants.RESERVEDPOSITION);
            flagValue = false;
            //write to the channel
            if (channel != null && channel.isConnected() && channel.isOpen()) {
                channel.write(IsisUtil.framePacket(psnpBytes, interfaceIndex));
            }
        }
    }

    /**
     * Processes PSN PDU message.
     * Checks for self originated LSP entries in PSNP message and sends the missing LSP.
     *
     * @param isisMessage PSN PDU message instance
     * @param channel     channel instance
     */
    public void processPsnPduMessage(IsisMessage isisMessage, Channel channel) {
        log.debug("Enters processPsnPduMessage ...!!!");
        //If adjacency not formed don't process.
        IsisNeighbor neighbor = neighbouringRouter(isisMessage.sourceMac());
        if (networkType == IsisNetworkType.BROADCAST && neighbor == null) {
            return;
        }

        Psnp psnPacket = (Psnp) isisMessage;
        List<IsisTlv> isisTlvs = psnPacket.getAllTlv();
        Iterator iterator = isisTlvs.iterator();
        while (iterator.hasNext()) {
            IsisTlv isisTlv = (IsisTlv) iterator.next();
            if (isisTlv instanceof LspEntriesTlv) {
                LspEntriesTlv lspEntriesTlv = (LspEntriesTlv) isisTlv;
                List<LspEntry> lspEntryList = lspEntriesTlv.lspEntry();
                Iterator lspEntryListIterator = lspEntryList.iterator();
                while (lspEntryListIterator.hasNext()) {
                    LspEntry lspEntry = (LspEntry) lspEntryListIterator.next();
                    String lspKey = lspEntry.lspId();
                    LspWrapper lspWrapper = isisLsdb.findLsp(psnPacket.isisPduType(), lspKey);
                    if (lspWrapper != null) {
                        if (lspWrapper.isSelfOriginated()) {
                            //Sent the LSP
                            sendLsp((LsPdu) lspWrapper.lsPdu(), channel);
                        }
                    }
                }
            }
        }
    }

    /**
     * Processes CSN PDU message.
     *
     * @param isisMessage CSN PDU message instance
     * @param channel     channel instance
     */
    public void processCsnPduMessage(IsisMessage isisMessage, Channel channel) {
        log.debug("Enters processCsnPduMessage ...!!!");
        IsisNeighbor neighbor = neighbouringRouter(isisMessage.sourceMac());
        if (networkType == IsisNetworkType.BROADCAST && neighbor == null) {
            return;
        }

        Csnp csnPacket = (Csnp) isisMessage;
        IsisPduType psnPduType = (IsisPduType.L2CSNP.equals(csnPacket.isisPduType())) ?
                IsisPduType.L2PSNP : IsisPduType.L1PSNP;
        IsisPduType lsPduType = (IsisPduType.L2CSNP.equals(csnPacket.isisPduType())) ?
                IsisPduType.L2LSPDU : IsisPduType.L1LSPDU;

        List<LspEntry> lspEntryRequestList = new ArrayList<>();
        boolean selfOriginatedFound = false;
        List<IsisTlv> isisTlvs = csnPacket.getAllTlv();
        Iterator iterator = isisTlvs.iterator();
        while (iterator.hasNext()) {
            IsisTlv isisTlv = (IsisTlv) iterator.next();
            if (isisTlv instanceof LspEntriesTlv) {
                LspEntriesTlv lspEntriesTlv = (LspEntriesTlv) isisTlv;
                List<LspEntry> lspEntryList = lspEntriesTlv.lspEntry();
                Iterator lspEntryListIterator = lspEntryList.iterator();
                while (lspEntryListIterator.hasNext()) {
                    LspEntry lspEntry = (LspEntry) lspEntryListIterator.next();
                    String lspKey = lspEntry.lspId();
                    LspWrapper lspWrapper = isisLsdb.findLsp(lsPduType, lspKey);
                    if (lspWrapper != null) {
                        LsPdu lsPdu = (LsPdu) lspWrapper.lsPdu();
                        if (lspWrapper.isSelfOriginated()) {
                            selfOriginatedFound = true;
                            if (lspEntry.lspSequenceNumber() < lsPdu.sequenceNumber()) {
                                sendLsp(lsPdu, channel);
                            }
                        } else {
                            if (lsPdu.sequenceNumber() < lspEntry.lspSequenceNumber()) {
                                lspEntryRequestList.add(lspEntry);
                                flagValue = true;
                            }
                        }
                    } else {
                        lspEntryRequestList.add(lspEntry);
                        flagValue = true;
                    }
                }
            }
        }
        if (flagValue) {
            sendPsnPduMessage(lspEntryRequestList, psnPduType, channel);
            lspEntryRequestList.clear();
        }

        if (!selfOriginatedFound) {
            String lspKey = isisLsdb.lspKey(systemId);
            LspWrapper wrapper = isisLsdb.findLsp(lsPduType, lspKey);
            if (wrapper != null) {
                sendLsp((LsPdu) wrapper.lsPdu(), channel);
            }
        }
    }

    /**
     * Sends the partial sequence number PDU.
     *
     * @param lspEntryRequestList list of lsp entry request
     * @param isisPduType         intermediate system PDU type
     * @param channel             netty channel instance
     */
    private void sendPsnPduMessage(List<LspEntry> lspEntryRequestList, IsisPduType isisPduType, Channel channel) {
        IsisHeader isisHeader = new LspGenerator().getHeader(isisPduType);
        Psnp psnp = new Psnp(isisHeader);
        psnp.setSourceId(lspKeyP2P(this.systemId));
        TlvHeader tlvHeader = new TlvHeader();
        tlvHeader.setTlvType(TlvType.LSPENTRY.value());
        tlvHeader.setTlvLength(0);
        LspEntriesTlv lspEntriesTlv = new LspEntriesTlv(tlvHeader);
        for (LspEntry lspEntry : lspEntryRequestList) {
            lspEntry.setLspChecksum(0);
            lspEntry.setLspSequenceNumber(0);
            lspEntry.setRemainingTime(0);
            lspEntriesTlv.addLspEntry(lspEntry);
        }
        psnp.addTlv(lspEntriesTlv);
        //write it to channel buffer.
        byte[] psnpBytes = psnp.asBytes();
        psnpBytes = IsisUtil.addLengthAndMarkItInReserved(psnpBytes, IsisConstants.LENGTHPOSITION,
                                                          IsisConstants.LENGTHPOSITION + 1,
                                                          IsisConstants.RESERVEDPOSITION);
        flagValue = false;
        //write to the channel
        if (channel != null && channel.isConnected() && channel.isOpen()) {
            channel.write(IsisUtil.framePacket(psnpBytes, interfaceIndex));
        }
    }

    /**
     * Gets the LSP key.
     *
     * @param systemId system ID
     * @return key
     */
    public String lspKeyP2P(String systemId) {
        StringBuilder lspKey = new StringBuilder();
        lspKey.append(systemId);
        lspKey.append(".00");
        return lspKey.toString();
    }

    /**
     * Starts the hello timer which sends hello packet every configured seconds.
     *
     * @param channel netty channel instance
     */
    public void startHelloSender(Channel channel) {
        log.debug("IsisInterfaceImpl::startHelloSender");
        if (!helloSenderStarted) {
            isisHelloPduSender = new IsisHelloPduSender(channel, this);
            exServiceHello = Executors.newSingleThreadScheduledExecutor();
            final ScheduledFuture<?> helloHandle =
                    exServiceHello.scheduleAtFixedRate(isisHelloPduSender, 0,
                                                       helloInterval, TimeUnit.SECONDS);
            helloSenderStarted = true;
        }
    }

    /**
     * Stops the hello timer which sends hello packet every configured seconds.
     */
    public void stopHelloSender() {
        log.debug("IsisInterfaceImpl::stopHelloSender");
        exServiceHello.shutdown();
        helloSenderStarted = false;
    }
}
