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

import org.jboss.netty.channel.Channel;
import org.onlab.packet.Ip4Address;
import org.onlab.util.Bandwidth;
import org.onosproject.ospf.controller.DeviceInformation;
import org.onosproject.ospf.controller.LinkInformation;
import org.onosproject.ospf.controller.LsaWrapper;
import org.onosproject.ospf.controller.OspfArea;
import org.onosproject.ospf.controller.OspfDeviceTed;
import org.onosproject.ospf.controller.OspfInterface;
import org.onosproject.ospf.controller.OspfLinkTed;
import org.onosproject.ospf.controller.OspfLsa;
import org.onosproject.ospf.controller.OspfLsaType;
import org.onosproject.ospf.controller.OspfLsdb;
import org.onosproject.ospf.controller.OspfMessage;
import org.onosproject.ospf.controller.OspfNbr;
import org.onosproject.ospf.controller.OspfNeighborState;
import org.onosproject.ospf.controller.OspfPacketType;
import org.onosproject.ospf.controller.OspfRouter;
import org.onosproject.ospf.controller.TopologyForDeviceAndLink;
import org.onosproject.ospf.controller.area.OspfAreaImpl;
import org.onosproject.ospf.controller.area.OspfInterfaceImpl;
import org.onosproject.ospf.controller.lsdb.LsaWrapperImpl;
import org.onosproject.ospf.controller.util.OspfInterfaceType;
import org.onosproject.ospf.protocol.lsa.LsaHeader;
import org.onosproject.ospf.protocol.lsa.OpaqueLsaHeader;
import org.onosproject.ospf.protocol.lsa.types.OpaqueLsa10;
import org.onosproject.ospf.protocol.lsa.types.TopLevelTlv;
import org.onosproject.ospf.protocol.ospfpacket.OspfMessageWriter;
import org.onosproject.ospf.protocol.ospfpacket.OspfPacketHeader;
import org.onosproject.ospf.protocol.ospfpacket.subtype.LsRequestPacket;
import org.onosproject.ospf.protocol.ospfpacket.types.DdPacket;
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
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Represents an OSPF neighbor.
 * The first thing an OSPF router must do is find its neighbors and form adjacency.
 * Each neighbor that the router finds will be represented by this class.
 */
public class OspfNbrImpl implements OspfNbr {
    private static final Logger log = LoggerFactory.getLogger(OspfNbrImpl.class);
    private OspfNeighborState state;
    private InternalRxmtDdPacket rxmtDdPacketTask;
    private InternalInactivityTimeCheck inActivityTimeCheckTask;
    private InternalFloodingTask floodingTask;
    private InternalRxmtLsrPacket rxmtLsrPacketTask;
    private ScheduledExecutorService exServiceRxmtLsr;
    private ScheduledExecutorService exServiceFlooding;
    private ScheduledExecutorService exServiceRxmtDDPacket;
    private ScheduledExecutorService exServiceInActivity;

    private boolean floodingTimerScheduled = false;
    private boolean rxmtLsrTimerScheduled = false;
    private boolean rxmtDdPacketTimerScheduled = false;
    private boolean inActivityTimerScheduled = false;

    /**
     * When the two neighbors are exchanging databases, they form a master/slave relationship.
     * The master sends the first Database Description Packet
     */
    private int isMaster;

    /**
     * The DD Sequence Number of the DD packet that is currently being sent to the neighbor.
     */
    private long ddSeqNum;

    /**
     * Another data structure for keeping information of the last received DD packet.
     */
    private DdPacket lastDdPacket;

    /**
     * Another data structure for keeping information of the last Sent DD packet.
     */
    private DdPacket lastSentDdPacket;

    /**
     * Another data structure for keeping information of the last Sent LSrequest packet.
     */
    private LsRequest lastSentLsrPacket;

    /**
     * The router ID of the Neighbor Router.
     */
    private Ip4Address neighborId;

    /**
     * The IP address of the neighboring router's interface to the attached network.
     */
    private Ip4Address neighborIpAddr;

    /**
     * The neighbor's IDEA of the designated router.
     */
    private Ip4Address neighborDr;

    /**
     * The neighbor's IDEA of the backup designated router.
     */
    private Ip4Address neighborBdr;

    private int routerPriority;
    private int routerDeadInterval;

    /**
     * The list of LSAs that have to be flooded.
     */
    private Map<String, OspfLsa> reTxList = new LinkedHashMap();

    /**
     * The list of LSAs that have been flooded but not yet acknowledged on this adjacency.
     */
    private Map<String, OspfLsa> pendingReTxList = new LinkedHashMap();

    /**
     * List of LSAs which are failed to received ACK.
     */
    private Map failedTxList = new HashMap();

    /**
     * The complete list of LSAs that make up the area link-state database, at the moment the.
     * neighbor goes into Database Exchange state (EXCHANGE).
     */
    private List<LsaHeader> ddSummaryList = new CopyOnWriteArrayList();

    /**
     * LSA Request List from Neighbor.
     */
    private Hashtable lsReqList = new Hashtable();

    /**
     * The optional OSPF capabilities supported by the neighbor.
     */
    private int options;
    private boolean isOpaqueCapable;

    /**
     * A link to the OSPF-Interface this Neighbor belongs to.
     */
    private OspfInterface ospfInterface;

    /**
     * A link to the OSPF-Area this Neighbor Data Structure belongs to.
     */
    private OspfArea ospfArea;
    private List<TopLevelTlv> topLevelTlvs = new ArrayList<>();
    private List<DeviceInformation> deviceInformationList = new ArrayList<>();

    private TopologyForDeviceAndLink topologyForDeviceAndLink;

    /**
     * Creates an instance of Neighbor.
     *
     * @param paramOspfArea                  OSPF Area instance
     * @param paramOspfInterface             OSPF interface instance
     * @param ipAddr                         IP address
     * @param routerId                       router id
     * @param options                        options
     * @param topologyForDeviceAndLinkCommon topology for device and link instance
     */
    public OspfNbrImpl(OspfArea paramOspfArea, OspfInterface paramOspfInterface,
                       Ip4Address ipAddr, Ip4Address routerId, int options,
                       TopologyForDeviceAndLink topologyForDeviceAndLinkCommon) {
        this.ospfArea = paramOspfArea;
        this.ospfInterface = paramOspfInterface;
        state = OspfNeighborState.DOWN;
        isMaster = OspfUtil.NOT_MASTER;
        ddSeqNum = OspfUtil.createRandomNumber();
        neighborIpAddr = ipAddr;
        neighborId = routerId;
        this.options = options;
        lastDdPacket = new DdPacket();
        routerDeadInterval = paramOspfInterface.routerDeadIntervalTime();
        this.topologyForDeviceAndLink = topologyForDeviceAndLinkCommon;
    }

    /**
     * Gets the IP address of this neighbor.
     *
     * @return the IP address of this neighbor
     */
    public Ip4Address neighborIpAddr() {
        return neighborIpAddr;
    }

    /**
     * Gets the neighbor is opaque enabled or not.
     *
     * @return true if the neighbor is opaque enabled else false.
     */
    public boolean isOpaqueCapable() {
        return isOpaqueCapable;
    }

    /**
     * Sets the neighbor is opaque enabled or not.
     *
     * @param isOpaqueCapable true if the neighbor is opaque enabledelse false
     */
    public void setIsOpaqueCapable(boolean isOpaqueCapable) {
        this.isOpaqueCapable = isOpaqueCapable;
    }

    /**
     * Sets router dead interval.
     *
     * @param routerDeadInterval router dead interval
     */
    public void setRouterDeadInterval(int routerDeadInterval) {
        this.routerDeadInterval = routerDeadInterval;
    }

    /**
     * Have seen a Neighbor, but the Neighbor doesn't know about me.
     *
     * @param ospfHello Hello Packet instance
     * @param channel   netty channel instance
     */
    public void oneWayReceived(OspfMessage ospfHello, Channel channel) {
        log.debug("OSPFNbr::oneWayReceived...!!!");
        stopInactivityTimeCheck();
        startInactivityTimeCheck();

        if (state == OspfNeighborState.ATTEMPT) {
            state = OspfNeighborState.INIT;
        } else if (state == OspfNeighborState.DOWN) {
            state = OspfNeighborState.INIT;
        }

        if (state.getValue() >= OspfNeighborState.TWOWAY.getValue()) {
            state = OspfNeighborState.INIT;
            failedTxList.clear();
            ddSummaryList.clear();
            lsReqList.clear();
        }
    }

    /**
     * Called when a DD OSPFMessage is received while state was INIT.
     *
     * @param ospfMessage ospf message instance
     * @param channel     netty channel instance
     * @throws Exception might throws exception
     */
    public void twoWayReceived(OspfMessage ospfMessage, Channel channel) throws Exception {
        log.debug("OSPFNbr::twoWayReceived...!!!");
        stopInactivityTimeCheck();
        startInactivityTimeCheck();
        startFloodingTimer(channel);

        OspfPacketHeader packet = (OspfPacketHeader) ospfMessage;
        if (state.getValue() <= OspfNeighborState.TWOWAY.getValue()) {
            if (formAdjacencyOrNot()) {
                state = OspfNeighborState.EXSTART;

                ddSeqNum++;
                DdPacket ddPacket = new DdPacket();
                // seting OSPF Header
                ddPacket.setOspfVer(OspfUtil.OSPF_VERSION);
                ddPacket.setOspftype(OspfPacketType.DD.value());
                ddPacket.setRouterId(ospfArea.routerId());
                ddPacket.setAreaId(ospfArea.areaId());
                ddPacket.setAuthType(OspfUtil.NOT_ASSIGNED);
                ddPacket.setAuthentication(OspfUtil.NOT_ASSIGNED);
                ddPacket.setOspfPacLength(OspfUtil.NOT_ASSIGNED); // to calculate packet length
                ddPacket.setChecksum(OspfUtil.NOT_ASSIGNED);
                boolean isOpaqueEnabled = ospfArea.isOpaqueEnabled();
                if (isOpaqueEnabled) {
                    ddPacket.setOptions(ospfArea.opaqueEnabledOptions());
                } else {
                    ddPacket.setOptions(ospfArea.options());
                }
                ddPacket.setIsInitialize(OspfUtil.INITIALIZE_SET);
                ddPacket.setIsMore(OspfUtil.MORE_SET);
                ddPacket.setIsMaster(OspfUtil.IS_MASTER);
                ddPacket.setImtu(ospfInterface.mtu());
                ddPacket.setSequenceNo(ddSeqNum);

                setLastSentDdPacket(ddPacket);
                rxmtDdPacketTask = new InternalRxmtDdPacket(channel);
                startRxMtDdTimer(channel);
                //setting destination ip
                ddPacket.setDestinationIp(packet.sourceIp());
                byte[] messageToWrite = getMessage(ddPacket);
                channel.write(messageToWrite);
            } else {
                state = OspfNeighborState.TWOWAY;
            }
        }
    }

    /**
     * Checks whether to form adjacency or not.
     *
     * @return true indicates form adjacency, else false
     */
    private boolean formAdjacencyOrNot() {
        boolean formAdjacency = false;

        if (ospfInterface.interfaceType() == OspfInterfaceType.POINT_TO_POINT.value()) {
            formAdjacency = true;
        } else if (ospfInterface.interfaceType() == OspfInterfaceType.BROADCAST.value()) {
            if (ospfInterface.ipAddress().equals(this.neighborDr) ||
                    ospfInterface.ipAddress().equals(this.neighborBdr)) {
                formAdjacency = true;
            } else if (neighborBdr.equals(neighborIpAddr) ||
                    neighborDr.equals(neighborIpAddr)) {
                formAdjacency = true;
            }
        }

        log.debug("FormAdjacencyOrNot - neighborDR: {}, neighborBDR: {}, neighborIPAddr: {}, formAdjacencyOrNot {}",
                  neighborDr, neighborBdr, neighborIpAddr, formAdjacency);

        return formAdjacency;
    }

    /**
     * At this point Master/Slave relationship is definitely established.
     * DD sequence numbers have been exchanged.
     * This is the begin of sending/receiving of DD OSPFMessages.
     *
     * @param ospfMessage      OSPF message instance
     * @param neighborIsMaster neighbor is master or slave
     * @param payload          contains the LSAs to add in Dd Packet
     * @param ch               netty channel instance
     * @throws Exception might throws exception
     */
    public void negotiationDone(OspfMessage ospfMessage,
                                boolean neighborIsMaster, List payload, Channel ch) throws Exception {
        stopRxMtDdTimer();
        OspfPacketHeader packet = (OspfPacketHeader) ospfMessage;
        DdPacket ddPacketForCheck = (DdPacket) packet;
        if (ddPacketForCheck.isOpaqueCapable()) {
            OspfLsdb database = ospfArea.database();
            List opaqueLsas = database.getAllLsaHeaders(true, true);
            Iterator iterator = opaqueLsas.iterator();
            while (iterator.hasNext()) {
                OspfLsa ospfLsa = (OspfLsa) iterator.next();
                if (ospfLsa.getOspfLsaType() == OspfLsaType.AREA_LOCAL_OPAQUE_LSA) {
                    OpaqueLsa10 opaqueLsa10 = (OpaqueLsa10) ospfLsa;
                    topLevelTlvs = opaqueLsa10.topLevelValues();
                }
            }
        }
        if (state == OspfNeighborState.EXSTART) {
            state = OspfNeighborState.EXCHANGE;
            boolean excludeMaxAgeLsa = true;
            //list of contents of area wise LSA
            ddSummaryList = (CopyOnWriteArrayList) ospfArea.getLsaHeaders(excludeMaxAgeLsa, isOpaqueCapable);

            if (neighborIsMaster) {
                processLsas(payload);
                // ...construct new DD Packet...
                DdPacket ddPacket = new DdPacket();
                // setting OSPF Header
                ddPacket.setOspfVer(OspfUtil.OSPF_VERSION);
                ddPacket.setOspftype(OspfPacketType.DD.value());
                ddPacket.setRouterId(ospfArea.routerId());
                ddPacket.setAreaId(ospfArea.areaId());
                ddPacket.setAuthType(OspfUtil.NOT_ASSIGNED);
                ddPacket.setAuthentication(OspfUtil.NOT_ASSIGNED);
                ddPacket.setOspfPacLength(OspfUtil.NOT_ASSIGNED); // to calculate packet length
                ddPacket.setChecksum(OspfUtil.NOT_ASSIGNED);
                boolean isOpaqueEnabled = ospfArea.isOpaqueEnabled();
                if (isOpaqueEnabled && isOpaqueCapable) {
                    ddPacket.setOptions(ospfArea.opaqueEnabledOptions());
                } else {
                    ddPacket.setOptions(ospfArea.options());
                }
                ddPacket.setIsInitialize(OspfUtil.INITIALIZE_NOTSET);
                ddPacket.setIsMore(OspfUtil.MORE_NOTSET);
                ddPacket.setIsMaster(OspfUtil.NOT_MASTER);
                ddPacket.setImtu(ospfInterface.mtu());
                ddPacket.setSequenceNo(ddSeqNum);
                //setting the destination
                ddPacket.setDestinationIp(packet.sourceIp());
                setLastSentDdPacket(ddPacket);
                getIsMoreBit();

                byte[] messageToWrite = getMessage(lastSentDdPacket);
                ch.write(messageToWrite);
            } else {
                // process LSA Vector's List, Add it to LSRequestList.
                processLsas(payload);
                DdPacket ddPacket = new DdPacket();
                // setting OSPF Header
                ddPacket.setOspfVer(OspfUtil.OSPF_VERSION);
                ddPacket.setOspftype(OspfPacketType.DD.value());
                ddPacket.setRouterId(ospfArea.routerId());
                ddPacket.setAreaId(ospfArea.areaId());
                ddPacket.setAuthType(OspfUtil.NOT_ASSIGNED);
                ddPacket.setAuthentication(OspfUtil.NOT_ASSIGNED);
                ddPacket.setOspfPacLength(OspfUtil.NOT_ASSIGNED); // to calculate packet length
                ddPacket.setChecksum(OspfUtil.NOT_ASSIGNED);
                boolean isOpaqueEnabled = ospfArea.isOpaqueEnabled();
                if (isOpaqueEnabled && isOpaqueCapable) {
                    ddPacket.setOptions(ospfArea.opaqueEnabledOptions());
                } else {
                    ddPacket.setOptions(ospfArea.options());
                }
                ddPacket.setIsInitialize(OspfUtil.INITIALIZE_NOTSET);
                ddPacket.setIsMore(OspfUtil.MORE_NOTSET);
                ddPacket.setIsMaster(OspfUtil.IS_MASTER);
                ddPacket.setImtu(ospfInterface.mtu());
                ddPacket.setSequenceNo(ddSeqNum);
                setLastSentDdPacket(ddPacket);
                getIsMoreBit();
                ddPacket.setDestinationIp(packet.sourceIp());
                byte[] messageToWrite = getMessage(lastSentDdPacket);
                ch.write(messageToWrite);
                startRxMtDdTimer(ch);
            }
        }
    }

    /**
     * Process the LSA Headers received in the last received Database Description OSPFMessage.
     *
     * @param ddPayload LSA headers to process
     * @throws Exception might throws exception
     */
    public void processLsas(List ddPayload) throws Exception {
        log.debug("OSPFNbr::processLsas...!!!");
        OspfLsa nextLsa;
        Iterator lsas = ddPayload.iterator();
        while (lsas.hasNext()) {
            nextLsa = (OspfLsa) lsas.next();
            // check LSA Type.
            if (((nextLsa.getOspfLsaType().value() > OspfLsaType.EXTERNAL_LSA.value()) &&
                    (nextLsa.getOspfLsaType().value() < OspfLsaType.LINK_LOCAL_OPAQUE_LSA.value())) ||
                    (nextLsa.getOspfLsaType().value() > OspfLsaType.AS_OPAQUE_LSA.value())) {
                // unknown lsType found!
                seqNumMismatch("LS Type found was unknown.");
                return;
            }

            if ((nextLsa.getOspfLsaType() == OspfLsaType.EXTERNAL_LSA) &&
                    !ospfArea.isExternalRoutingCapability()) {
                // LSA is external and the Area has no external lsa capability
                seqNumMismatch("External LSA found although area is stub.");
                return;
            }

            LsaWrapper lsaHasInstance = ospfArea.lsaLookup(nextLsa);
            if (lsaHasInstance == null) {
                lsReqList.put(((OspfAreaImpl) ospfArea).getLsaKey((LsaHeader) nextLsa), nextLsa);
            } else {
                String isNew = ((OspfAreaImpl) ospfArea).isNewerOrSameLsa(nextLsa,
                                                                          lsaHasInstance.ospfLsa());
                if (isNew.equals("latest")) {
                    lsReqList.put(((OspfAreaImpl) ospfArea).getLsaKey((LsaHeader) nextLsa), nextLsa);
                }
            }
        }
    }

    /**
     * Handles sequence number mis match event.
     *
     * @param reason a string represents the mismatch reason
     * @return OSPF message instance
     * @throws Exception might throws exception
     */
    public OspfMessage seqNumMismatch(String reason) throws Exception {
        log.debug("OSPFNbr::seqNumMismatch...{} ", reason);
        stopRxMtDdTimer();

        if (state.getValue() >= OspfNeighborState.EXCHANGE.getValue()) {
           /* if (state == OspfNeighborState.FULL) {
                ospfArea.refreshArea(ospfInterface);
            }*/

            state = OspfNeighborState.EXSTART;
            lsReqList.clear();
            ddSummaryList.clear();
            //increment the dd sequence number
            ddSeqNum++;

            DdPacket ddPacket = new DdPacket();
            // seting OSPF Header
            ddPacket.setOspfVer(OspfUtil.OSPF_VERSION);
            ddPacket.setOspftype(OspfPacketType.DD.value());
            ddPacket.setRouterId(ospfArea.routerId());
            ddPacket.setAreaId(ospfArea.areaId());
            ddPacket.setAuthType(OspfUtil.NOT_ASSIGNED);
            ddPacket.setAuthentication(OspfUtil.NOT_ASSIGNED);
            ddPacket.setOspfPacLength(OspfUtil.NOT_ASSIGNED); // to calculate packet length
            ddPacket.setChecksum(OspfUtil.NOT_ASSIGNED);
            boolean isOpaqueEnabled = ospfArea.isOpaqueEnabled();
            if (isOpaqueEnabled) {
                ddPacket.setOptions(ospfArea.opaqueEnabledOptions());
            } else {
                ddPacket.setOptions(ospfArea.options());
            }
            ddPacket.setIsInitialize(OspfUtil.INITIALIZE_SET);
            ddPacket.setIsMore(OspfUtil.MORE_SET);
            ddPacket.setIsMaster(OspfUtil.IS_MASTER);
            ddPacket.setImtu(ospfInterface.mtu());
            ddPacket.setSequenceNo(ddSeqNum);

            setLastSentDdPacket(ddPacket);
            //setting destination ip
            ddPacket.setDestinationIp(neighborIpAddr());
            setLastSentDdPacket(ddPacket);

            return ddPacket;
        }

        return null;
    }

    /**
     * Called if a LS Request has been received for an LSA which is not contained in the database.
     * This indicates an error in the Database Exchange process.
     * Actions to be performed are the same as in seqNumMismatch.
     * In addition, stop the possibly activated re transmission timer.
     *
     * @param ch netty channel instance
     * @throws Exception on error
     */
    public void badLSReq(Channel ch) throws Exception {
        log.debug("OSPFNbr::badLSReq...!!!");

        if (state.getValue() >= OspfNeighborState.EXCHANGE.getValue()) {
            if (state == OspfNeighborState.FULL) {
                ospfArea.refreshArea(ospfInterface);
            }

            stopRxMtDdTimer();
            state = OspfNeighborState.EXSTART;

            lsReqList.clear();
            ddSummaryList.clear();
            reTxList.clear();
            //increment the dd sequence number
            isMaster = OspfUtil.IS_MASTER;
            ddSeqNum++;
            DdPacket ddPacket = new DdPacket();
            // seting OSPF Header
            ddPacket.setOspfVer(OspfUtil.OSPF_VERSION);
            ddPacket.setOspftype(OspfPacketType.DD.value());
            ddPacket.setRouterId(ospfArea.routerId());
            ddPacket.setAreaId(ospfArea.areaId());
            ddPacket.setAuthType(OspfUtil.NOT_ASSIGNED);
            ddPacket.setAuthentication(OspfUtil.NOT_ASSIGNED);
            ddPacket.setOspfPacLength(OspfUtil.NOT_ASSIGNED); // to calculate packet length
            ddPacket.setChecksum(OspfUtil.NOT_ASSIGNED);

            // setting DD Body
            boolean isOpaqueEnabled = ospfArea.isOpaqueEnabled();
            if (isOpaqueEnabled && this.isOpaqueCapable) {
                ddPacket.setOptions(ospfArea.opaqueEnabledOptions());
            } else {
                ddPacket.setOptions(ospfArea.options());
            }
            ddPacket.setIsInitialize(OspfUtil.INITIALIZE_SET);
            ddPacket.setIsMore(OspfUtil.MORE_SET);
            ddPacket.setIsMaster(OspfUtil.IS_MASTER);
            ddPacket.setImtu(ospfInterface.mtu());
            ddPacket.setSequenceNo(ddSeqNum);

            rxmtDdPacketTask = new InternalRxmtDdPacket(ch);
            startRxMtDdTimer(ch);

            //setting destination ip
            ddPacket.setDestinationIp(neighborIpAddr());
            setLastSentDdPacket(ddPacket);
            byte[] messageToWrite = getMessage(ddPacket);
            ch.write(messageToWrite);
        }
    }

    /**
     * Called if state is EXCHANGE. This method is executed every time a DD Packets arrives.
     * When the last Packet arrives, it transfers the state into LOADING or FULL
     *
     * @param neighborIsMaster true if neighbor is master else false
     * @param dataDescPkt      DdPacket instance
     * @param ch               netty channel instance
     * @throws Exception might throws exception
     */
    public void processDdPacket(boolean neighborIsMaster, DdPacket dataDescPkt,
                                Channel ch) throws Exception {
        log.debug("OSPFNbr::neighborIsMaster.{}", neighborIsMaster);

        if (!neighborIsMaster) {
            stopRxMtDdTimer();
            ddSeqNum++;
            processLsas(dataDescPkt.getLsaHeaderList());
            if ((ddSummaryList.isEmpty()) &&
                    (dataDescPkt.isMore() == OspfUtil.MORE_NOTSET)) {
                log.debug(
                        "OSPFNbr::ddSummaryList is empty and dataDescPkt.isMore is zero..!!!");
                // generate the neighbor event ExchangeDone.
                exchangeDone(dataDescPkt, ch);
            } else {
                log.debug("OSPFNbr::ddSummaryList is present...!!!");
                // send a new Database Description Packet to the slave.
                DdPacket ddPacket = new DdPacket();
                // seting OSPF Header
                ddPacket.setOspfVer(OspfUtil.OSPF_VERSION);
                ddPacket.setOspftype(OspfPacketType.DD.value());
                ddPacket.setRouterId(ospfArea.routerId());
                ddPacket.setAreaId(ospfArea.areaId());
                ddPacket.setAuthType(OspfUtil.NOT_ASSIGNED);
                ddPacket.setAuthentication(OspfUtil.NOT_ASSIGNED);
                ddPacket.setOspfPacLength(OspfUtil.NOT_ASSIGNED); // to calculate packet length
                ddPacket.setChecksum(OspfUtil.NOT_ASSIGNED);
                // setting DD Body
                boolean isOpaqueEnabled = ospfArea.isOpaqueEnabled();
                if (isOpaqueEnabled && isOpaqueCapable) {
                    ddPacket.setOptions(ospfArea.opaqueEnabledOptions());
                } else {
                    ddPacket.setOptions(ospfArea.options());
                }
                ddPacket.setIsInitialize(OspfUtil.INITIALIZE_NOTSET);
                ddPacket.setIsMore(OspfUtil.MORE_NOTSET);
                ddPacket.setIsMaster(OspfUtil.IS_MASTER);
                ddPacket.setImtu(ospfInterface.mtu());
                ddPacket.setSequenceNo(ddSeqNum);

                setLastSentDdPacket(ddPacket);
                getIsMoreBit();
                //Set the destination IP Address
                ddPacket.setDestinationIp(dataDescPkt.sourceIp());
                byte[] messageToWrite = getMessage(lastSentDdPacket());
                ch.write(messageToWrite);

                startRxMtDdTimer(ch);
            }
        } else {
            log.debug("OSPFNbr::neighborIsMaster is master...!!!");
            ddSeqNum = dataDescPkt.sequenceNo();
            processLsas(dataDescPkt.getLsaHeaderList());

            DdPacket ddPacket = new DdPacket();
            // seting OSPF Header
            ddPacket.setOspfVer(OspfUtil.OSPF_VERSION);
            ddPacket.setOspftype(OspfPacketType.DD.value());
            ddPacket.setRouterId(ospfArea.routerId());
            ddPacket.setAreaId(ospfArea.areaId());
            ddPacket.setAuthType(OspfUtil.NOT_ASSIGNED);
            ddPacket.setAuthentication(OspfUtil.NOT_ASSIGNED);
            ddPacket.setOspfPacLength(OspfUtil.NOT_ASSIGNED); // to calculate packet length
            ddPacket.setChecksum(OspfUtil.NOT_ASSIGNED);
            // setting DD Body
            boolean isOpaqueEnabled = ospfArea.isOpaqueEnabled();
            if (isOpaqueEnabled && this.isOpaqueCapable) {
                ddPacket.setOptions(ospfArea.opaqueEnabledOptions());
            } else {
                ddPacket.setOptions(ospfArea.options());
            }
            ddPacket.setIsInitialize(OspfUtil.INITIALIZE_NOTSET);
            ddPacket.setIsMore(OspfUtil.MORE_NOTSET);
            ddPacket.setIsMaster(OspfUtil.NOT_MASTER);
            ddPacket.setImtu(ospfInterface.mtu());
            ddPacket.setSequenceNo(ddSeqNum);
            setLastSentDdPacket(ddPacket);
            getIsMoreBit();

            if ((ddPacket.isMore() == OspfUtil.MORE_NOTSET) &&
                    (dataDescPkt.isMore() == OspfUtil.MORE_NOTSET)) {
                // generate the neighbor event ExchangeDone.
                exchangeDone(dataDescPkt, ch);
            }

            ddPacket.setDestinationIp(dataDescPkt.sourceIp());
            byte[] messageToWrite = getMessage(ddPacket);
            ch.write(messageToWrite);
        }
    }

    /**
     * Sets the more bit in stored, last sent DdPacket.
     */
    private void getIsMoreBit() {
        DdPacket ddPacket = lastSentDdPacket();
        int count = ddSummaryList.size();

        if (!ddSummaryList.isEmpty()) {
            Iterator itr = ddSummaryList.iterator();
            int currentLength = OspfUtil.DD_HEADER_LENGTH;
            int maxSize = ospfInterface.mtu() - OspfUtil.LSA_HEADER_LENGTH; // subtract a normal IP header.
            while (itr.hasNext()) {
                if ((currentLength + OspfUtil.LSA_HEADER_LENGTH) > maxSize) {
                    break;
                }

                LsaHeader lsaHeader = (LsaHeader) itr.next();
                ddPacket.addLsaHeader(lsaHeader);
                currentLength = currentLength + OspfUtil.LSA_HEADER_LENGTH;
                ddSummaryList.remove(lsaHeader);
                count--;
            }

            if (count > 0) {
                ddPacket.setIsMore(OspfUtil.MORE_SET);
            } else {
                ddPacket.setIsMore(OspfUtil.MORE_NOTSET);
            }
        }

        setLastSentDdPacket(ddPacket);
    }

    /**
     * At this point, the router has sent and received an entire sequence of DD packets.
     * Now it must be determined whether the new state is FULL, or LS Request packets
     * have to be send.
     *
     * @param message OSPF message instance
     * @param ch      netty channel handler
     */
    public void exchangeDone(OspfMessage message, Channel ch) {
        log.debug("OSPFNbr::exchangeDone...!!!");
        stopRxMtDdTimer();

        OspfPacketHeader header = (OspfPacketHeader) message;

        if (state == OspfNeighborState.EXCHANGE) {
            if (lsReqList.isEmpty()) {
                state = OspfNeighborState.FULL;
                //handler.addDeviceInformation(this);
                //handler.addLinkInformation(this, topLevelTlvs);
            } else {
                state = OspfNeighborState.LOADING;
                LsRequest lsRequest = buildLsRequest();
                //Setting the destination address
                lsRequest.setDestinationIp(header.sourceIp());
                byte[] messageToWrite = getMessage(lsRequest);
                ch.write(messageToWrite);

                setLastSentLsrPacket(lsRequest);
                startRxMtLsrTimer(ch);
            }
        }
    }

    /**
     * Builds LS Request.
     *
     * @return ls request instance
     */
    private LsRequest buildLsRequest() {
        //send link state request packet to neighbor
        //for recent lsa's which are not received in exchange state
        LsRequest lsRequest = new LsRequest();
        lsRequest.setOspfVer(OspfUtil.OSPF_VERSION);
        lsRequest.setOspftype(OspfPacketType.LSREQUEST.value());
        lsRequest.setRouterId(ospfArea.routerId());
        lsRequest.setAreaId(ospfArea.areaId());
        lsRequest.setAuthType(OspfUtil.NOT_ASSIGNED);
        lsRequest.setAuthentication(OspfUtil.NOT_ASSIGNED);
        lsRequest.setOspfPacLength(OspfUtil.NOT_ASSIGNED); // to calculate packet length
        lsRequest.setChecksum(OspfUtil.NOT_ASSIGNED);

        Set lsaKeys = lsReqList.keySet();
        Iterator itr = lsaKeys.iterator();

        int currentLength = OspfUtil.OSPF_HEADER_LENGTH;
        int maxSize = ospfInterface.mtu() -
                OspfUtil.LSA_HEADER_LENGTH; // subtract a normal IP header.

        while (itr.hasNext()) {
            if ((currentLength + OspfUtil.LSREQUEST_LENGTH) >= maxSize) {
                break;
            }
            LsRequestPacket lsRequestPacket = new LsRequestPacket();

            String key = ((String) itr.next());
            String[] lsaKey = key.split("-");
            OspfLsa lsa = (OspfLsa) lsReqList.get(key);

            lsRequestPacket.setLsType(Integer.valueOf(lsaKey[0]));
            lsRequestPacket.setOwnRouterId(lsaKey[2]);

            if (((lsa.getOspfLsaType().value() == OspfLsaType.AREA_LOCAL_OPAQUE_LSA.value()) ||
                    (lsa.getOspfLsaType().value() == OspfLsaType.LINK_LOCAL_OPAQUE_LSA.value())) ||
                    (lsa.getOspfLsaType().value() == OspfLsaType.AS_OPAQUE_LSA.value())) {
                OpaqueLsaHeader header = (OpaqueLsaHeader) lsa;
                byte[] opaqueIdBytes = OspfUtil.convertToTwoBytes(header.opaqueId());
                lsRequestPacket.setLinkStateId(header.opaqueType() + "." + "0" + "." + opaqueIdBytes[0]
                                                       + "." + opaqueIdBytes[1]);
            } else {
                lsRequestPacket.setLinkStateId(lsaKey[1]);
            }

            lsRequest.addLinkStateRequests(lsRequestPacket);
            currentLength = currentLength + OspfUtil.LSREQUEST_LENGTH;
        }

        return lsRequest;
    }

    /**
     * Determines whether an adjacency should be established/maintained with the neighbor or not.
     *
     * @param ch netty channel instance
     */
    public void adjOk(Channel ch) {
        log.debug("OSPFNbr::adjOk...!!!");
        if (ospfInterface.interfaceType() != OspfInterfaceType.POINT_TO_POINT.value()) {
            if (state == OspfNeighborState.TWOWAY) {
                if (formAdjacencyOrNot()) {
                    state = OspfNeighborState.EXSTART;
                    //check for sequence number in lsdb
                    ddSeqNum++;

                    DdPacket ddPacket = new DdPacket();
                    // seting OSPF Header
                    ddPacket.setOspfVer(OspfUtil.OSPF_VERSION);
                    ddPacket.setOspftype(OspfPacketType.DD.value());
                    ddPacket.setRouterId(ospfArea.routerId());
                    ddPacket.setAreaId(ospfArea.areaId());
                    ddPacket.setAuthType(OspfUtil.NOT_ASSIGNED);
                    ddPacket.setAuthentication(OspfUtil.NOT_ASSIGNED);
                    ddPacket.setOspfPacLength(OspfUtil.NOT_ASSIGNED);
                    ddPacket.setChecksum(OspfUtil.NOT_ASSIGNED);

                    // setting DD Body
                    boolean isOpaqueEnabled = ospfArea.isOpaqueEnabled();
                    if (isOpaqueEnabled && this.isOpaqueCapable) {
                        ddPacket.setOptions(ospfArea.opaqueEnabledOptions());
                    } else {
                        ddPacket.setOptions(ospfArea.options());
                    }
                    ddPacket.setIsInitialize(OspfUtil.INITIALIZE_SET);
                    ddPacket.setIsMore(OspfUtil.MORE_SET);
                    ddPacket.setIsMaster(OspfUtil.IS_MASTER);
                    ddPacket.setImtu(ospfInterface.mtu());
                    ddPacket.setSequenceNo(ddSeqNum);
                    rxmtDdPacketTask = new InternalRxmtDdPacket(ch);
                    startRxMtDdTimer(ch);
                    //setting destination ip
                    ddPacket.setDestinationIp(neighborIpAddr());
                    setLastSentDdPacket(ddPacket);
                    byte[] messageToWrite = getMessage(ddPacket);
                    ch.write(messageToWrite);
                }
            } else if (state.getValue() >= OspfNeighborState.EXSTART.getValue()) {
                if (!formAdjacencyOrNot()) {
                    state = OspfNeighborState.TWOWAY;
                    lsReqList.clear();
                    ddSummaryList.clear();
                    reTxList.clear();
                }
            }
        }
    }

    /**
     * LS Update Packet has been received while state was EXCHANGE or LOADING.
     * Examine the received LSAs, check whether they were requested or not and process
     * them accordingly. Therefore use method "processReceivedLsa" for further treatment.
     *
     * @param lsUpdPkt LS Update Packet received while Neighbor state was EXCHANGE or
     *                 LOADING
     * @param ch       netty channel instance
     * @throws Exception might throws exception
     */
    public void processLsUpdate(LsUpdate lsUpdPkt, Channel ch) throws Exception {
        stopRxMtLsrTimer();
        log.debug("OSPFNbr::processLsUpdate...!!!");

        List lsaList = lsUpdPkt.getLsaList();
        if (!lsaList.isEmpty()) {
            Iterator itr = lsaList.iterator();

            while (itr.hasNext()) {
                LsaHeader lsaHeader = (LsaHeader) itr.next();
                String key = ((OspfAreaImpl) ospfArea).getLsaKey(lsaHeader);

                if (lsReqList.containsKey(key)) {
                    boolean removeIt;
                    removeIt = processReceivedLsa(lsaHeader, false, ch,
                                                  lsUpdPkt.sourceIp());
                    if (removeIt) {
                        lsReqList.remove(key);
                    }
                } else {
                    // LSA was received via Flooding
                    processReceivedLsa(lsaHeader, true, ch,
                                       lsUpdPkt.sourceIp());
                }
            }

            if (lsReqList.isEmpty() && (state == OspfNeighborState.LOADING)) {
                // loading complete
                loadingDone();
            } else {
                stopRxMtLsrTimer();
                LsRequest lsRequest = buildLsRequest();
                lsRequest.setDestinationIp(lsUpdPkt.sourceIp());
                setLastSentLsrPacket(lsRequest);

                startRxMtLsrTimer(ch);
            }
        }
    }

    /***
     * Method gets called when no more ls request list and moving to FULL State.
     *
     * @throws Exception might throws exception
     */
    public void loadingDone() throws Exception {
        stopRxMtLsrTimer();
        stopRxMtDdTimer();
        log.debug("OSPFNbr::loadingDone...!!!");
        state = OspfNeighborState.FULL;
        ospfArea.refreshArea(ospfInterface);
    }

    /**
     * Adds device and link.
     *
     * @param topologyForDeviceAndLink topology for device and link instance
     */
    private void callDeviceAndLinkAdding(TopologyForDeviceAndLink topologyForDeviceAndLink) {
        Map<String, DeviceInformation> deviceInformationMap = topologyForDeviceAndLink.deviceInformationMap();
        Map<String, DeviceInformation> deviceInformationMapForPointToPoint =
                topologyForDeviceAndLink.deviceInformationMapForPointToPoint();
        Map<String, DeviceInformation> deviceInformationMapToDelete =
                topologyForDeviceAndLink.deviceInformationMapToDelete();
        Map<String, LinkInformation> linkInformationMap = topologyForDeviceAndLink.linkInformationMap();
        Map<String, LinkInformation> linkInformationMapForPointToPoint =
                topologyForDeviceAndLink.linkInformationMapForPointToPoint();
        OspfRouter ospfRouter = new OspfRouterImpl();

        if (deviceInformationMap.size() != 0) {
            for (String key : deviceInformationMap.keySet()) {
                DeviceInformation value = deviceInformationMap.get(key);
                ospfRouter.setRouterIp(value.routerId());
                ospfRouter.setAreaIdOfInterface(ospfArea.areaId());
                ospfRouter.setNeighborRouterId(value.deviceId());
                OspfDeviceTed ospfDeviceTed = new OspfDeviceTedImpl();
                List<Ip4Address> ip4Addresses = value.interfaceId();
                ospfDeviceTed.setIpv4RouterIds(ip4Addresses);
                ospfRouter.setDeviceTed(ospfDeviceTed);
                ospfRouter.setOpaque(ospfArea.isOpaqueEnabled());
                if (value.isDr()) {
                    ospfRouter.setDr(value.isDr());
                } else {
                    ospfRouter.setDr(false);
                }
                int size = value.interfaceId().size();
                for (int i = 0; i < size; i++) {
                    ospfRouter.setInterfaceId(value.interfaceId().get(i));
                }
                ((OspfInterfaceImpl) ospfInterface).addDeviceInformation(ospfRouter);
            }
        }
        if (deviceInformationMapForPointToPoint.size() != 0) {
            for (String key : deviceInformationMapForPointToPoint.keySet()) {
                DeviceInformation value = deviceInformationMapForPointToPoint.get(key);
                ospfRouter.setRouterIp(value.routerId());
                ospfRouter.setAreaIdOfInterface(ospfArea.areaId());
                ospfRouter.setNeighborRouterId(value.deviceId());
                OspfDeviceTed ospfDeviceTed = new OspfDeviceTedImpl();
                List<Ip4Address> ip4Addresses = value.interfaceId();
                ospfDeviceTed.setIpv4RouterIds(ip4Addresses);
                ospfRouter.setDeviceTed(ospfDeviceTed);
                ospfRouter.setOpaque(value.isDr());
                int size = value.interfaceId().size();
                for (int i = 0; i < size; i++) {
                    ospfRouter.setInterfaceId(value.interfaceId().get(i));
                }
                ((OspfInterfaceImpl) ospfInterface).addDeviceInformation(ospfRouter);
            }
        }
        for (Map.Entry<String, LinkInformation> entry : linkInformationMap.entrySet()) {
            String key = entry.getKey();
            LinkInformation value = entry.getValue();
            OspfRouter ospfRouterForLink = new OspfRouterImpl();
            ospfRouterForLink.setInterfaceId(value.interfaceIp());
            ospfRouterForLink.setAreaIdOfInterface(ospfArea.areaId());
            ospfRouterForLink.setOpaque(ospfArea.isOpaqueEnabled());
            OspfLinkTed ospfLinkTed = topologyForDeviceAndLink.getOspfLinkTedHashMap(
                    value.linkDestinationId().toString());
            if (ospfLinkTed == null) {
                ospfLinkTed = new OspfLinkTedImpl();
                ospfLinkTed.setMaximumLink(Bandwidth.bps(0));
                ospfLinkTed.setMaxReserved(Bandwidth.bps(0));
                ospfLinkTed.setTeMetric(0);
            }

            if (!value.isLinkSrcIdNotRouterId()) {
                ospfRouterForLink.setRouterIp(value.linkSourceId());
                ospfRouterForLink.setNeighborRouterId(value.linkDestinationId());
                try {
                    ((OspfInterfaceImpl) ospfInterface).addLinkInformation(ospfRouterForLink, ospfLinkTed);
                } catch (Exception e) {
                    log.debug("Exception addLinkInformation: " + e.getMessage());
                }
            }
        }
    }

    // RFC 2328 Section 13 - partly as flooding procedure

    /**
     * Processes the received Lsa.
     *
     * @param recLsa              received Lsa
     * @param receivedViaFlooding received via flooding or not
     * @param ch                  channel instance
     * @param sourceIp            source of this Lsa
     * @return true to remove it from lsReqList else false
     * @throws Exception might throws exception
     */
    public boolean processReceivedLsa(LsaHeader recLsa,
                                      boolean receivedViaFlooding, Channel ch, Ip4Address sourceIp)
            throws Exception {
        log.debug("OSPFNbr::processReceivedLsa(recLsa, recievedViaFlooding, ch)...!!!");

        //Validate the lsa checksum RFC 2328 13 (1)
        ChecksumCalculator checkSum = new ChecksumCalculator();
        if (!checkSum.isValidLsaCheckSum(recLsa,
                                         recLsa.getOspfLsaType().value(),
                                         OspfUtil.LSAPACKET_CHECKSUM_POS1,
                                         OspfUtil.LSAPACKET_CHECKSUM_POS2)) {
            log.debug("Checksum mismatch. Received LSA packet type {} ",
                      recLsa.lsType());

            return true;
        }

        //If LSA type is unknown discard the lsa RFC 2328 13(2)
        if (((recLsa.getOspfLsaType().value() > OspfLsaType.EXTERNAL_LSA.value()) &&
                (recLsa.getOspfLsaType().value() < OspfLsaType.LINK_LOCAL_OPAQUE_LSA.value())) ||
                (recLsa.getOspfLsaType().value() > OspfLsaType.AS_OPAQUE_LSA.value())) {
            return true;
        }

        //If LSA type is external & the area is configured as stub area discard the lsa RFC 2328 13(3)
        if ((recLsa.getOspfLsaType() == OspfLsaType.EXTERNAL_LSA) &&
                (!ospfArea.isExternalRoutingCapability())) {
            return true;
        }

        //if lsa age is equal to maxage && instance is not in lsdb && none of neighbors are in exchange
        // or loading state
        // Acknowledge the receipt by sending LSAck to the sender. 2328 13(4)
        if ((recLsa.age() == OspfParameters.MAXAGE) &&
                (ospfArea.lsaLookup(recLsa) == null) &&
                ospfArea.noNeighborInLsaExchangeProcess()) {
            // RFC 2328 Section 13. (4)
            // Because the LSA was not yet requested, it is treated as a flooded LSA and thus
            // acknowledged.
            directAcknowledge(recLsa, ch, sourceIp);
            return true;
        }

        String key = ((OspfAreaImpl) ospfArea).getLsaKey(recLsa);
        LsaWrapper lsWrapper = ospfArea.lsaLookup(recLsa);
        String status = isNullorLatest(lsWrapper, recLsa);
        //Section 13 (5)
        if (status.equals("isNullorLatest")) {

            if (recLsa.lsType() == OspfLsaType.ROUTER.value() && recLsa.advertisingRouter().equals(
                    ospfArea.routerId())) {
                if (recLsa.lsSequenceNo() > ((LsaWrapperImpl) lsWrapper).lsaHeader().lsSequenceNo()) {
                    ospfArea.setDbRouterSequenceNumber(recLsa.lsSequenceNo() + 1);
                    processSelfOriginatedLsa();
                }

                if (recLsa.age() == OspfParameters.MAXAGE) {
                    ((LsaWrapperImpl) lsWrapper).lsaHeader().setAge(OspfParameters.MAXAGE);
                    //remove from db & bin, add the lsa to MaxAge bin.
                    ospfArea.addLsaToMaxAgeBin(((OspfAreaImpl) ospfArea).getLsaKey(((LsaWrapperImpl)
                            lsWrapper).lsaHeader()), lsWrapper);
                    ospfArea.removeLsaFromBin(lsWrapper);
                }

                return true;
            } else if (recLsa.lsType() == OspfLsaType.NETWORK.value() && isLinkStateMatchesOwnRouterId(
                    recLsa.linkStateId())) {
                // if we are not DR or if origination router ID not equal to our router ID //either
                // DR state changed or our router ID was changed
                //set LSAge = MaxAge
                //flood the LSA
                if (((OspfInterfaceImpl) ospfInterface).state() != OspfInterfaceState.DR ||
                        !recLsa.advertisingRouter().equals(
                                ospfArea.routerId())) {
                    if (lsWrapper != null) {
                        ((LsaWrapperImpl) lsWrapper).lsaHeader().setAge(OspfParameters.MAXAGE);
                        //remove from bin, add the lsa to MaxAge bin.
                        ospfArea.addLsaToMaxAgeBin(((OspfAreaImpl) ospfArea).getLsaKey(((LsaWrapperImpl)
                                lsWrapper).lsaHeader()), lsWrapper);
                        ospfArea.removeLsaFromBin(lsWrapper);
                    } else {
                        recLsa.setAge(OspfParameters.MAXAGE);
                        ((OspfAreaImpl) ospfArea).addToOtherNeighborLsaTxList(recLsa);
                    }
                }

                return true;
            } else {
                if (recLsa.age() == OspfParameters.MAXAGE) {
                    ((OspfInterfaceImpl) ospfInterface).addLsaHeaderForDelayAck(recLsa);
                    //remove from db & bin, add the lsa to MaxAge bin.
                    if (lsWrapper != null) {
                        lsWrapper.setLsaAgeReceived(OspfParameters.MAXAGE);
                        ospfArea.addLsaToMaxAgeBin(((OspfAreaImpl) ospfArea).getLsaKey(((LsaWrapperImpl)
                                lsWrapper).lsaHeader()), lsWrapper);
                        ospfArea.removeLsaFromBin(lsWrapper);
                    } else {
                        ((OspfAreaImpl) ospfArea).addToOtherNeighborLsaTxList(recLsa);
                    }

                    return true;
                } else {
                    ospfArea.addLsa(recLsa, ospfInterface);
                    log.debug("Inside addLsaMethod");
                    topologyForDeviceAndLink.addLocalDevice(recLsa, ospfInterface, ospfArea);
                    callDeviceAndLinkAdding(topologyForDeviceAndLink);
                    log.debug("Adding to lsdb interface State {}", ((OspfInterfaceImpl) ospfInterface).state().value());
                    // should not send any acknowledge if flooded out on receiving interface
                    if (((OspfInterfaceImpl) ospfInterface).state().value() == OspfInterfaceState.BDR.value()) {
                        if (neighborDr.equals(sourceIp)) {
                            log.debug("Adding for delayed ack {}", recLsa);
                            ((OspfInterfaceImpl) ospfInterface).addLsaHeaderForDelayAck(recLsa);
                        }
                    } else {
                        log.debug("Adding for delayed ack {}", recLsa);
                        ((OspfInterfaceImpl) ospfInterface).addLsaHeaderForDelayAck(recLsa);
                    }

                    if (((OspfInterfaceImpl) ospfInterface).state().value() == OspfInterfaceState.DR.value() ||
                            ((OspfInterfaceImpl) ospfInterface).state().value() ==
                                    OspfInterfaceState.POINT2POINT.value()) {
                        ((OspfAreaImpl) ospfArea).addToOtherNeighborLsaTxList(recLsa);
                    }
                }

            }
        }
        // RFC 2328 Section 13  (6)
        if (lsReqList.contains(key)) {
            badLSReq(ch);
        }
        if (status.equals("same")) { //13 (7)
            if (pendingReTxList.containsKey(key)) {
                pendingReTxList.remove(key);
                if (((OspfInterfaceImpl) ospfInterface).state().value() == OspfInterfaceState.BDR.value()) {
                    if (neighborDr.equals(recLsa.advertisingRouter())) {
                        ((OspfInterfaceImpl) ospfInterface).addLsaHeaderForDelayAck(recLsa);
                    }
                }
            } else {
                directAcknowledge(recLsa, ch, sourceIp);
                return true;
            }
        } else if (status.equals("old")) { // section 13 - point 8
            if ((recLsa.lsSequenceNo() == OspfParameters.MAXSEQUENCENUMBER) &&
                    (recLsa.age() == OspfParameters.MAXAGE)) {
                // section 13 - point 8
                // simple discard the received LSA -
                return true;
            } else {
                // respond back with the same LSA
                //Using flood LSA to sent the LSUpdate back to advertising router
                int diff = Math.abs(lsWrapper.lsaAgeReceived() - recLsa.age());
                if (diff > OspfParameters.MINLSARRIVAL) {
                    sendLsa(((LsaWrapperImpl) lsWrapper).lsaHeader(), sourceIp, ch);
                }
            }
        }

        constructDeviceInformationFromDb();
        callDeviceAndLinkAdding(topologyForDeviceAndLink);

        return true;
    }

    /**
     * Constructs device and link information from link state database.
     */
    private void constructDeviceInformationFromDb() {
        OspfLsdb database = ospfArea.database();
        List lsas = database.getAllLsaHeaders(true, true);
        Iterator iterator = lsas.iterator();
        while (iterator.hasNext()) {
            OspfLsa ospfLsa = (OspfLsa) iterator.next();
            if (ospfLsa.getOspfLsaType().value() == OspfLsaType.ROUTER.value()) {
                topologyForDeviceAndLink.addLocalDevice(ospfLsa, ospfInterface, ospfArea);
            } else if (ospfLsa.getOspfLsaType().value() == OspfLsaType.NETWORK.value()) {
                topologyForDeviceAndLink.addLocalDevice(ospfLsa, ospfInterface, ospfArea);
            }
        }
    }

    /**
     * Checks Link State ID is equal to one of the router's own IP interface addresses.
     *
     * @param linkStateId link state id
     * @return true if link state matches or false
     */
    private boolean isLinkStateMatchesOwnRouterId(String linkStateId) {
        boolean isLinkStateMatches = false;
        List<OspfInterface> interfaceLst = ospfArea.ospfInterfaceList();
        for (OspfInterface ospfInterface : interfaceLst) {
            if (ospfInterface.ipAddress().toString().equals(linkStateId)) {
                isLinkStateMatches = true;
                break;
            }
        }

        return isLinkStateMatches;
    }

    /**
     * RFC 2328 Section 13 (5).
     *
     * @param lsWrapper ls wrapper instance
     * @param recLsa    received LSA instance
     * @return returns a string status
     */
    public String isNullorLatest(LsaWrapper lsWrapper, LsaHeader recLsa) {


        if (lsWrapper != null) {
            LsaHeader ownLsa = (LsaHeader) lsWrapper.ospfLsa();
            String status = ospfArea.isNewerOrSameLsa(recLsa, ownLsa);

            if (status.equals("latest")) {
                return "isNullorLatest";
            } else {
                return status;
            }
        } else {
            return "isNullorLatest";
        }
    }

    /**
     * RFC 2328 section 13.4
     * Processing self-originated LSAs.
     *
     * @throws Exception might throws exception
     */
    public void processSelfOriginatedLsa() throws Exception {
        ospfArea.refreshArea(ospfInterface);
    }

    /**
     * Sends the LSA to destination address.
     *
     * @param lsa         LSA instance to sent
     * @param destination destination IP address
     * @param ch          netty channel instance
     */
    public void sendLsa(LsaHeader lsa, Ip4Address destination, Channel ch) {
        if (lsa == null) {
            return;
        }

        LsUpdate responseLsUpdate = new LsUpdate();
        // seting OSPF Header
        responseLsUpdate.setOspfVer(OspfUtil.OSPF_VERSION);
        responseLsUpdate.setOspftype(OspfPacketType.LSUPDATE.value());
        responseLsUpdate.setRouterId(ospfArea.routerId());
        responseLsUpdate.setAreaId(ospfArea.areaId());
        responseLsUpdate.setAuthType(OspfUtil.NOT_ASSIGNED);
        responseLsUpdate.setAuthentication(OspfUtil.NOT_ASSIGNED);
        responseLsUpdate.setOspfPacLength(OspfUtil.NOT_ASSIGNED); // to calculate packet length
        responseLsUpdate.setChecksum(OspfUtil.NOT_ASSIGNED);
        responseLsUpdate.setNumberOfLsa(1);
        responseLsUpdate.addLsa(lsa);

        //setting the destination.
        responseLsUpdate.setDestinationIp(destination);
        byte[] messageToWrite = getMessage(responseLsUpdate);
        ch.write(messageToWrite);
    }

    /**
     * Sends a direct Acknowledgment for a particular LSA to the Neighbor.
     *
     * @param ackLsa   LSA instance
     * @param ch       netty channel instance
     * @param sourceIp source IP address
     */
    public void directAcknowledge(LsaHeader ackLsa, Channel ch, Ip4Address sourceIp) {
        log.debug("OSPFNbr::directAcknowledge...!!!");

        LsAcknowledge ackContent = new LsAcknowledge();
        // seting OSPF Header
        ackContent.setOspfVer(OspfUtil.OSPF_VERSION);
        ackContent.setOspftype(OspfPacketType.LSAACK.value());
        ackContent.setRouterId(ospfArea.routerId());
        ackContent.setAreaId(ospfArea.areaId());
        ackContent.setAuthType(OspfUtil.NOT_ASSIGNED);
        ackContent.setAuthentication(OspfUtil.NOT_ASSIGNED);
        ackContent.setOspfPacLength(OspfUtil.NOT_ASSIGNED); // to calculate packet length
        ackContent.setChecksum(OspfUtil.NOT_ASSIGNED);
        ackContent.addLinkStateHeader(ackLsa);
        //setting the destination IP
        ackContent.setDestinationIp(sourceIp);
        byte[] messageToWrite = getMessage(ackContent);
        ch.write(messageToWrite);
    }

    /**
     * Called when neighbor is down.
     *
     * @throws Exception might throws exception
     */
    public void neighborDown() throws Exception {
        log.debug("Neighbor Down {} and NeighborId {}", neighborIpAddr,
                  neighborId);
        stopInactivityTimeCheck();
        stopRxMtDdTimer();
        stopRxMtLsrTimer();

        if (floodingTimerScheduled) {
            stopFloodingTimer();
            floodingTimerScheduled = false;
        }

        state = OspfNeighborState.DOWN;
        ospfArea.refreshArea(ospfInterface);
        lsReqList.clear();
        ddSummaryList.clear();
        if (neighborIpAddr.equals(neighborBdr) ||
                neighborIpAddr.equals(neighborDr)) {
            ((OspfInterfaceImpl) ospfInterface).neighborChange();
        }
        log.debug("Neighbor Went Down : "
                          + this.neighborIpAddr + " , " + this.neighborId);
        removeDeviceDetails(this.neighborId);
        OspfRouter ospfRouter = new OspfRouterImpl();
        ospfRouter.setRouterIp(this.neighborId());
        ospfRouter.setInterfaceId(ospfInterface.ipAddress());
        ospfRouter.setAreaIdOfInterface(ospfArea.areaId());
        ospfRouter.setDeviceTed(new OspfDeviceTedImpl());
        ((OspfInterfaceImpl) ospfInterface).removeDeviceInformation(ospfRouter);
        removeDeviceDetails(this.neighborIpAddr);
        OspfRouter ospfRouter1 = new OspfRouterImpl();
        ospfRouter1.setRouterIp(this.neighborIpAddr);
        ospfRouter1.setInterfaceId(ospfInterface.ipAddress());
        ospfRouter1.setAreaIdOfInterface(ospfArea.areaId());
        ospfRouter1.setDeviceTed(new OspfDeviceTedImpl());
        ((OspfInterfaceImpl) ospfInterface).removeDeviceInformation(ospfRouter1);
    }

    /**
     * Removes device details.
     *
     * @param routerId router id
     */
    private void removeDeviceDetails(Ip4Address routerId) {
        String key = "device:" + routerId;
        topologyForDeviceAndLink.removeDeviceInformationMap(key);
    }

    /**
     * Starts the inactivity timer.
     */
    public void startInactivityTimeCheck() {
        if (!inActivityTimerScheduled) {
            log.debug("OSPFNbr::startInactivityTimeCheck");
            inActivityTimeCheckTask = new InternalInactivityTimeCheck();
            exServiceInActivity = Executors.newSingleThreadScheduledExecutor();
            exServiceInActivity.scheduleAtFixedRate(inActivityTimeCheckTask, routerDeadInterval,
                                                    routerDeadInterval, TimeUnit.SECONDS);
            inActivityTimerScheduled = true;
        }
    }

    /**
     * Stops the inactivity timer.
     */
    public void stopInactivityTimeCheck() {
        if (inActivityTimerScheduled) {
            log.debug("OSPFNbr::stopInactivityTimeCheck ");
            exServiceInActivity.shutdown();
            inActivityTimerScheduled = false;
        }
    }

    /**
     * Starts the flooding timer.
     *
     * @param channel channel instance
     */
    public void startFloodingTimer(Channel channel) {

        if (!floodingTimerScheduled) {
            log.debug("OSPFNbr::startFloodingTimer");
            floodingTask = new InternalFloodingTask(channel);
            exServiceFlooding = Executors.newSingleThreadScheduledExecutor();
            //Run every 5 seconds.
            exServiceFlooding.scheduleAtFixedRate(floodingTask, OspfParameters.START_NOW,
                                                  OspfParameters.MINLSINTERVAL, TimeUnit.SECONDS);
            floodingTimerScheduled = true;
        }
    }

    /**
     * Stops the flooding timer.
     */
    public void stopFloodingTimer() {
        if (floodingTimerScheduled) {
            log.debug("OSPFNbr::stopFloodingTimer ");
            exServiceFlooding.shutdown();
            floodingTimerScheduled = false;
        }
    }

    /**
     * Starts the Dd Retransmission executor task.
     *
     * @param ch netty channel instance
     */
    private void startRxMtDdTimer(Channel ch) {
        if (!rxmtDdPacketTimerScheduled) {
            long retransmitInterval = ospfInterface.reTransmitInterval();
            rxmtDdPacketTask = new InternalRxmtDdPacket(ch);
            exServiceRxmtDDPacket = Executors.newSingleThreadScheduledExecutor();
            exServiceRxmtDDPacket.scheduleAtFixedRate(rxmtDdPacketTask, retransmitInterval,
                                                      retransmitInterval, TimeUnit.SECONDS);
            rxmtDdPacketTimerScheduled = true;
        }
    }

    /**
     * Stops the Dd Retransmission executor task.
     */
    public void stopRxMtDdTimer() {
        if (rxmtDdPacketTimerScheduled) {
            exServiceRxmtDDPacket.shutdown();
            rxmtDdPacketTimerScheduled = false;
        }
    }

    /**
     * Starts Ls request retransmission executor task.
     *
     * @param ch Netty channel instance
     */
    private void startRxMtLsrTimer(Channel ch) {
        if (!rxmtLsrTimerScheduled) {
            log.debug("OSPFNbr::startRxMtLsrTimer...!!!");
            long retransmitIntrvl = ospfInterface.reTransmitInterval();
            rxmtLsrPacketTask = new InternalRxmtLsrPacket(ch);
            exServiceRxmtLsr = Executors.newSingleThreadScheduledExecutor();
            exServiceRxmtLsr.scheduleAtFixedRate(rxmtLsrPacketTask, retransmitIntrvl,
                                                 retransmitIntrvl, TimeUnit.SECONDS);
            rxmtLsrTimerScheduled = true;
        }
    }

    /**
     * Stops Ls request retransmission executor task.
     */
    public void stopRxMtLsrTimer() {
        if (rxmtLsrTimerScheduled) {
            exServiceRxmtLsr.shutdown();
            rxmtLsrTimerScheduled = false;
        }
    }

    /**
     * Gets the last sent DdPacket.
     *
     * @return DdPacket instance
     */
    public DdPacket lastDdPacket() {
        return lastDdPacket;
    }

    /**
     * Sets the last sent DdPacket.
     *
     * @param lastDdPacket DdPacket instance
     */
    public void setLastDdPacket(DdPacket lastDdPacket) {
        this.lastDdPacket = lastDdPacket;
    }

    /**
     * Gets neighbor id.
     *
     * @return neighbor id
     */
    public Ip4Address neighborId() {
        return neighborId;
    }

    /**
     * Sets the neighbor id.
     *
     * @param neighborId neighbor id
     */
    public void setNeighborId(Ip4Address neighborId) {
        this.neighborId = neighborId;
    }

    /**
     * Gets the neighbor DR address.
     *
     * @return neighbor DR address
     */
    public Ip4Address neighborDr() {
        return neighborDr;
    }

    /**
     * Sets the neighbor DR address.
     *
     * @param neighborDr neighbor DR address
     */
    public void setNeighborDr(Ip4Address neighborDr) {
        this.neighborDr = neighborDr;
    }

    /**
     * Gets the neighbor BDR address.
     *
     * @return neighbor BDR address
     */
    public Ip4Address neighborBdr() {
        return neighborBdr;
    }

    /**
     * Sets the neighbor BDR address.
     *
     * @param neighborBdr neighbor BDR address
     */
    public void setNeighborBdr(Ip4Address neighborBdr) {
        this.neighborBdr = neighborBdr;
    }

    /**
     * Gets router priority.
     *
     * @return router priority
     */
    public int routerPriority() {
        return routerPriority;
    }

    /**
     * Sets router priority.
     *
     * @param routerPriority router priority
     */
    public void setRouterPriority(int routerPriority) {
        this.routerPriority = routerPriority;
    }

    /**
     * Gets the options value.
     *
     * @return options value
     */
    public int options() {
        return options;
    }

    /**
     * Sets the options value.
     *
     * @param options options value
     */
    public void setOptions(int options) {
        this.options = options;
    }

    /**
     * Gets the DD sequence number.
     *
     * @return DD sequence number
     */
    public long ddSeqNum() {
        return ddSeqNum;
    }

    /**
     * Sets the DD sequence number.
     *
     * @param ddSeqNum DD sequence number
     */
    public void setDdSeqNum(long ddSeqNum) {
        this.ddSeqNum = ddSeqNum;
    }

    /**
     * Gets neighbor is master or not.
     *
     * @return true if neighbor is master else false
     */
    public int isMaster() {
        return isMaster;
    }

    /**
     * Gets the last sent DD Packet.
     *
     * @return last sent DD Packet
     */
    public DdPacket lastSentDdPacket() {
        return lastSentDdPacket;
    }

    /**
     * Sets the last sent DD Packet.
     *
     * @param lastSentDdPacket last sent DD Packet
     */
    public void setLastSentDdPacket(DdPacket lastSentDdPacket) {
        this.lastSentDdPacket = lastSentDdPacket;
    }

    /**
     * Gets the last sent Ls Request Packet.
     *
     * @return last sent Ls Request Packet
     */
    public LsRequest getLastSentLsrPacket() {
        return lastSentLsrPacket;
    }

    /**
     * Sets the last sent Ls Request Packet.
     *
     * @param lastSentLsrPacket last sent Ls Request Packet
     */
    public void setLastSentLsrPacket(LsRequest lastSentLsrPacket) {
        this.lastSentLsrPacket = lastSentLsrPacket;
    }

    /**
     * Gets the neighbors state.
     *
     * @return neighbors state
     */
    public OspfNeighborState getState() {
        return state;
    }

    /**
     * Sets the neighbors state.
     *
     * @param state neighbors state
     */
    public void setState(OspfNeighborState state) {
        this.state = state;
    }

    /**
     * Sets neighbor is master or not.
     *
     * @param isMaster neighbor is master or not
     */
    public void setIsMaster(int isMaster) {
        this.isMaster = isMaster;
    }

    /**
     * Gets the ls request list.
     *
     * @return ls request list
     */
    public Hashtable getLsReqList() {
        return lsReqList;
    }

    /**
     * Gets the reTxList instance.
     *
     * @return reTxList instance
     */
    public Map getReTxList() {
        return reTxList;
    }

    /**
     * Gets the pending re transmit list.
     *
     * @return pendingReTxList instance
     */
    public Map<String, OspfLsa> getPendingReTxList() {
        return pendingReTxList;
    }

    /**
     * Gets message as bytes.
     *
     * @param ospfMessage OSPF message
     * @return OSPF message
     */
    private byte[] getMessage(OspfMessage ospfMessage) {
        OspfMessageWriter messageWriter = new OspfMessageWriter();
        if (((OspfInterfaceImpl) ospfInterface).state().equals(OspfInterfaceState.POINT2POINT)) {
            ospfMessage.setDestinationIp(OspfUtil.ALL_SPF_ROUTERS);
        }
        return (messageWriter.getMessage(ospfMessage, ospfInterface.interfaceIndex(),
                                         ((OspfInterfaceImpl) ospfInterface).state().value()));
    }


    /**
     * Represents a Task which will do an inactivity time check.
     */
    private class InternalInactivityTimeCheck implements Runnable {
        /**
         * Constructor.
         */
        InternalInactivityTimeCheck() {
        }

        @Override
        public void run() {
            try {
                log.debug("Neighbor Not Heard till the past router dead interval .");
                neighborDown();
            } catch (Exception e) {
                log.debug("Exception at inactivity time check...!!!");
            }
        }
    }

    /**
     * Task which re transmits DdPacket every configured time interval.
     */
    private class InternalRxmtDdPacket implements Runnable {
        Channel ch;

        /**
         * Creates an instance or Re transmit DD packet timer.
         *
         * @param ch netty channel instance
         */
        InternalRxmtDdPacket(Channel ch) {
            this.ch = ch;
        }

        @Override
        public void run() {
            if ((ch != null) && ch.isConnected()) {
                DdPacket ddPacket = lastSentDdPacket();
                byte[] messageToWrite = getMessage(ddPacket);
                ch.write(messageToWrite);
                log.debug("Re-Transmit DD Packet .");
            } else {
                log.debug(
                        "Re-Transmit DD Packet failed. Channel not connected..");
            }
        }
    }

    /**
     * Task which re transmits Ls request Packet every configured time interval.
     */
    private class InternalRxmtLsrPacket implements Runnable {
        Channel ch;

        /**
         * Creates an instance or Re transmit LS Request packet timer.
         *
         * @param ch netty channel instance
         */
        InternalRxmtLsrPacket(Channel ch) {
            this.ch = ch;
        }

        @Override
        public void run() {
            if ((ch != null) && ch.isConnected()) {
                LsRequest lsrPacket = getLastSentLsrPacket();
                byte[] messageToWrite = getMessage(lsrPacket);
                ch.write(messageToWrite);
                log.debug("Re-Transmit LSRequest Packet .");
            } else {
                log.debug(
                        "Re-Transmit LSRequest failed. Channel not connected..");
            }
        }
    }

    /**
     * Task which transmits Ls update Packet based on the re transmit list.
     * every configured time interval.
     */
    private class InternalFloodingTask implements Runnable {
        Channel channel;

        /**
         * Creates an instance or Flooding task.
         *
         * @param ch netty channel instance
         */
        InternalFloodingTask(Channel ch) {
            this.channel = ch;
        }

        @Override
        public void run() {
            if ((channel != null) && channel.isConnected()) {

                if ((pendingReTxList != null) && (pendingReTxList.size() > 0)) {
                    List<LsUpdate> lsUpdateList = buildLsUpdate(pendingReTxList);

                    for (LsUpdate lsupdate : lsUpdateList) {
                        //Pending for acknowledge directly sent it to neighbor
                        lsupdate.setDestinationIp(neighborIpAddr);
                        byte[] messageToWrite = getMessage(lsupdate);
                        channel.write(messageToWrite);
                    }
                }

                if ((reTxList != null) && (reTxList.size() > 0)) {
                    List<LsUpdate> lsUpdateList = buildLsUpdate(reTxList);

                    for (LsUpdate lsupdate : lsUpdateList) {
                        //set the destination
                        if ((((OspfInterfaceImpl) ospfInterface).state() == OspfInterfaceState.DR) ||
                                (((OspfInterfaceImpl) ospfInterface).state() == OspfInterfaceState.POINT2POINT)) {
                            lsupdate.setDestinationIp(OspfUtil.ALL_SPF_ROUTERS);
                        } else if (((OspfInterfaceImpl) ospfInterface).state() == OspfInterfaceState.DROTHER ||
                                (((OspfInterfaceImpl) ospfInterface).state() == OspfInterfaceState.BDR)) {
                            lsupdate.setDestinationIp(neighborDr);
                        }
                        byte[] messageToWrite = getMessage(lsupdate);
                        channel.write(messageToWrite);
                    }
                }
            }
        }

        /**
         * Builds the LsUpdate for flooding.
         *
         * @param txList list contains LSAs
         * @return list of LsUpdate instances
         */
        private List buildLsUpdate(Map<String, OspfLsa> txList) {
            List<LsUpdate> lsUpdateList = new ArrayList<>();
            ListIterator itr = new ArrayList(txList.keySet()).listIterator();
            while (itr.hasNext()) {
                LsUpdate lsupdate = new LsUpdate();
                // seting OSPF Header
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
                int maxSize = ospfInterface.mtu() -
                        OspfUtil.LSA_HEADER_LENGTH; // subtract a normal IP header.

                int noLsa = 0;
                while (itr.hasNext()) {

                    String key = (String) itr.next();
                    OspfLsa lsa = txList.get(key);
                    if (lsa != null) {
                        if ((lsa.age() + OspfParameters.INFTRA_NS_DELAY) >= OspfParameters.MAXAGE) {
                            ((LsaHeader) lsa.lsaHeader()).setAge(OspfParameters.MAXAGE);
                        } else {
                            ((LsaHeader) lsa.lsaHeader()).setAge(lsa.age() + OspfParameters.INFTRA_NS_DELAY);
                        }

                        if ((currentLength + ((LsaHeader) lsa.lsaHeader()).lsPacketLen()) >= maxSize) {
                            itr.previous();
                            break;
                        }
                        log.debug("FloodingTimer::LSA Type::{}, Header: {}, LSA: {}", lsa.getOspfLsaType(),
                                  lsa.lsaHeader(), lsa);
                        lsupdate.addLsa(lsa);
                        noLsa++;
                        currentLength = currentLength + ((LsaHeader) lsa.lsaHeader()).lsPacketLen();
                    }
                    log.debug("FloodingTimer::Removing key {}", key);
                    if (txList.equals(reTxList)) {
                        reTxList.remove(key);
                        pendingReTxList.put(key, lsa);
                    }
                }
                //set number of lsa's
                lsupdate.setNumberOfLsa(noLsa);
                lsUpdateList.add(lsupdate);
            }
            return lsUpdateList;
        }
    }
}
