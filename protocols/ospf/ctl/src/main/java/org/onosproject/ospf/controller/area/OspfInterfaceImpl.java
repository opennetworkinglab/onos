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

package org.onosproject.ospf.controller.area;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.onlab.packet.Ip4Address;
import org.onosproject.ospf.controller.LsaWrapper;
import org.onosproject.ospf.controller.OspfArea;
import org.onosproject.ospf.controller.OspfInterface;
import org.onosproject.ospf.controller.OspfLinkTed;
import org.onosproject.ospf.controller.OspfLsa;
import org.onosproject.ospf.controller.OspfMessage;
import org.onosproject.ospf.controller.OspfNbr;
import org.onosproject.ospf.controller.OspfNeighborState;
import org.onosproject.ospf.controller.OspfPacketType;
import org.onosproject.ospf.controller.OspfRouter;
import org.onosproject.ospf.controller.TopologyForDeviceAndLink;
import org.onosproject.ospf.controller.impl.Controller;
import org.onosproject.ospf.controller.impl.OspfNbrImpl;
import org.onosproject.ospf.controller.impl.TopologyForDeviceAndLinkImpl;
import org.onosproject.ospf.controller.lsdb.LsaWrapperImpl;
import org.onosproject.ospf.controller.lsdb.OspfLsdbImpl;
import org.onosproject.ospf.controller.util.OspfEligibleRouter;
import org.onosproject.ospf.controller.util.OspfInterfaceType;
import org.onosproject.ospf.protocol.lsa.LsaHeader;
import org.onosproject.ospf.protocol.lsa.OpaqueLsaHeader;
import org.onosproject.ospf.protocol.ospfpacket.OspfMessageWriter;
import org.onosproject.ospf.protocol.ospfpacket.OspfPacketHeader;
import org.onosproject.ospf.protocol.ospfpacket.subtype.LsRequestPacket;
import org.onosproject.ospf.protocol.ospfpacket.types.DdPacket;
import org.onosproject.ospf.protocol.ospfpacket.types.HelloPacket;
import org.onosproject.ospf.protocol.ospfpacket.types.LsAcknowledge;
import org.onosproject.ospf.protocol.ospfpacket.types.LsRequest;
import org.onosproject.ospf.protocol.ospfpacket.types.LsUpdate;
import org.onosproject.ospf.protocol.util.ChecksumCalculator;
import org.onosproject.ospf.protocol.util.OspfInterfaceState;
import org.onosproject.ospf.protocol.util.OspfParameters;
import org.onosproject.ospf.protocol.util.OspfUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Representation of an OSPF interface.
 */
public class OspfInterfaceImpl implements OspfInterface {
    private static final Logger log = LoggerFactory.getLogger(OspfInterfaceImpl.class);
    private int interfaceIndex;
    private Ip4Address ipAddress;
    private Ip4Address ipNetworkMask;
    private Channel channel = null;
    private int helloIntervalTime;
    private int routerDeadIntervalTime;
    private int routerPriority;
    private int interfaceType;
    private int mtu;
    private int reTransmitInterval;
    private Ip4Address dr;
    private Ip4Address bdr;
    private OspfInterfaceState state;
    private List<LsaHeader> linkStateHeaders = new ArrayList<>();
    private Map<String, OspfNbr> listOfNeighbors = new ConcurrentHashMap<>();
    private Map<String, LsaHeader> listOfNeighborMap = new ConcurrentHashMap<>();
    private long delay = 0;
    private InternalHelloTimer helloTimerTask;
    private InternalWaitTimer waitTimerTask;
    private InternalDelayedAckTimer delayedAckTimerTask;
    private ScheduledExecutorService exServiceHello;
    private ScheduledExecutorService exServiceWait;
    private ScheduledExecutorService exServiceDelayedAck;
    private boolean isDelayedAckTimerScheduled = false;
    private int delayedAckTimerInterval = 2500;
    private int interfaceTypeOldValue = 0;
    private TopologyForDeviceAndLink topologyForDeviceAndLink = new TopologyForDeviceAndLinkImpl();
    private OspfArea ospfArea;
    private Controller controller;


    /**
     * Gets the interface state.
     *
     * @return interfaceState state of the interface
     */
    public OspfInterfaceState state() {
        return state;
    }

    /**
     * Sets the interface state.
     *
     * @param ospfInterfaceState interface state enum instance
     */
    public void setState(OspfInterfaceState ospfInterfaceState) {
        this.state = ospfInterfaceState;
    }

    /**
     * Sets the netty channel.
     *
     * @param channel channel
     */
    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    /**
     * Returns OSPF area instance.
     *
     * @return OSPF area instance
     */
    public OspfArea ospfArea() {
        return ospfArea;
    }

    /**
     * Sets OSPF controller instance.
     *
     * @param controller OSPF controller instance
     */
    public void setController(Controller controller) {
        this.controller = controller;
    }

    /**
     * Sets OSPF area instance.
     *
     * @param ospfArea OSPF area instance
     */
    public void setOspfArea(OspfArea ospfArea) {
        this.ospfArea = ospfArea;
    }

    /**
     * Gets interface state.
     *
     * @return interface state
     */
    public String interfaceState() {
        return state.interfaceState();
    }

    /**
     * Gets link state headers.
     *
     * @return get the list of lsa headers
     */
    public List<LsaHeader> linkStateHeaders() {
        Set<String> key = listOfNeighborMap.keySet();
        for (String keys : key) {
            LsaHeader lsaHeader = listOfNeighborMap.get(keys);
            linkStateHeaders.add(lsaHeader);
        }
        return linkStateHeaders;
    }

    /**
     * Gets IP network mask.
     *
     * @return network mask
     */
    public Ip4Address ipNetworkMask() {
        return ipNetworkMask;
    }

    /**
     * Sets IP network mask.
     *
     * @param ipNetworkMask network mask
     */
    @Override
    public void setIpNetworkMask(Ip4Address ipNetworkMask) {
        this.ipNetworkMask = ipNetworkMask;
    }

    /**
     * Adds neighboring router to list.
     *
     * @param ospfNbr ospfNbr instance
     */
    public void addNeighbouringRouter(OspfNbr ospfNbr) {
        listOfNeighbors.put(ospfNbr.neighborId().toString(), ospfNbr);
    }

    /**
     * Gets the neighbour details from listOfNeighbors map.
     *
     * @param neighborId neighbors id
     * @return ospfNbr neighbor instance
     */
    public OspfNbr neighbouringRouter(String neighborId) {
        return listOfNeighbors.get(neighborId);
    }

    /**
     * Removes all the neighbors.
     */
    public void removeNeighbors() {
        Set<String> neighbors = listOfNeighbors.keySet();
        for (String neighborId : neighbors) {
            removeNeighbor(listOfNeighbors.get(neighborId));
            log.debug("Neighbor removed - {}", neighborId);
        }
        listOfNeighbors.clear();
    }

    /**
     * Removes neighbor from the interface neighbor map.
     *
     * @param ospfNeighbor OSPF neighbor instance
     */
    public void removeNeighbor(OspfNbr ospfNeighbor) {
        log.debug("Neighbor removed - {}", ospfNeighbor.neighborId());
        ospfNeighbor.stopInactivityTimeCheck();
        ospfNeighbor.stopFloodingTimer();
        ospfNeighbor.stopRxMtDdTimer();
        ospfNeighbor.stopRxMtLsrTimer();

        listOfNeighbors.remove(ospfNeighbor.neighborId());
    }


    /**
     * Adds LSA header to map.
     *
     * @param lsaHeader LSA header instance
     */
    public void addLsaHeaderForDelayAck(LsaHeader lsaHeader) {
        String key = lsaHeader.lsType() + "-" + lsaHeader.linkStateId() + "-" +
                lsaHeader.advertisingRouter();
        if (lsaHeader.lsType() == OspfParameters.LINK_LOCAL_OPAQUE_LSA ||
                lsaHeader.lsType() == OspfParameters.AREA_LOCAL_OPAQUE_LSA ||
                lsaHeader.lsType() == OspfParameters.AS_OPAQUE_LSA) {
            OpaqueLsaHeader header = (OpaqueLsaHeader) lsaHeader;
            key = lsaHeader.lsType() + "-" + header.opaqueType() + header.opaqueId()
                    + "-" + lsaHeader.advertisingRouter();
        }

        log.debug("Adding LSA key {} for delayed Ack", key);
        listOfNeighborMap.put(key, lsaHeader);
    }

    /**
     * Removes LSA header from map.
     *
     * @param lsaKey key used to store LSA in map
     */
    public void removeLsaFromNeighborMap(String lsaKey) {
        listOfNeighborMap.remove(lsaKey);
    }

    /**
     * Checks neighbor is in the list or not.
     *
     * @param neighborId neighbors id
     * @return true if neighbor in list else false
     */
    public boolean isNeighborInList(String neighborId) {
        return listOfNeighbors.containsKey(neighborId);
    }

    /**
     * Gets the list of neighbors.
     *
     * @return listOfNeighbors as key value pair
     */
    public Map<String, OspfNbr> listOfNeighbors() {
        return listOfNeighbors;
    }

    /**
     * Sets the list of neighbors.
     *
     * @param listOfNeighbors as key value pair
     */
    public void setListOfNeighbors(HashMap<String, OspfNbr> listOfNeighbors) {
        this.listOfNeighbors = listOfNeighbors;
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
     * Gets the IP address.
     *
     * @return IP address
     */
    public Ip4Address ipAddress() {
        return ipAddress;
    }

    /**
     * Sets the interface IP address.
     *
     * @param ipAddress interface IP address
     */
    public void setIpAddress(Ip4Address ipAddress) {
        this.ipAddress = ipAddress;
    }

    /**
     * Gets router priority.
     *
     * @return routerPriority value
     */
    public int routerPriority() {
        return routerPriority;
    }

    /**
     * Sets router priority.
     *
     * @param routerPriority value
     */
    public void setRouterPriority(int routerPriority) {
        this.routerPriority = routerPriority;
    }

    /**
     * Gets hello interval time.
     *
     * @return hello interval time
     */
    public int helloIntervalTime() {
        return helloIntervalTime;
    }

    /**
     * Sets hello interval time.
     *
     * @param helloIntervalTime an integer interval time
     */
    public void setHelloIntervalTime(int helloIntervalTime) {
        this.helloIntervalTime = helloIntervalTime;
    }

    /**
     * Gets router dead interval time.
     *
     * @return router dead interval time
     */
    public int routerDeadIntervalTime() {
        return routerDeadIntervalTime;
    }

    /**
     * Sets router dead interval time.
     *
     * @param routerDeadIntervalTime router dead interval time
     */
    public void setRouterDeadIntervalTime(int routerDeadIntervalTime) {
        this.routerDeadIntervalTime = routerDeadIntervalTime;
    }

    /**
     * Gets interface type.
     *
     * @return interfaceType an integer represents interface type
     */
    public int interfaceType() {
        return interfaceType;
    }

    /**
     * Sets interface type.
     *
     * @param interfaceType interface type
     */
    public void setInterfaceType(int interfaceType) {
        this.interfaceType = interfaceType;
    }

    /**
     * Gets max transfer unit.
     *
     * @return mtu an integer represents max transfer unit
     */
    public int mtu() {
        return mtu;
    }

    /**
     * Sets max transfer unit.
     *
     * @param mtu max transfer unit
     */
    public void setMtu(int mtu) {
        this.mtu = mtu;
    }

    /**
     * Gets retransmit interval.
     *
     * @return retransmit interval
     */
    public int reTransmitInterval() {
        return reTransmitInterval;
    }

    /**
     * Sets retransmit interval.
     *
     * @param reTransmitInterval retransmit interval
     */
    public void setReTransmitInterval(int reTransmitInterval) {
        this.reTransmitInterval = reTransmitInterval;
    }

    /**
     * Gets designated routers IP address.
     *
     * @return dr designated routers IP address
     */
    public Ip4Address dr() {
        return dr;
    }

    /**
     * Sets designated routers IP address.
     *
     * @param dr designated routers IP address
     */
    public void setDr(Ip4Address dr) {
        this.dr = dr;
    }

    /**
     * Gets backup designated routers IP address.
     *
     * @return bdr backup designated routers IP address
     */
    public Ip4Address bdr() {
        return bdr;
    }

    /**
     * Sets backup designated routers IP address.
     *
     * @param bdr backup designated routers IP address
     */
    public void setBdr(Ip4Address bdr) {
        this.bdr = bdr;
    }

    /**
     * Represents an interface is up and connected.
     *
     * @throws Exception might throws exception
     */
    public void interfaceUp() throws Exception {
        log.debug("OSPFInterfaceChannelHandler::interfaceUp...!!!");
        if (interfaceType() == OspfInterfaceType.POINT_TO_POINT.value()) {
            setState(OspfInterfaceState.POINT2POINT);
            interfaceTypeOldValue = interfaceType();
            log.debug("OSPFInterfaceChannelHandler::InterfaceType {} state {} ",
                      interfaceType(), state());
        } else if (interfaceType() == OspfInterfaceType.BROADCAST.value()) {
            //if router priority is 0, move the state to DROther
            interfaceTypeOldValue = interfaceType();
            if (routerPriority() == 0) {
                setState(OspfInterfaceState.DROTHER);
            } else {
                log.debug("OSPFInterfaceChannelHandler::InterfaceType {} state {} RouterPriority {}",
                          interfaceType(),
                          state(), routerPriority());
                setState(OspfInterfaceState.WAITING);
                //start wait timer - like inactivity timer with router deadInterval
                startWaitTimer();
            }
        }
        // Start hello timer with interval from config - convert seconds to milliseconds
        startHelloTimer();
        ospfArea.refreshArea(this);
    }


    /**
     * Gets called when a BDR was detected before the wait timer expired.
     *
     * @param ch channel instance
     * @throws Exception might throws exception
     */
    public void backupSeen(Channel ch) throws Exception {
        log.debug("OSPFInterfaceChannelHandler::backupSeen ");
        if (state() == OspfInterfaceState.WAITING) {
            electRouter(ch);
        }
    }

    /**
     * Gets called when no hello message received for particular period.
     *
     * @param ch channel instance
     * @throws Exception might throws exception
     */
    public void waitTimer(Channel ch) throws Exception {
        log.debug("OSPFInterfaceChannelHandler::waitTimer ");
        //According to RFC-2328 section 9.4
        if (state() == OspfInterfaceState.WAITING) {
            electRouter(ch);
        }
    }

    /**
     * Initiates DR election process.
     *
     * @param ch netty channel instance
     * @throws Exception might throws exception
     */
    public void callDrElection(Channel ch) throws Exception {
        log.debug("OSPFInterfaceChannelHandler::callDrElection ");
        //call when timer expired
        //no hello message received for particular interval
        //section 9.4
        electRouter(ch);
        interfaceTypeOldValue = interfaceType();
    }

    /**
     * Neighbor change event is triggered when the router priority gets changed.
     *
     * @throws Exception might throws exception
     */
    public void neighborChange() throws Exception {
        log.debug("OSPFInterfaceChannelHandler::neighborChange ");
        if (state() == OspfInterfaceState.DR || state() == OspfInterfaceState.BDR ||
                state() == OspfInterfaceState.DROTHER) {
            electRouter(channel);
        }
    }

    /**
     * Gets called when an interface is down.
     * All interface variables are reset, and interface timers disabled.
     * Also all neighbor connections associated with the interface are destroyed.
     */
    public void interfaceDown() {
        log.debug("OSPFInterfaceChannelHandler::interfaceDown ");
        stopHelloTimer();
        listOfNeighbors().clear();
        setState(OspfInterfaceState.DOWN);
    }

    /**
     * When an OSPF message received it is handed over to this method.
     * Based on the type of the OSPF message received it will be handed over
     * to corresponding message handler methods.
     *
     * @param ospfMessage received OSPF message
     * @param ctx         channel handler context instance.
     * @throws Exception might throws exception
     */
    public void processOspfMessage(OspfMessage ospfMessage, ChannelHandlerContext ctx) throws Exception {
        log.debug("OspfChannelHandler::processOspfMessage...!!!");

        if (!validateMessage(ospfMessage)) {
            return;
        }

        switch (ospfMessage.ospfMessageType().value()) {
            case OspfParameters.HELLO:
                processHelloMessage(ospfMessage, ctx);
                break;
            case OspfParameters.DD:
                processDdMessage(ospfMessage, ctx);
                break;
            case OspfParameters.LSREQUEST:
                processLsRequestMessage(ospfMessage, ctx);
                break;
            case OspfParameters.LSUPDATE:
                processLsUpdateMessage(ospfMessage, ctx);
                break;
            case OspfParameters.LSACK:
                processLsAckMessage(ospfMessage, ctx);
                break;
            default:
                log.debug("Unknown packet to process...!!!");
                break;
        }
    }

    /**
     * Validates the OSPF message received.
     *
     * @param ospfMessage OSPF message.
     * @return true if it is a valid else false.
     * @throws Exception might throws exception
     */
    private boolean validateMessage(OspfMessage ospfMessage) throws Exception {
        boolean isValid = true;
        OspfPacketHeader header = (OspfPacketHeader) ospfMessage;

        //added the check to eliminate self origin packets also two interfaces on same router.
        if (!header.sourceIp().equals(ipAddress()) && !header.routerId().equals(
                ospfArea.routerId())) {
            //Verify the checksum
            ChecksumCalculator checksum = new ChecksumCalculator();
            if (!checksum.isValidOspfCheckSum(ospfMessage, OspfUtil.OSPFPACKET_CHECKSUM_POS1,
                                              OspfUtil.OSPFPACKET_CHECKSUM_POS2)) {
                log.debug("Checksum mismatch. Received packet type {} ", ospfMessage.ospfMessageType());
                return false;
            }
            if (((OspfPacketHeader) ospfMessage).ospfVersion() != OspfUtil.OSPF_VERSION_2) {
                log.debug("Received osfpMessage Version should match with Interface Version ");
                return false;
            }
            if (!((OspfPacketHeader) ospfMessage).areaId().equals(ospfArea.areaId())) {
                log.debug("Received ospf packets are from different area than our Area ID. " +
                                  "Received Area ID {}, Our AreaId {} ",
                          ((OspfPacketHeader) ospfMessage).areaId(), ospfArea.areaId());
                return false;
            }

            //According to RFC-2328 (8.2)
            /**
             * ABR should receive packets from backbone 0.0.0.0 as we are not acting as ABR
             * we are rejecting the packet.
             */
            if (((OspfPacketHeader) ospfMessage).areaId().equals(Ip4Address.valueOf("0.0.0.0"))) {
                log.debug("ABR should receive packets from backbone 0.0.0.0 as we are not acting as " +
                                  "ABR we are rejecting the ospf packet");
                return false;
            }
            if (interfaceType() == OspfInterfaceType.BROADCAST.value() &&
                    !OspfUtil.sameNetwork(((OspfPacketHeader) ospfMessage).sourceIp(),
                                          ipAddress(), ipNetworkMask())) {
                log.debug("Received packets from different subnets. Discarding...!!!");
                return false;
            }
        } else {
            isValid = false;
        }

        return isValid;
    }

    /**
     * Processes Hello message.
     *
     * @param ospfMessage OSPF message instance.
     * @param ctx         context instance.
     * @throws Exception might throws exception
     */
    void processHelloMessage(OspfMessage ospfMessage, ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.getChannel();
        log.debug("OspfChannelHandler::processHelloMessage...!!!");
        HelloPacket helloPacket = (HelloPacket) ospfMessage;

        // processing of hello packet as per RFC 2328 section 10.5
        log.debug("OspfChannelHandler::processHelloMessage::Interface Type {} OSPFInterfaceState {} ",
                  interfaceType(), state());

        if (interfaceType() != OspfInterfaceType.POINT_TO_POINT.value()) {
            if (!helloPacket.networkMask().equals(ipNetworkMask())) {
                log.debug("OspfChannelHandler::processHelloMessage::Hello Packet Received does not " +
                                  "match the same network mask as the configure Interface");
                return;
            }
        }
        if (helloPacket.helloInterval() != helloIntervalTime()) {
            log.debug("OspfChannelHandler::processHelloMessage::Hello Packet Received have the same " +
                              "hello interval as configured Interface");
            return;
        }
        if (helloPacket.routerDeadInterval() != routerDeadIntervalTime()) {
            log.debug("OspfChannelHandler::processHelloMessage::Hello Packet Received have the same " +
                              "Router Dead interval as configured Interface");
            return;
        }

        if (interfaceType == OspfInterfaceType.POINT_TO_POINT.value() &&
                !helloPacket.dr().equals(OspfUtil.DEFAULTIP)) {
            log.debug("OspfChannelHandler::processHelloMessage:: Neighbor in broadcast network");
            return;
        }

        if (interfaceType == OspfInterfaceType.POINT_TO_POINT.value()) {
            // to verify if the neighbor which sent the hello is present in the OSPF Interface neighboring list .
            OspfNbr nbr;
            if (!isNeighborInList(helloPacket.routerId().toString())) {
                nbr = new OspfNbrImpl(ospfArea, this, helloPacket.sourceIp(),
                                      helloPacket.routerId(), helloPacket.options(), topologyForDeviceAndLink);
                addNeighbouringRouter(nbr);
            } else {
                nbr = neighbouringRouter(helloPacket.routerId().toString());
                nbr.setRouterPriority(helloPacket.routerPriority());
            }
            if (!helloPacket.containsNeighbour(ospfArea.routerId())) {
                ((OspfNbrImpl) nbr).oneWayReceived(helloPacket, channel);
            } else {
                ((OspfNbrImpl) nbr).twoWayReceived(helloPacket, ctx.getChannel());
            }
        } else if (interfaceType == OspfInterfaceType.BROADCAST.value()) {

            if (state() == OspfInterfaceState.WAITING) {
                if ((!helloPacket.dr().equals(Ip4Address.valueOf("0.0.0.0"))) &&
                        (!helloPacket.bdr().equals(Ip4Address.valueOf("0.0.0.0")))) {
                    stopWaitTimer();
                    setDr(helloPacket.dr());
                    setBdr(helloPacket.bdr());
                    if (helloPacket.dr().equals(ipAddress())) {
                        setState(OspfInterfaceState.DR);
                        //refresh router Lsa
                        ospfArea.refreshArea(this);
                    } else if (helloPacket.bdr().equals(ipAddress())) {
                        setState(OspfInterfaceState.BDR);
                        //refresh router Lsa
                        ospfArea.refreshArea(this);
                    } else {
                        setState(OspfInterfaceState.DROTHER);
                        ospfArea.refreshArea(this);
                    }

                } else if (!helloPacket.dr().equals(Ip4Address.valueOf("0.0.0.0")) ||
                        !helloPacket.bdr().equals(Ip4Address.valueOf("0.0.0.0"))) {
                    setDr(helloPacket.dr());
                    setBdr(helloPacket.bdr());
                }
                Ip4Address sourceIp = helloPacket.sourceIp();
                OspfNbr nbr;
                if (!isNeighborInList(helloPacket.routerId().toString())) {
                    nbr = new OspfNbrImpl(ospfArea, this, sourceIp, helloPacket.routerId(),
                                          helloPacket.options(), topologyForDeviceAndLink);
                    nbr.setNeighborId(helloPacket.routerId());
                    nbr.setNeighborBdr(helloPacket.bdr());
                    nbr.setNeighborDr(helloPacket.dr());
                    nbr.setRouterPriority(helloPacket.routerPriority());
                    addNeighbouringRouter(nbr);
                } else {
                    nbr = neighbouringRouter(helloPacket.routerId().toString());
                    nbr.setRouterPriority(helloPacket.routerPriority());
                }
                if (!helloPacket.containsNeighbour(ospfArea.routerId())) {
                    ((OspfNbrImpl) nbr).oneWayReceived(helloPacket, channel);
                } else {
                    ((OspfNbrImpl) nbr).twoWayReceived(helloPacket, ctx.getChannel());
                }

                if (helloPacket.dr().equals(sourceIp)) {
                    if (helloPacket.bdr().equals(Ip4Address.valueOf("0.0.0.0"))) {
                        // call backup seen
                        stopWaitTimer();
                        backupSeen(ctx.getChannel());
                    }
                }

                if (helloPacket.bdr().equals(sourceIp)) {
                    // call backup seen
                    stopWaitTimer();
                    backupSeen(ctx.getChannel());
                }
            } else {

                if ((!helloPacket.dr().equals(Ip4Address.valueOf("0.0.0.0")) ||
                        !helloPacket.bdr().equals(Ip4Address.valueOf("0.0.0.0")))
                        && routerPriority() == 0) {
                    setDr(helloPacket.dr());
                    setBdr(helloPacket.bdr());
                }
                //To verify if the neighbor which sent the hello is present in the OSPF Interface neighboring list .
                Ip4Address sourceIp = helloPacket.sourceIp();
                OspfNbr nbr;
                if (!isNeighborInList(helloPacket.routerId().toString())) {
                    nbr = new OspfNbrImpl(ospfArea, this, sourceIp, helloPacket.routerId(),
                                          helloPacket.options(), topologyForDeviceAndLink);
                    nbr.setNeighborId(helloPacket.routerId());
                    nbr.setNeighborBdr(helloPacket.bdr());
                    nbr.setNeighborDr(helloPacket.dr());
                    nbr.setRouterPriority(helloPacket.routerPriority());
                    addNeighbouringRouter(nbr);
                    ((OspfNbrImpl) nbr).oneWayReceived(helloPacket, channel);
                } else {
                    log.debug("OspfChannelHandler::NeighborInList::helloPacket.bdr(): {}, " +
                                      "helloPacket.dr(): {}", helloPacket.bdr(), helloPacket.dr());
                    nbr = neighbouringRouter(helloPacket.routerId().toString());
                    nbr.setRouterPriority(helloPacket.routerPriority());
                    if (!helloPacket.containsNeighbour(ospfArea.routerId())) {
                        ((OspfNbrImpl) nbr).oneWayReceived(helloPacket, channel);
                    } else {
                        ((OspfNbrImpl) nbr).twoWayReceived(helloPacket, ctx.getChannel());
                    }
                    if (nbr.routerPriority() != helloPacket.routerPriority()) {
                        nbr.setNeighborBdr(helloPacket.bdr());
                        nbr.setNeighborDr(helloPacket.dr());
                        neighborChange();
                    }


                    if (nbr.neighborIpAddr().equals(helloPacket.dr()) &&
                            !(nbr.neighborIpAddr().equals(nbr.neighborDr()))) {
                        nbr.setNeighborBdr(helloPacket.bdr());
                        nbr.setNeighborDr(helloPacket.dr());
                        neighborChange();
                    }

                    if (!(nbr.neighborIpAddr().equals(helloPacket.dr())) &&
                            (nbr.neighborIpAddr().equals(nbr.neighborDr()))) {
                        nbr.setNeighborBdr(helloPacket.bdr());
                        nbr.setNeighborDr(helloPacket.dr());
                        neighborChange();
                    }

                    if (nbr.neighborIpAddr().equals(helloPacket.bdr()) &&
                            !(nbr.neighborIpAddr().equals(nbr.neighborBdr()))) {
                        nbr.setNeighborBdr(helloPacket.bdr());
                        nbr.setNeighborDr(helloPacket.dr());
                        neighborChange();
                    }

                    if (!(nbr.neighborIpAddr().equals(helloPacket.bdr())) &&
                            (nbr.neighborIpAddr().equals(nbr.neighborBdr()))) {
                        nbr.setNeighborBdr(helloPacket.bdr());
                        nbr.setNeighborDr(helloPacket.dr());
                        neighborChange();
                    }

                    nbr.setNeighborBdr(helloPacket.bdr());
                    nbr.setNeighborDr(helloPacket.dr());
                }
            }
        }
    }

    /**
     * process the DD message which received.
     *
     * @param ospfMessage OSPF message instance.
     * @param ctx         channel handler context instance
     * @throws Exception might throws exception
     */
    void processDdMessage(OspfMessage ospfMessage, ChannelHandlerContext ctx) throws Exception {
        log.debug("OspfChannelHandler::processDdMessage...!!!");
        Channel channel = ctx.getChannel();
        DdPacket ddPacket = (DdPacket) ospfMessage;
        log.debug("Got DD packet from {}", ddPacket.sourceIp());
        //check it is present in listOfNeighbors
        Ip4Address neighbourId = ddPacket.routerId();
        OspfNbr nbr = neighbouringRouter(neighbourId.toString());

        if (nbr != null) {
            log.debug("OspfChannelHandler::processDdMessage:: OSPFNeighborState {}", nbr.getState());
            // set options for the NBR
            nbr.setIsOpaqueCapable(ddPacket.isOpaqueCapable());
            if (ddPacket.imtu() > mtu()) {
                log.debug("the MTU size is greater than the interface MTU");
                return;
            }
            if (nbr.getState() == OspfNeighborState.DOWN) {
                return;
            }
            if (nbr.getState() == OspfNeighborState.ATTEMPT) {
                return;
            }
            if (nbr.getState() == OspfNeighborState.TWOWAY) {
                nbr.adjOk(channel);
                return;
            }
            //if init is the state call twoWayReceived
            if (nbr.getState() == OspfNeighborState.INIT) {
                ((OspfNbrImpl) nbr).twoWayReceived(ddPacket, ctx.getChannel());
            } else if (nbr.getState() == OspfNeighborState.EXSTART) {
                //get I,M,MS Bits
                int initialize = ddPacket.isInitialize();
                int more = ddPacket.isMore();
                int masterOrSlave = ddPacket.isMaster();
                int options = ddPacket.options();
                nbr.setOptions(options);

                if (initialize == OspfUtil.INITIALIZE_SET && more == OspfUtil.MORE_SET &&
                        masterOrSlave == OspfUtil.IS_MASTER) {
                    if (ddPacket.getLsaHeaderList().isEmpty()) {
                        if (OspfUtil.ipAddressToLong(ddPacket.routerId().toString()) >
                                OspfUtil.ipAddressToLong(ospfArea.routerId().toString())) {
                            nbr.setIsMaster(OspfUtil.IS_MASTER);
                            ((OspfNbrImpl) nbr).setLastDdPacket(ddPacket);
                            nbr.setDdSeqNum(ddPacket.sequenceNo());
                            nbr.setOptions(ddPacket.options());
                            ((OspfNbrImpl) nbr).negotiationDone(ddPacket, true, ddPacket.getLsaHeaderList(),
                                                                ctx.getChannel());
                        }
                    }
                }
                if (initialize == OspfUtil.INITIALIZE_NOTSET && masterOrSlave == OspfUtil.NOT_MASTER) {
                    if (nbr.ddSeqNum() == ddPacket.sequenceNo()) {
                        if (OspfUtil.ipAddressToLong(ddPacket.routerId().toString()) <
                                OspfUtil.ipAddressToLong(ospfArea.routerId().toString())) {
                            ((OspfNbrImpl) nbr).setLastDdPacket(ddPacket);
                            nbr.setOptions(ddPacket.options());
                            nbr.setDdSeqNum(nbr.ddSeqNum() + 1);
                            ((OspfNbrImpl) nbr).negotiationDone(ddPacket, false, ddPacket.getLsaHeaderList(),
                                                                ctx.getChannel());
                        }
                    }
                }

            } else if (nbr.getState() == OspfNeighborState.EXCHANGE) {
                //get I,M,MS Bits
                log.debug("Neighbor state:: EXCHANGE");
                boolean isDuplicateDDPacket = compareDdPackets(ddPacket, ((OspfNbrImpl) nbr).lastDdPacket());
                int initialize = ddPacket.isInitialize();
                int more = ddPacket.isMore();
                int masterOrSlave = ddPacket.isMaster();
                int options = ddPacket.options();

                if (!isDuplicateDDPacket) {
                    //if dd packet is not duplicate  then continue
                    if (nbr.isMaster() != masterOrSlave) {
                        DdPacket newResPacket =
                                (DdPacket) ((OspfNbrImpl) nbr).seqNumMismatch("Master/Slave Inconsistency");
                        newResPacket.setDestinationIp(ddPacket.sourceIp());
                        log.debug("Sending back DDPacket to {}", ddPacket.sourceIp());
                        byte[] messageToWrite = getMessage(newResPacket);
                        ctx.getChannel().write(messageToWrite);
                    } else if (initialize == 1) {
                        DdPacket newResPacket =
                                (DdPacket) ((OspfNbrImpl) nbr).seqNumMismatch("Initialize bit inconsistency");
                        newResPacket.setDestinationIp(ddPacket.sourceIp());
                        log.debug("Sending back DDPacket to {}", ddPacket.sourceIp());
                        byte[] messageToWrite = getMessage(newResPacket);
                        ctx.getChannel().write(messageToWrite);
                    } else {

                        if (masterOrSlave == OspfUtil.NOT_MASTER) {
                            if (ddPacket.sequenceNo() == nbr.ddSeqNum()) {
                                //Process the DD Packet
                                ((OspfNbrImpl) nbr).processDdPacket(false, ddPacket, ctx.getChannel());
                                log.debug("Received DD Packet");
                            } else {
                                DdPacket newResPacket =
                                        (DdPacket) ((OspfNbrImpl) nbr).seqNumMismatch("Sequence Number Mismatch");
                                newResPacket.setDestinationIp(ddPacket.sourceIp());
                                log.debug("Sending back DDPacket to {}", ddPacket.sourceIp());
                                byte[] messageToWrite = getMessage(newResPacket);
                                ctx.getChannel().write(messageToWrite);
                            }
                        } else {
                            //we are the slave
                            if (ddPacket.sequenceNo() == (nbr.ddSeqNum() + 1)) {
                                ((OspfNbrImpl) nbr).setLastDdPacket(ddPacket);
                                ((OspfNbrImpl) nbr).processDdPacket(true, ddPacket, ctx.getChannel());
                                log.debug("Process DD Packet");
                            } else {
                                DdPacket newResPacket =
                                        (DdPacket) ((OspfNbrImpl) nbr).seqNumMismatch("options inconsistency");
                                newResPacket.setDestinationIp(ddPacket.sourceIp());
                                log.debug("Sending back DDPacket to {}", ddPacket.sourceIp());
                                byte[] messageToWrite = getMessage(newResPacket);
                                ctx.getChannel().write(messageToWrite);
                            }
                        }
                    }
                } else {
                    if (masterOrSlave == OspfUtil.NOT_MASTER) {
                        return;
                    } else {
                        DdPacket newResPacket = ((OspfNbrImpl) nbr).lastSentDdPacket();
                        log.debug("Sending back DDPacket to {}", ddPacket.sourceIp());
                        byte[] messageToWrite = getMessage(newResPacket);
                        ctx.getChannel().write(messageToWrite);
                    }
                }
            } else if (nbr.getState() == OspfNeighborState.LOADING || nbr.getState() == OspfNeighborState.FULL) {
                //In case if we are slave then we have to send the last received DD Packet
                int options = ddPacket.options();
                if (nbr.options() != options) {
                    OspfMessage newResPacket = ((OspfNbrImpl) nbr).seqNumMismatch("Initialize bit inconsistency");
                    newResPacket.setDestinationIp(ddPacket.sourceIp());
                    byte[] messageToWrite = getMessage(newResPacket);
                    ctx.getChannel().write(messageToWrite);
                } else if (ddPacket.isInitialize() == OspfUtil.INITIALIZE_SET) {
                    OspfMessage newResPacket = ((OspfNbrImpl) nbr).seqNumMismatch("Initialize bit inconsistency");
                    newResPacket.setDestinationIp(ddPacket.sourceIp());
                    byte[] messageToWrite = getMessage(newResPacket);
                    ctx.getChannel().write(messageToWrite);
                }
                boolean isDuplicate = compareDdPackets(ddPacket, ((OspfNbrImpl) nbr).lastDdPacket());
                //we are master
                if (nbr.isMaster() != OspfUtil.IS_MASTER) {
                    // check if the packet is duplicate, duplicates should be discarded by the master
                    if (isDuplicate) {
                        log.debug("received a duplicate DD packet");
                    }
                } else {
                    //The slave must respond to duplicates by repeating the last Database Description packet
                    //that it had sent.
                    if (isDuplicate) {
                        ddPacket.setDestinationIp(ddPacket.sourceIp());
                        byte[] messageToWrite = getMessage(((OspfNbrImpl) nbr).lastSentDdPacket());
                        ctx.getChannel().write(messageToWrite);
                        log.debug("Sending back the duplicate packet ");
                    }
                }
            }
        }
    }

    /**
     * Process the Ls Request message.
     *
     * @param ospfMessage OSPF message instance.
     * @param ctx         channel handler context instance.
     * @throws Exception might throws exception
     */
    void processLsRequestMessage(OspfMessage ospfMessage, ChannelHandlerContext ctx) throws Exception {
        log.debug("OspfChannelHandler::processLsRequestMessage...!!!");
        Channel channel = ctx.getChannel();
        LsRequest lsrPacket = (LsRequest) ospfMessage;
        OspfNbr nbr = neighbouringRouter(lsrPacket.routerId().toString());

        if (nbr.getState() == OspfNeighborState.EXCHANGE || nbr.getState() == OspfNeighborState.LOADING ||
                nbr.getState() == OspfNeighborState.FULL) {

            LsRequest reqMsg = (LsRequest) ospfMessage;
            if (reqMsg.getLinkStateRequests().isEmpty()) {
                log.debug("Received Link State Request Vector is Empty ");
                return;
            } else {
                //Send the LsUpdate back
                ListIterator<LsRequestPacket> listItr = reqMsg.getLinkStateRequests().listIterator();
                while (listItr.hasNext()) {
                    LsUpdate lsupdate = new LsUpdate();
                    lsupdate.setOspfVer(OspfUtil.OSPF_VERSION);
                    lsupdate.setOspftype(OspfPacketType.LSUPDATE.value());
                    lsupdate.setRouterId(ospfArea.routerId());
                    lsupdate.setAreaId(ospfArea.areaId());
                    lsupdate.setAuthType(OspfUtil.NOT_ASSIGNED);
                    lsupdate.setAuthentication(OspfUtil.NOT_ASSIGNED);
                    lsupdate.setOspfPacLength(OspfUtil.NOT_ASSIGNED); // to calculate packet length
                    lsupdate.setChecksum(OspfUtil.NOT_ASSIGNED);

                    //limit to mtu
                    int currentLength = OspfUtil.OSPF_HEADER_LENGTH + OspfUtil.FOUR_BYTES;
                    int maxSize = mtu() -
                            OspfUtil.LSA_HEADER_LENGTH; // subtract a normal IP header.
                    int noLsa = 0;
                    while (listItr.hasNext()) {
                        LsRequestPacket lsRequest = (LsRequestPacket) listItr.next();
                        // to verify length of the LSA
                        LsaWrapper wrapper = ospfArea.getLsa(lsRequest.lsType(), lsRequest.linkStateId(),
                                                             lsRequest.ownRouterId());
                        OspfLsa ospflsa = wrapper.ospfLsa();
                        if ((currentLength + ((LsaWrapperImpl) wrapper).lsaHeader().lsPacketLen()) >= maxSize) {
                            listItr.previous();
                            break;
                        }
                        if (ospflsa != null) {
                            lsupdate.addLsa(ospflsa);
                            noLsa++;

                            currentLength = currentLength + ((LsaWrapperImpl) wrapper).lsaHeader().lsPacketLen();
                        } else {
                            nbr.badLSReq(channel);
                        }
                    }
                    lsupdate.setNumberOfLsa(noLsa);
                    //set the destination
                    if (state() == OspfInterfaceState.DR ||
                            state() == OspfInterfaceState.BDR ||
                            state() == OspfInterfaceState.POINT2POINT) {
                        lsupdate.setDestinationIp(OspfUtil.ALL_SPF_ROUTERS);
                    } else if (state() == OspfInterfaceState.DROTHER) {
                        lsupdate.setDestinationIp(OspfUtil.ALL_DROUTERS);
                    }
                    byte[] messageToWrite = getMessage(lsupdate);
                    ctx.getChannel().write(messageToWrite);
                }
            }
        }
    }

    /**
     * Process the ls update message.
     *
     * @param ospfMessage OSPF message instance.
     * @param ctx         channel handler context instance.
     * @throws Exception might throws exception
     */
    void processLsUpdateMessage(OspfMessage ospfMessage, ChannelHandlerContext ctx) throws Exception {
        log.debug("OspfChannelHandler::processLsUpdateMessage");
        LsUpdate lsUpdate = (LsUpdate) ospfMessage;
        String neighbourId = lsUpdate.routerId().toString();
        //LSUpdate packet has been associated with a particular neighbor.
        //Neighbor should not be in lesser state than Exchange.
        if (isNeighborInList(neighbourId)) {
            OspfNbrImpl nbr = (OspfNbrImpl) neighbouringRouter(neighbourId);
            if (nbr.getState() == OspfNeighborState.EXCHANGE ||
                    nbr.getState() == OspfNeighborState.LOADING) {
                nbr.processLsUpdate(lsUpdate, ctx.getChannel());
            } else if (nbr.getState() == OspfNeighborState.FULL) {
                if (lsUpdate.noLsa() != 0) {
                    List<OspfLsa> list = lsUpdate.getLsaList();
                    Iterator itr = list.iterator();
                    while (itr.hasNext()) {
                        LsaHeader lsa = (LsaHeader) itr.next();
                        nbr.processReceivedLsa(lsa, true, ctx.getChannel(), lsUpdate.sourceIp());
                    }
                } else {
                    return;
                }
            }
        }
    }

    /**
     * Process the ls acknowledge message.
     *
     * @param ospfMessage OSPF message instance.
     * @param ctx         channel handler context instance.
     * @throws Exception might throws exception
     */
    void processLsAckMessage(OspfMessage ospfMessage, ChannelHandlerContext ctx) throws Exception {
        log.debug("OspfChannelHandler::processLsAckMessage");
        LsAcknowledge lsAckPacket = (LsAcknowledge) ospfMessage;
        //check it is present in listOfNeighbors
        OspfNbrImpl nbr = (OspfNbrImpl) neighbouringRouter(lsAckPacket.routerId().toString());
        if (nbr != null) {
            if (nbr.getState().getValue() < OspfNeighborState.EXCHANGE.getValue()) {
                // discard the packet.
                return;
            } else {
                // process ls acknowledgements
                Iterator itr = lsAckPacket.getLinkStateHeaders().iterator();
                while (itr.hasNext()) {
                    LsaHeader lsRequest = (LsaHeader) itr.next();

                    OspfLsa ospfLsa =
                            (OspfLsa) nbr.getPendingReTxList().get(((OspfAreaImpl) ospfArea).getLsaKey(lsRequest));
                    if (ospfLsa != null) {
                        String isSame = ((OspfLsdbImpl) ospfArea.database()).isNewerOrSameLsa(
                                lsRequest, (LsaHeader) ospfLsa);
                        if (isSame.equals("same")) {
                            nbr.getPendingReTxList().remove(((OspfAreaImpl) ospfArea).getLsaKey(lsRequest));
                        }
                    }
                }
            }
        }
    }

    /**
     * Compares two Dd Packets to check whether its duplicate or not.
     *
     * @param receivedDPacket received DD packet from network.
     * @param lastDdPacket    Last DdPacket which we sent.
     * @return true if it is a duplicate packet else false.
     */
    public boolean compareDdPackets(DdPacket receivedDPacket, DdPacket lastDdPacket) {
        if (receivedDPacket.isInitialize() == lastDdPacket.isInitialize()) {
            if (receivedDPacket.isMaster() == lastDdPacket.isMaster()) {
                if (receivedDPacket.isMore() == lastDdPacket.isMore()) {
                    if (receivedDPacket.options() == lastDdPacket.options()) {
                        if (receivedDPacket.sequenceNo() == lastDdPacket.sequenceNo()) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Starts the hello timer which sends hello packet every configured seconds.
     */
    public void startHelloTimer() {
        log.debug("OSPFInterfaceChannelHandler::startHelloTimer");
        exServiceHello = Executors.newSingleThreadScheduledExecutor();
        helloTimerTask = new InternalHelloTimer();
        final ScheduledFuture<?> helloHandle =
                exServiceHello.scheduleAtFixedRate(helloTimerTask, delay, helloIntervalTime, TimeUnit.SECONDS);
    }

    /**
     * Stops the hello timer.
     */
    public void stopHelloTimer() {
        log.debug("OSPFInterfaceChannelHandler::stopHelloTimer ");
        exServiceHello.shutdown();
    }

    /**
     * Starts the wait timer.
     */
    public void startWaitTimer() {
        log.debug("OSPFNbr::startWaitTimer");
        exServiceWait = Executors.newSingleThreadScheduledExecutor();
        waitTimerTask = new InternalWaitTimer();
        final ScheduledFuture<?> waitTimerHandle =
                exServiceWait.schedule(waitTimerTask, routerDeadIntervalTime(), TimeUnit.SECONDS);
    }

    /**
     * Stops the wait timer.
     */
    public void stopWaitTimer() {
        log.debug("OSPFNbr::stopWaitTimer ");
        exServiceWait.shutdown();
    }

    /**
     * Starts the timer which waits for configured seconds and sends Delayed Ack Packet.
     */
    public void startDelayedAckTimer() {
        if (!isDelayedAckTimerScheduled) {
            log.debug("Started DelayedAckTimer...!!!");
            exServiceDelayedAck = Executors.newSingleThreadScheduledExecutor();
            delayedAckTimerTask = new InternalDelayedAckTimer();
            final ScheduledFuture<?> delayAckHandle =
                    exServiceDelayedAck.scheduleAtFixedRate(delayedAckTimerTask, delayedAckTimerInterval,
                                                            delayedAckTimerInterval, TimeUnit.MILLISECONDS);
            isDelayedAckTimerScheduled = true;
        }
    }

    /**
     * Stops the delayed acknowledge timer.
     */
    public void stopDelayedAckTimer() {
        if (isDelayedAckTimerScheduled) {
            log.debug("Stopped DelayedAckTimer...!!!");
            isDelayedAckTimerScheduled = false;
            exServiceDelayedAck.shutdown();
        }
    }

    /**
     * Performs DR election.
     *
     * @param ch Netty Channel instance.
     * @throws Exception might throws exception
     */
    public void electRouter(Channel ch) throws Exception {

        Ip4Address currentDr = dr();
        Ip4Address currentBdr = bdr();
        OspfInterfaceState oldState = state();
        OspfInterfaceState newState;

        log.debug("OSPFInterfaceChannelHandler::electRouter -> currentDr: {}, currentBdr: {}",
                  currentDr, currentBdr);
        List<OspfEligibleRouter> eligibleRouters = calculateListOfEligibleRouters(new OspfEligibleRouter());

        log.debug("OSPFInterfaceChannelHandler::electRouter -> eligibleRouters: {}", eligibleRouters);
        OspfEligibleRouter electedBdr = electBdr(eligibleRouters);
        OspfEligibleRouter electedDr = electDr(eligibleRouters, electedBdr);

        setBdr(electedBdr.getIpAddress());
        setDr(electedDr.getIpAddress());

        if (electedBdr.getIpAddress().equals(ipAddress()) &&
                !electedBdr.getIpAddress().equals(currentBdr)) {
            setState(OspfInterfaceState.BDR);
        }

        if (electedDr.getIpAddress().equals(ipAddress()) &&
                !electedDr.getIpAddress().equals(currentDr)) {
            setState(OspfInterfaceState.DR);
        }

        if (state() != oldState &&
                !(state() == OspfInterfaceState.DROTHER &&
                        oldState.value() < OspfInterfaceState.DROTHER.value())) {
            log.debug("Recalculating as the State is changed ");
            log.debug("OSPFInterfaceChannelHandler::electRouter -> currentDr: {}, currentBdr: {}",
                      currentDr, currentBdr);
            eligibleRouters = calculateListOfEligibleRouters(new OspfEligibleRouter());

            log.debug("OSPFInterfaceChannelHandler::electRouter -> eligibleRouters: {}", eligibleRouters);
            electedBdr = electBdr(eligibleRouters);
            electedDr = electDr(eligibleRouters, electedBdr);

            setBdr(electedBdr.getIpAddress());
            setDr(electedDr.getIpAddress());
        }

        if (electedBdr.getIpAddress().equals(ipAddress()) &&
                !electedBdr.getIpAddress().equals(currentBdr)) {
            setState(OspfInterfaceState.BDR);
            ospfArea.refreshArea(this);
        }

        if (electedDr.getIpAddress().equals(ipAddress()) &&
                !electedDr.getIpAddress().equals(currentDr)) {
            setState(OspfInterfaceState.DR);
            //Refresh Router Lsa & Network Lsa
            ospfArea.refreshArea(this);
        }

        if (currentDr != electedDr.getIpAddress() || currentBdr != electedBdr.getIpAddress()) {
            Set<String> negibhorIdList;
            negibhorIdList = listOfNeighbors().keySet();
            for (String routerid : negibhorIdList) {
                OspfNbrImpl nbr = (OspfNbrImpl) neighbouringRouter(routerid);
                if (nbr.getState().getValue() >= OspfNeighborState.TWOWAY.getValue()) {
                    nbr.adjOk(ch);
                }
            }
        }

        log.debug("OSPFInterfaceChannelHandler::electRouter -> ElectedDR: {}, ElectedBDR: {}",
                  electedDr.getIpAddress(), electedBdr.getIpAddress());
    }


    /**
     * BDR Election process. Find the list of eligible router to participate in the process.
     *
     * @param electedDr router elected as DR.
     * @return list of eligible routers
     */
    public List<OspfEligibleRouter> calculateListOfEligibleRouters(OspfEligibleRouter electedDr) {
        log.debug("OSPFNbr::calculateListOfEligibleRouters ");
        Set<String> neighborIdList;
        List<OspfEligibleRouter> eligibleRouters = new ArrayList<>();

        neighborIdList = listOfNeighbors().keySet();
        for (String routerId : neighborIdList) {
            OspfNbrImpl nbr = (OspfNbrImpl) neighbouringRouter(routerId);
            if (nbr.getState().getValue() >= OspfNeighborState.TWOWAY.getValue() &&
                    nbr.routerPriority() > 0) {
                OspfEligibleRouter router = new OspfEligibleRouter();
                router.setIpAddress(nbr.neighborIpAddr());
                router.setRouterId(nbr.neighborId());
                router.setRouterPriority(nbr.routerPriority());
                if (nbr.neighborDr().equals(nbr.neighborIpAddr()) ||
                        electedDr.getIpAddress().equals(nbr.neighborIpAddr())) {
                    router.setIsDr(true);
                } else if (nbr.neighborBdr().equals(nbr.neighborIpAddr())) {
                    router.setIsBdr(true);
                }
                eligibleRouters.add(router);
            }
        }
        // interface does not have states like two and all
        if (routerPriority() > 0) {
            OspfEligibleRouter router = new OspfEligibleRouter();
            router.setIpAddress(ipAddress());
            router.setRouterId(ospfArea.routerId());
            router.setRouterPriority(routerPriority());
            if (dr().equals(ipAddress()) ||
                    electedDr.getIpAddress().equals(ipAddress())) {
                router.setIsDr(true);
            } else if (bdr().equals(ipAddress()) &&
                    !dr().equals(ipAddress())) {
                router.setIsBdr(true);
            }

            eligibleRouters.add(router);
        }

        return eligibleRouters;
    }

    /**
     * Based on router priority assigns BDR.
     *
     * @param eligibleRouters list of routers to participate in bdr election.
     * @return OSPF Eligible router instance.
     */
    public OspfEligibleRouter electBdr(List<OspfEligibleRouter> eligibleRouters) {
        log.debug("OSPFInterfaceChannelHandler::electBdr -> eligibleRouters: {}", eligibleRouters);
        List<OspfEligibleRouter> declaredAsBdr = new ArrayList<>();
        List<OspfEligibleRouter> notDrAndBdr = new ArrayList<>();
        for (OspfEligibleRouter router : eligibleRouters) {
            if (router.isBdr()) {
                declaredAsBdr.add(router);
            }
            if (!router.isBdr() && !router.isDr()) {
                notDrAndBdr.add(router);
            }
        }

        OspfEligibleRouter electedBdr = new OspfEligibleRouter();
        if (!declaredAsBdr.isEmpty()) {
            if (declaredAsBdr.size() == 1) {
                electedBdr = declaredAsBdr.get(0);
            } else if (declaredAsBdr.size() > 1) {
                electedBdr = selectRouterBasedOnPriority(declaredAsBdr);
            }
        } else {
            if (notDrAndBdr.size() == 1) {
                electedBdr = notDrAndBdr.get(0);
            } else if (notDrAndBdr.size() > 1) {
                electedBdr = selectRouterBasedOnPriority(notDrAndBdr);
            }
        }

        electedBdr.setIsBdr(true);
        electedBdr.setIsDr(false);

        return electedBdr;
    }

    /**
     * DR Election process.
     *
     * @param eligibleRouters list of eligible routers.
     * @param electedBdr      Elected Bdr, OSPF eligible router instance.
     * @return OSPF eligible router instance.
     */
    public OspfEligibleRouter electDr(List<OspfEligibleRouter> eligibleRouters,
                                      OspfEligibleRouter electedBdr) {

        List<OspfEligibleRouter> declaredAsDr = new ArrayList<>();
        for (OspfEligibleRouter router : eligibleRouters) {
            if (router.isDr()) {
                declaredAsDr.add(router);
            }
        }

        OspfEligibleRouter electedDr = new OspfEligibleRouter();
        if (!declaredAsDr.isEmpty()) {
            if (declaredAsDr.size() == 1) {
                electedDr = declaredAsDr.get(0);
            } else if (eligibleRouters.size() > 1) {
                electedDr = selectRouterBasedOnPriority(declaredAsDr);
            }
        } else {
            electedDr = electedBdr;
            electedDr.setIsDr(true);
            electedDr.setIsBdr(false);
        }

        return electedDr;
    }

    /**
     * DR election process.
     *
     * @param routersList list of eligible routers.
     * @return OSPF eligible router instance.
     */
    public OspfEligibleRouter selectRouterBasedOnPriority(List<OspfEligibleRouter> routersList) {

        OspfEligibleRouter initialRouter = routersList.get(0);

        for (int i = 1; i < routersList.size(); i++) {
            OspfEligibleRouter router = routersList.get(i);
            if (router.getRouterPriority() > initialRouter.getRouterPriority()) {
                initialRouter = router;
            } else if (router.getRouterPriority() == initialRouter.getRouterPriority()) {
                try {
                    if (OspfUtil.ipAddressToLong(router.getIpAddress().toString()) >
                            OspfUtil.ipAddressToLong(initialRouter.getIpAddress().toString())) {
                        initialRouter = router;
                    }
                } catch (Exception e) {
                    log.debug("OSPFInterfaceChannelHandler::selectRouterBasedOnPriority ->" +
                                      " eligibleRouters: {}", initialRouter);
                }
            }
        }

        return initialRouter;
    }

    /**
     * Adds device information.
     *
     * @param ospfRouter OSPF router instance
     */
    public void addDeviceInformation(OspfRouter ospfRouter) {
        controller.addDeviceDetails(ospfRouter);
    }

    /**
     * removes device information.
     *
     * @param ospfRouter OSPF neighbor instance
     */
    public void removeDeviceInformation(OspfRouter ospfRouter) {
        controller.removeDeviceDetails(ospfRouter);
    }

    /**
     * Adds link information.
     *
     * @param ospfRouter  OSPF router instance
     * @param ospfLinkTed list link ted instances
     */
    public void addLinkInformation(OspfRouter ospfRouter, OspfLinkTed ospfLinkTed) {
        controller.addLinkDetails(ospfRouter, ospfLinkTed);
    }

    /**
     * Removes link information.
     *
     * @param ospfRouter  OSPF router instance
     * @param ospfLinkTed OSPF link TED instance
     */
    public void removeLinkInformation(OspfRouter ospfRouter, OspfLinkTed ospfLinkTed) {
        controller.removeLinkDetails(ospfRouter, ospfLinkTed);
    }

    /**
     * Gets message as bytes.
     *
     * @param ospfMessage OSPF message
     * @return OSPF message
     */
    private byte[] getMessage(OspfMessage ospfMessage) {
        OspfMessageWriter messageWriter = new OspfMessageWriter();
        if (state().equals(OspfInterfaceState.POINT2POINT)) {
            ospfMessage.setDestinationIp(OspfUtil.ALL_SPF_ROUTERS);
        }
        return (messageWriter.getMessage(ospfMessage, interfaceIndex, state.value()));
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OspfInterfaceImpl that = (OspfInterfaceImpl) o;
        return Objects.equal(helloIntervalTime, that.helloIntervalTime) &&
                Objects.equal(routerDeadIntervalTime, that.routerDeadIntervalTime) &&
                Objects.equal(routerPriority, that.routerPriority) &&
                Objects.equal(interfaceType, that.interfaceType) &&
                Objects.equal(mtu, that.mtu) &&
                Objects.equal(reTransmitInterval, that.reTransmitInterval) &&
                Objects.equal(ipAddress, that.ipAddress) &&
                Objects.equal(ipNetworkMask, that.ipNetworkMask) &&
                Objects.equal(listOfNeighbors, that.listOfNeighbors) &&
                Objects.equal(dr, that.dr) &&
                Objects.equal(bdr, that.bdr);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(ipAddress, ipNetworkMask, helloIntervalTime,
                                routerDeadIntervalTime, routerPriority, listOfNeighbors,
                                interfaceType, mtu, reTransmitInterval, dr, bdr);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("ipAddress", ipAddress)
                .add("routerPriority", routerPriority)
                .add("helloIntervalTime", helloIntervalTime)
                .add("routerDeadIntervalTime", routerDeadIntervalTime)
                .add("interfaceType", interfaceType)
                .add("mtu", mtu)
                .add("reTransmitInterval", reTransmitInterval)
                .add("dr", dr)
                .add("bdr", bdr)
                .toString();
    }

    /**
     * Represents a Hello task which sent a hello message every configured time interval.
     */
    private class InternalHelloTimer implements Runnable {

        /**
         * Creates an instance of Hello Timer.
         */
        InternalHelloTimer() {
        }

        @Override
        public void run() {
            if (channel != null && channel.isOpen() && channel.isConnected()) {
                if (interfaceType() == OspfInterfaceType.BROADCAST.value()) {
                    if (interfaceTypeOldValue != interfaceType()) {
                        try {
                            callDrElection(channel);
                        } catch (Exception e) {
                            log.debug("Error while calling interfaceUp {}", e.getMessage());
                        }
                    }
                } else {
                    if (interfaceTypeOldValue != interfaceType()) {
                        interfaceTypeOldValue = interfaceType();
                    }
                }
                HelloPacket hellopacket = new HelloPacket();
                //Headers
                hellopacket.setOspfVer(OspfUtil.OSPF_VERSION);
                hellopacket.setOspftype(OspfPacketType.HELLO.value());
                hellopacket.setOspfPacLength(0); //will be modified while encoding
                hellopacket.setRouterId(ospfArea.routerId());
                hellopacket.setAreaId(ospfArea.areaId());
                hellopacket.setChecksum(0); //will be modified while encoding
                hellopacket.setAuthType(OspfUtil.NOT_ASSIGNED);
                hellopacket.setAuthentication(OspfUtil.NOT_ASSIGNED);
                //Body
                hellopacket.setNetworkMask(ipNetworkMask());
                hellopacket.setOptions(ospfArea.options());
                hellopacket.setHelloInterval(helloIntervalTime());
                hellopacket.setRouterPriority(routerPriority());
                hellopacket.setRouterDeadInterval(routerDeadIntervalTime());
                hellopacket.setDr(dr());
                hellopacket.setBdr(bdr());

                Map<String, OspfNbr> listOfNeighbors = listOfNeighbors();
                Set<String> keys = listOfNeighbors.keySet();
                Iterator itr = keys.iterator();
                while (itr.hasNext()) {
                    String nbrKey = (String) itr.next();
                    OspfNbrImpl nbr = (OspfNbrImpl) listOfNeighbors.get(nbrKey);
                    if (nbr.getState() != OspfNeighborState.DOWN) {
                        hellopacket.addNeighbor(Ip4Address.valueOf(nbrKey));
                    }
                }
                // build a hello Packet
                if (channel == null || !channel.isOpen() || !channel.isConnected()) {
                    log.debug("Hello Packet not sent !!.. Channel Issue...");
                    return;
                }

                hellopacket.setDestinationIp(OspfUtil.ALL_SPF_ROUTERS);
                byte[] messageToWrite = getMessage(hellopacket);
                ChannelFuture future = channel.write(messageToWrite);
                if (future.isSuccess()) {
                    log.debug("Hello Packet successfully sent !!");
                } else {
                    future.awaitUninterruptibly();
                }

            }
        }
    }

    /**
     * Represents a Wait Timer task which waits the interface state to become WAITING.
     * It initiates DR election process.
     */
    private class InternalWaitTimer implements Runnable {
        Channel ch;

        /**
         * Creates an instance of Wait Timer.
         */
        InternalWaitTimer() {
            this.ch = channel;
        }

        @Override
        public void run() {
            log.debug("Wait timer expires...");
            if (ch != null && ch.isConnected()) {
                try {
                    waitTimer(ch);
                } catch (Exception e) {
                    log.debug("Exception at wait timer ...!!!");
                }
            }
        }
    }

    /**
     * Represents a task which sent a LS Acknowledge from the link state headers list.
     */
    private class InternalDelayedAckTimer implements Runnable {
        Channel ch;

        /**
         * Creates an instance of Delayed acknowledge timer.
         */
        InternalDelayedAckTimer() {
            this.ch = channel;
        }

        @Override
        public void run() {
            if (!linkStateHeaders().isEmpty()) {
                isDelayedAckTimerScheduled = true;
                if (ch != null && ch.isConnected()) {

                    List<LsaHeader> listOfLsaHeadersAcknowledged = new ArrayList<>();
                    List<LsaHeader> listOfLsaHeaders = linkStateHeaders();
                    log.debug("Delayed Ack, Number of Lsa's to Ack {}", listOfLsaHeaders.size());
                    Iterator itr = listOfLsaHeaders.iterator();
                    while (itr.hasNext()) {
                        LsAcknowledge ackContent = new LsAcknowledge();
                        //Setting OSPF Header
                        ackContent.setOspfVer(OspfUtil.OSPF_VERSION);
                        ackContent.setOspftype(OspfPacketType.LSAACK.value());
                        ackContent.setRouterId(ospfArea.routerId());
                        ackContent.setAreaId(ospfArea.areaId());
                        ackContent.setAuthType(OspfUtil.NOT_ASSIGNED);
                        ackContent.setAuthentication(OspfUtil.NOT_ASSIGNED);
                        ackContent.setOspfPacLength(OspfUtil.NOT_ASSIGNED);
                        ackContent.setChecksum(OspfUtil.NOT_ASSIGNED);
                        //limit to mtu
                        int currentLength = OspfUtil.OSPF_HEADER_LENGTH;
                        int maxSize = mtu() -
                                OspfUtil.LSA_HEADER_LENGTH; // subtract a normal IP header.
                        while (itr.hasNext()) {
                            if ((currentLength + OspfUtil.LSA_HEADER_LENGTH) >= maxSize) {
                                break;
                            }
                            LsaHeader lsaHeader = (LsaHeader) itr.next();
                            ackContent.addLinkStateHeader(lsaHeader);
                            currentLength = currentLength + OspfUtil.LSA_HEADER_LENGTH;
                            listOfLsaHeadersAcknowledged.add(lsaHeader);
                            log.debug("Delayed Ack, Added Lsa's to Ack {}", lsaHeader);
                        }

                        log.debug("Delayed Ack, Number of Lsa's in LsAck packet {}",
                                  ackContent.getLinkStateHeaders().size());

                        //set the destination
                        if (state() == OspfInterfaceState.DR || state() == OspfInterfaceState.BDR
                                || state() == OspfInterfaceState.POINT2POINT) {
                            ackContent.setDestinationIp(OspfUtil.ALL_SPF_ROUTERS);
                        } else if (state() == OspfInterfaceState.DROTHER) {
                            ackContent.setDestinationIp(OspfUtil.ALL_DROUTERS);
                        }
                        byte[] messageToWrite = getMessage(ackContent);
                        ch.write(messageToWrite);

                        for (LsaHeader lsa : listOfLsaHeadersAcknowledged) {
                            linkStateHeaders().remove(lsa);
                            removeLsaFromNeighborMap(((OspfAreaImpl) ospfArea).getLsaKey(lsa));
                        }
                    }
                }
            }
        }
    }
}