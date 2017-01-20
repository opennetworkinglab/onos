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
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.timeout.IdleStateAwareChannelHandler;
import org.jboss.netty.handler.timeout.ReadTimeoutException;
import org.onlab.packet.Ip4Address;
import org.onosproject.isis.controller.IsisInterface;
import org.onosproject.isis.controller.IsisLsdb;
import org.onosproject.isis.controller.IsisMessage;
import org.onosproject.isis.controller.IsisProcess;
import org.onosproject.isis.controller.impl.lsdb.DefaultIsisLsdb;
import org.onosproject.isis.exceptions.IsisParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Channel handler deals with the ISIS channel connection.
 * Also it dispatches messages to the appropriate handlers for processing.
 */
public class IsisChannelHandler extends IdleStateAwareChannelHandler {

    private static final Logger log = LoggerFactory.getLogger(IsisChannelHandler.class);
    private static Map<Integer, Object> isisDb = null;
    private Channel channel = null;
    private Controller controller;
    private List<IsisProcess> processes = null;
    private List<ScheduledExecutorService> executorList = new ArrayList<>();
    private byte[] configPacket = null;
    private Map<Integer, IsisInterface> isisInterfaceMap = new ConcurrentHashMap<>();
    private IsisLsdb isisLsdb = new DefaultIsisLsdb();
    private List<Ip4Address> interfaceIps = new ArrayList<>();

    /**
     * Creates an instance of ISIS channel handler.
     *
     * @param controller controller instance
     * @param processes  list of configured processes
     */
    public IsisChannelHandler(Controller controller, List<IsisProcess> processes) {
        this.controller = controller;
        this.processes = processes;
        ((DefaultIsisLsdb) isisLsdb).setController(this.controller);
        ((DefaultIsisLsdb) isisLsdb).setIsisInterface(isisInterfaceList());
    }

    private List<IsisInterface> isisInterfaceList() {
        List<IsisInterface> isisInterfaceList = new ArrayList<>();
        for (Integer key : isisInterfaceMap.keySet()) {
            isisInterfaceList.add(isisInterfaceMap.get(key));
        }
        return isisInterfaceList;
    }

    /**
     * Initializes the interface map with interface details.
     */
    public void initializeInterfaceMap() {
        for (IsisProcess process : processes) {
            for (IsisInterface isisInterface : process.isisInterfaceList()) {
                IsisInterface anInterface = isisInterfaceMap.get(isisInterface.interfaceIndex());
                if (anInterface == null) {
                    isisInterfaceMap.put(isisInterface.interfaceIndex(), isisInterface);
                    interfaceIps.add(isisInterface.interfaceIpAddress());
                }
            }
        }
        //Initializes the interface with all interface ip details - for ls pdu generation
        initializeInterfaceIpList();
    }

    /**
     * Updates the interface map with interface details.
     *
     * @param isisProcesses updated process instances
     */
    public void updateInterfaceMap(List<IsisProcess> isisProcesses) {
        for (IsisProcess isisUpdatedProcess : isisProcesses) {
            for (IsisInterface isisUpdatedInterface : isisUpdatedProcess.isisInterfaceList()) {
                IsisInterface isisInterface = isisInterfaceMap.get(isisUpdatedInterface.interfaceIndex());
                if (isisInterface == null) {
                    isisInterfaceMap.put(isisUpdatedInterface.interfaceIndex(), isisUpdatedInterface);
                    interfaceIps.add(isisUpdatedInterface.interfaceIpAddress());
                } else {
                    if (!isisInterface.intermediateSystemName().equals(isisUpdatedInterface.intermediateSystemName())) {
                        isisInterface.setIntermediateSystemName(isisUpdatedInterface.intermediateSystemName());
                    }
                    if (isisInterface.reservedPacketCircuitType() != isisUpdatedInterface.reservedPacketCircuitType()) {
                        isisInterface.setReservedPacketCircuitType(isisUpdatedInterface.reservedPacketCircuitType());
                        isisInterface.removeNeighbors();
                    }
                    if (!isisInterface.circuitId().equals(isisUpdatedInterface.circuitId())) {
                        isisInterface.setCircuitId(isisUpdatedInterface.circuitId());
                    }
                    if (isisInterface.networkType() != isisUpdatedInterface.networkType()) {
                        isisInterface.setNetworkType(isisUpdatedInterface.networkType());
                        isisInterface.removeNeighbors();
                    }
                    if (!isisInterface.areaAddress().equals(isisUpdatedInterface.areaAddress())) {
                        isisInterface.setAreaAddress(isisUpdatedInterface.areaAddress());
                    }
                    if (isisInterface.holdingTime() != isisUpdatedInterface.holdingTime()) {
                        isisInterface.setHoldingTime(isisUpdatedInterface.holdingTime());
                    }
                    if (isisInterface.helloInterval() != isisUpdatedInterface.helloInterval()) {
                        isisInterface.setHelloInterval(isisUpdatedInterface.helloInterval());
                        isisInterface.stopHelloSender();
                        isisInterface.startHelloSender(channel);
                    }

                    isisInterfaceMap.put(isisInterface.interfaceIndex(), isisInterface);
                }
            }
        }
    }

    /**
     * Initializes the interface with all interface ip details.
     */
    public void initializeInterfaceIpList() {
        for (IsisProcess process : processes) {
            for (IsisInterface isisInterface : process.isisInterfaceList()) {
                ((DefaultIsisInterface) isisInterface).setAllConfiguredInterfaceIps(interfaceIps);
            }
        }
    }

    /**
     * Initialize channel, start hello sender and initialize LSDB.
     */
    private void initialize() {
        log.debug("IsisChannelHandler initialize..!!!");
        if (configPacket != null) {
            log.debug("IsisChannelHandler initialize -> sentConfig packet of length ::"
                              + configPacket.length);
            sentConfigPacket(configPacket);
        }
        initializeInterfaceMap();
        //start the hello timer
        startHelloSender();
        //Initialize Database
        isisLsdb.initializeDb();
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent evt) throws Exception {
        log.info("ISIS channelConnected from {}", evt.getChannel().getRemoteAddress());
        this.channel = evt.getChannel();
        initialize();
    }

    @Override
    public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent evt) {
        log.debug("IsisChannelHandler::channelDisconnected...!!!");
        if (controller != null) {
            controller.connectPeer();
            stopHelloSender();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        if (e.getCause() instanceof ReadTimeoutException) {
            log.debug("Disconnecting device {} due to read timeout", e.getChannel().getRemoteAddress());
            return;
        } else if (e.getCause() instanceof ClosedChannelException) {
            log.debug("Channel for ISIS {} already closed", e.getChannel().getRemoteAddress());
        } else if (e.getCause() instanceof IOException) {
            log.debug("Disconnecting ISIS {} due to IO Error: {}", e.getChannel().getRemoteAddress(),
                      e.getCause().getMessage());
        } else if (e.getCause() instanceof IsisParseException) {
            IsisParseException errMsg = (IsisParseException) e.getCause();
            byte errorCode = errMsg.errorCode();
            byte errorSubCode = errMsg.errorSubCode();
            log.debug("Error while parsing message from ISIS {}, ErrorCode {}",
                      e.getChannel().getRemoteAddress(), errorCode);
        } else if (e.getCause() instanceof RejectedExecutionException) {
            log.debug("Could not process message: queue full");
        } else {
            log.debug("Error while processing message from ISIS {}, {}",
                      e.getChannel().getRemoteAddress(), e.getCause().getMessage());
        }
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        log.debug("IsisChannelHandler::messageReceived...!!!");
        Object message = e.getMessage();
        if (message instanceof List) {
            List<IsisMessage> isisMessageList = (List<IsisMessage>) message;
            log.debug("IsisChannelHandler::List of IsisMessages Size {}", isisMessageList.size());
            if (isisMessageList != null) {
                for (IsisMessage isisMessage : isisMessageList) {
                    processIsisMessage(isisMessage, ctx);
                }
            } else {
                log.debug("IsisChannelHandler::IsisMessages Null List...!!");
            }
        }
        if (message instanceof IsisMessage) {
            IsisMessage isisMessage = (IsisMessage) message;
            log.debug("IsisChannelHandler::IsisMessages received...!!");
            processIsisMessage(isisMessage, ctx);
        }
    }

    /**
     * When an ISIS message received it is handed over to this method.
     * Based on the type of the ISIS message received it will be handed over
     * to corresponding message handler methods.
     *
     * @param isisMessage received ISIS message
     * @param ctx         channel handler context instance.
     * @throws Exception might throws exception
     */
    public void processIsisMessage(IsisMessage isisMessage, ChannelHandlerContext ctx) throws Exception {
        log.debug("IsisChannelHandler::processIsisMessage...!!!");
        int interfaceIndex = isisMessage.interfaceIndex();
        IsisInterface isisInterface = isisInterfaceMap.get(interfaceIndex);
        isisInterface.processIsisMessage(isisMessage, isisLsdb, channel);
    }

    /**
     * Starts the hello timer which sends hello packet every configured seconds.
     */
    public void startHelloSender() {
        log.debug("IsisController::startHelloSender");
        Set<Integer> interfaceIndexes = isisInterfaceMap.keySet();
        for (Integer interfaceIndex : interfaceIndexes) {
            IsisInterface isisInterface = isisInterfaceMap.get(interfaceIndex);
            isisInterface.startHelloSender(channel);
        }
    }

    /**
     * Stops the hello timer.
     */
    public void stopHelloSender() {
        log.debug("ISISChannelHandler::stopHelloTimer ");
        log.debug("IsisController::startHelloSender");
        Set<Integer> interfaceIndexes = isisInterfaceMap.keySet();
        for (Integer interfaceIndex : interfaceIndexes) {
            IsisInterface isisInterface = isisInterfaceMap.get(interfaceIndex);
            isisInterface.stopHelloSender();
        }
    }

    /**
     * Sends the interface configuration packet to server.
     *
     * @param configPacket interface configuration
     */
    public void sentConfigPacket(byte[] configPacket) {
        if (channel != null && channel.isConnected() && channel.isOpen()) {
            channel.write(configPacket);
            log.debug("IsisChannelHandler sentConfigPacket packet sent..!!!");
        } else {
            log.debug("IsisChannelHandler sentConfigPacket channel not connected - re try..!!!");
            this.configPacket = configPacket;
        }
    }
}