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
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.timeout.IdleStateAwareChannelHandler;
import org.jboss.netty.handler.timeout.ReadTimeoutException;
import org.onlab.packet.Ip4Address;
import org.onosproject.ospf.controller.OspfArea;
import org.onosproject.ospf.controller.OspfInterface;
import org.onosproject.ospf.controller.OspfMessage;
import org.onosproject.ospf.controller.OspfNbr;
import org.onosproject.ospf.controller.OspfProcess;
import org.onosproject.ospf.controller.area.OspfInterfaceImpl;
import org.onosproject.ospf.controller.util.OspfInterfaceType;
import org.onosproject.ospf.exceptions.OspfParseException;
import org.onosproject.ospf.protocol.util.OspfInterfaceState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;

/**
 * Channel handler deals with the OSPF channel connection.
 * Also it dispatches messages to the appropriate handlers for processing.
 */
public class OspfInterfaceChannelHandler extends IdleStateAwareChannelHandler {

    private static final Logger log = LoggerFactory.getLogger(OspfInterfaceChannelHandler.class);
    private static Map<Integer, Object> isisDb = null;
    private Channel channel = null;
    private Controller controller;
    private List<OspfProcess> processes = null;
    private byte[] configPacket = null;
    private Map<Integer, OspfInterface> ospfInterfaceMap = new ConcurrentHashMap<>();

    /**
     * Creates an instance of OSPF channel handler.
     *
     * @param controller controller instance
     * @param processes  list of configured processes
     */
    public OspfInterfaceChannelHandler(Controller controller, List<OspfProcess> processes) {
        this.controller = controller;
        this.processes = processes;
    }

    /**
     * Initializes the interface map with interface details.
     *
     * @throws Exception might throws exception
     */
    public void initializeInterfaceMap() throws Exception {
        for (OspfProcess process : processes) {
            for (OspfArea area : process.areas()) {
                for (OspfInterface ospfInterface : area.ospfInterfaceList()) {
                    OspfInterface anInterface = ospfInterfaceMap.get(ospfInterface.interfaceIndex());
                    if (anInterface == null) {
                        ospfInterface.setOspfArea(area);
                        ((OspfInterfaceImpl) ospfInterface).setController(controller);
                        ((OspfInterfaceImpl) ospfInterface).setState(OspfInterfaceState.DOWN);
                        ospfInterface.setDr(Ip4Address.valueOf("0.0.0.0"));
                        ospfInterface.setBdr(Ip4Address.valueOf("0.0.0.0"));
                        ospfInterfaceMap.put(ospfInterface.interfaceIndex(), ospfInterface);
                    }
                    ((OspfInterfaceImpl) ospfInterface).setChannel(channel);
                    ospfInterface.interfaceUp();
                    ospfInterface.startDelayedAckTimer();
                }
                //Initialize the LSDB and aging process
                area.initializeDb();
            }
        }
    }

    /**
     * Updates the interface map with interface details.
     *
     * @param ospfProcesses updated process instances
     * @throws Exception might throws exception
     */
    public void updateInterfaceMap(List<OspfProcess> ospfProcesses) throws Exception {
        for (OspfProcess ospfUpdatedProcess : ospfProcesses) {
            for (OspfArea updatedArea : ospfUpdatedProcess.areas()) {
                for (OspfInterface ospfUpdatedInterface : updatedArea.ospfInterfaceList()) {
                    OspfInterface ospfInterface = ospfInterfaceMap.get(ospfUpdatedInterface.interfaceIndex());
                    if (ospfInterface == null) {
                        ospfUpdatedInterface.setOspfArea(updatedArea);
                        ((OspfInterfaceImpl) ospfUpdatedInterface).setController(controller);
                        ((OspfInterfaceImpl) ospfUpdatedInterface).setState(OspfInterfaceState.DOWN);
                        ospfUpdatedInterface.setDr(Ip4Address.valueOf("0.0.0.0"));
                        ospfUpdatedInterface.setBdr(Ip4Address.valueOf("0.0.0.0"));
                        ospfInterfaceMap.put(ospfUpdatedInterface.interfaceIndex(), ospfUpdatedInterface);
                        ((OspfInterfaceImpl) ospfUpdatedInterface).setChannel(channel);
                        ospfUpdatedInterface.interfaceUp();
                        ospfUpdatedInterface.startDelayedAckTimer();
                    } else {
                        ospfInterface.setOspfArea(updatedArea);

                        if (ospfInterface.routerDeadIntervalTime() != ospfUpdatedInterface.routerDeadIntervalTime()) {
                            ospfInterface.setRouterDeadIntervalTime(ospfUpdatedInterface.routerDeadIntervalTime());
                            Map<String, OspfNbr> neighbors = ospfInterface.listOfNeighbors();
                            for (String key : neighbors.keySet()) {
                                OspfNbr ospfNbr = ospfInterface.neighbouringRouter(key);
                                ospfNbr.setRouterDeadInterval(ospfInterface.routerDeadIntervalTime());
                                ospfNbr.stopInactivityTimeCheck();
                                ospfNbr.startInactivityTimeCheck();
                            }
                        }
                        if (ospfInterface.interfaceType() != ospfUpdatedInterface.interfaceType()) {
                            ospfInterface.setInterfaceType(ospfUpdatedInterface.interfaceType());
                            if (ospfInterface.interfaceType() == OspfInterfaceType.POINT_TO_POINT.value()) {
                                ospfInterface.setDr(Ip4Address.valueOf("0.0.0.0"));
                                ospfInterface.setBdr(Ip4Address.valueOf("0.0.0.0"));
                            }
                            ospfInterface.removeNeighbors();
                        }
                        if (ospfInterface.helloIntervalTime() != ospfUpdatedInterface.helloIntervalTime()) {
                            ospfInterface.setHelloIntervalTime(ospfUpdatedInterface.helloIntervalTime());
                            ospfInterface.stopHelloTimer();
                            ospfInterface.startHelloTimer();
                        }
                        ospfInterfaceMap.put(ospfInterface.interfaceIndex(), ospfInterface);
                    }
                }
            }
        }
    }

    /**
     * Initialize channel, start hello sender and initialize LSDB.
     */
    private void initialize() throws Exception {
        log.debug("OspfChannelHandler initialize..!!!");
        if (configPacket != null) {
            log.debug("OspfChannelHandler initialize -> sentConfig packet of length ::"
                              + configPacket.length);
            sentConfigPacket(configPacket);
        }
        initializeInterfaceMap();
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent evt) throws Exception {
        log.info("OSPF channelConnected from {}", evt.getChannel().getRemoteAddress());
        this.channel = evt.getChannel();
        initialize();
    }

    @Override
    public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent evt) {
        log.debug("OspfChannelHandler::channelDisconnected...!!!");

        for (Integer interfaceIndex : ospfInterfaceMap.keySet()) {
            OspfInterface anInterface = ospfInterfaceMap.get(interfaceIndex);
            if (anInterface != null) {
                anInterface.interfaceDown();
                anInterface.stopDelayedAckTimer();
            }
        }

        if (controller != null) {
            controller.connectPeer();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent
            e) throws Exception {
        log.debug("[exceptionCaught]: " + e.toString());
        if (e.getCause() instanceof ReadTimeoutException) {
            log.debug("Disconnecting device {} due to read timeout", e.getChannel().getRemoteAddress());
            return;
        } else if (e.getCause() instanceof ClosedChannelException) {
            log.debug("Channel for OSPF {} already closed", e.getChannel().getRemoteAddress());
        } else if (e.getCause() instanceof IOException) {
            log.debug("Disconnecting OSPF {} due to IO Error: {}", e.getChannel().getRemoteAddress(),
                      e.getCause().getMessage());
        } else if (e.getCause() instanceof OspfParseException) {
            OspfParseException errMsg = (OspfParseException) e.getCause();
            byte errorCode = errMsg.errorCode();
            byte errorSubCode = errMsg.errorSubCode();
            log.debug("Error while parsing message from OSPF {}, ErrorCode {}",
                      e.getChannel().getRemoteAddress(), errorCode);
        } else if (e.getCause() instanceof RejectedExecutionException) {
            log.debug("Could not process message: queue full");
        } else {
            log.debug("Error while processing message from OSPF {}, {}",
                      e.getChannel().getRemoteAddress(), e.getCause().getMessage());
        }
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent
            e) throws Exception {
        log.debug("OspfChannelHandler::messageReceived...!!!");
        Object message = e.getMessage();
        if (message instanceof List) {
            List<OspfMessage> ospfMessageList = (List<OspfMessage>) message;
            log.debug("OspfChannelHandler::List of IsisMessages Size {}", ospfMessageList.size());
            if (ospfMessageList != null) {
                for (OspfMessage ospfMessage : ospfMessageList) {
                    processOspfMessage(ospfMessage, ctx);
                }
            } else {
                log.debug("OspfChannelHandler::OspfMessages Null List...!!");
            }
        }
        if (message instanceof OspfMessage) {
            OspfMessage ospfMessage = (OspfMessage) message;
            log.debug("OspfChannelHandler::OspfMessages received...!!");
            processOspfMessage(ospfMessage, ctx);
        }
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
    public void processOspfMessage(OspfMessage
                                           ospfMessage, ChannelHandlerContext ctx) throws Exception {
        log.debug("OspfChannelHandler::processOspfMessage...!!!");
        int interfaceIndex = ospfMessage.interfaceIndex();
        OspfInterface ospfInterface = ospfInterfaceMap.get(interfaceIndex);
        if (ospfInterface != null) {
            ospfInterface.processOspfMessage(ospfMessage, ctx);
        }
    }

    /**
     * Sends the interface configuration packet to server.
     *
     * @param configPacket interface configuration
     */
    public void sentConfigPacket(byte[] configPacket) {
        if (channel != null) {
            channel.write(configPacket);
            log.debug("OspfChannelHandler sentConfigPacket packet sent..!!!");
        } else {
            log.debug("OspfChannelHandler sentConfigPacket channel not connected - re try..!!!");
            this.configPacket = configPacket;
        }
    }
}