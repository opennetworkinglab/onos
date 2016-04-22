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

import com.fasterxml.jackson.databind.JsonNode;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.AdaptiveReceiveBufferSizePredictor;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.FixedReceiveBufferSizePredictorFactory;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.MacAddress;
import org.onlab.packet.TpPort;
import org.onosproject.isis.controller.IsisInterface;
import org.onosproject.isis.controller.IsisNetworkType;
import org.onosproject.isis.controller.IsisProcess;
import org.onosproject.isis.controller.IsisRouterType;
import org.onosproject.isis.io.util.IsisConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import static org.onlab.util.Tools.groupedThreads;

/**
 * Representation of an ISIS controller.
 */
public class Controller {
    protected static final int BUFFER_SIZE = 4 * 1024 * 1024;
    private static final Logger log = LoggerFactory.getLogger(Controller.class);
    private final int peerWorkerThreads = 16;
    private List<IsisProcess> processes = null;
    private IsisChannelHandler isisChannelHandler;
    private NioClientSocketChannelFactory peerExecFactory;
    private ClientBootstrap peerBootstrap = null;
    private TpPort isisPort = TpPort.tpPort(IsisConstants.SPORT);

    /**
     * Deactivates ISIS controller.
     */
    public void isisDeactivate() {
        peerExecFactory.shutdown();
    }

    /**
     * Updates the processes configuration.
     *
     * @param jsonNode json node instance
     * @throws Exception might throws parse exception
     */
    public void updateConfig(JsonNode jsonNode) throws Exception {
        log.debug("Controller::UpdateConfig called");
        byte[] configPacket = new byte[IsisConstants.CONFIG_LENGTH];
        byte numberOfInterface = 0; // number of interfaces to configure

        configPacket[0] = (byte) 0xFF; // its a conf packet - identifier
        List<IsisProcess> isisProcesses = getConfig(jsonNode);

        for (IsisProcess isisProcess : isisProcesses) {
            log.debug("IsisProcessDetails : " + isisProcess);
            for (IsisInterface isisInterface : isisProcess.isisInterfaceList()) {
                DefaultIsisInterface isisInterfaceImpl = (DefaultIsisInterface) isisInterface;
                log.debug("IsisInterfaceDetails : " + isisInterface);
                numberOfInterface++;
                configPacket[2 * numberOfInterface] = (byte) isisInterfaceImpl.interfaceIndex();
                if (isisInterface.networkType() == IsisNetworkType.BROADCAST &&
                        isisInterfaceImpl.reservedPacketCircuitType() == IsisRouterType.L1.value()) {
                    configPacket[(2 * numberOfInterface) + 1] = (byte) 0;
                } else if (isisInterface.networkType() == IsisNetworkType.BROADCAST &&
                        isisInterfaceImpl.reservedPacketCircuitType() == IsisRouterType.L2.value()) {
                    configPacket[(2 * numberOfInterface) + 1] = (byte) 1;
                } else if (isisInterface.networkType() == IsisNetworkType.P2P) {
                    configPacket[(2 * numberOfInterface) + 1] = (byte) 2;
                } else if (isisInterface.networkType() == IsisNetworkType.BROADCAST &&
                        isisInterfaceImpl.reservedPacketCircuitType() == IsisRouterType.L1L2.value()) {
                    configPacket[(2 * numberOfInterface) + 1] = (byte) 3;
                }
            }
        }
        configPacket[1] = numberOfInterface;
        //First time configuration
        if (processes == null) {
            processes = isisProcesses;
            //Initialize connection by creating a channel handler instance and sent the config packet);
            initConnection();
            //Initializing the interface map in channel handler
            isisChannelHandler.initializeInterfaceMap();
        } else {
            isisChannelHandler.updateInterfaceMap(isisProcesses);
        }
        //Send the config packet
        isisChannelHandler.sentConfigPacket(configPacket);
    }

    /**
     * Initializes the netty client channel connection.
     */
    private void initConnection() {
        if (peerBootstrap != null) {
            return;
        }
        peerBootstrap = createPeerBootStrap();

        peerBootstrap.setOption("reuseAddress", true);
        peerBootstrap.setOption("tcpNoDelay", true);
        peerBootstrap.setOption("keepAlive", true);
        peerBootstrap.setOption("receiveBufferSize", Controller.BUFFER_SIZE);
        peerBootstrap.setOption("receiveBufferSizePredictorFactory",
                                new FixedReceiveBufferSizePredictorFactory(
                                        Controller.BUFFER_SIZE));
        peerBootstrap.setOption("receiveBufferSizePredictor",
                                new AdaptiveReceiveBufferSizePredictor(64, 1024, 65536));
        peerBootstrap.setOption("child.keepAlive", true);
        peerBootstrap.setOption("child.tcpNoDelay", true);
        peerBootstrap.setOption("child.sendBufferSize", Controller.BUFFER_SIZE);
        peerBootstrap.setOption("child.receiveBufferSize", Controller.BUFFER_SIZE);
        peerBootstrap.setOption("child.receiveBufferSizePredictorFactory",
                                new FixedReceiveBufferSizePredictorFactory(
                                        Controller.BUFFER_SIZE));
        peerBootstrap.setOption("child.reuseAddress", true);

        isisChannelHandler = new IsisChannelHandler(this, processes);
        ChannelPipelineFactory pfact = new IsisPipelineFactory(isisChannelHandler);
        peerBootstrap.setPipelineFactory(pfact);
        ChannelFuture connection = peerBootstrap.connect(new InetSocketAddress(IsisConstants.SHOST, isisPort.toInt()));
    }

    /**
     * Creates peer boot strap.
     *
     * @return client bootstrap instance
     */
    private ClientBootstrap createPeerBootStrap() {

        if (peerWorkerThreads == 0) {
            peerExecFactory = new NioClientSocketChannelFactory(
                    Executors.newCachedThreadPool(groupedThreads("onos/isis", "boss-%d")),
                    Executors.newCachedThreadPool(groupedThreads("onos/isis", "worker-%d")));
            return new ClientBootstrap(peerExecFactory);
        } else {
            peerExecFactory = new NioClientSocketChannelFactory(
                    Executors.newCachedThreadPool(groupedThreads("onos/isis", "boss-%d")),
                    Executors.newCachedThreadPool(groupedThreads("onos/isis", "worker-%d")),
                    peerWorkerThreads);
            return new ClientBootstrap(peerExecFactory);
        }
    }

    /**
     * Gets all configured processes.
     *
     * @return all configured processes
     */
    public List<IsisProcess> getAllConfiguredProcesses() {
        return processes;
    }

    /**
     * Gets the list of processes configured.
     *
     * @param json posted json
     * @return list of processes configured
     */
    private List<IsisProcess> getConfig(JsonNode json) throws Exception {

        List<IsisProcess> isisProcessesList = new ArrayList<>();
        JsonNode jsonNodes = json;

        if (jsonNodes == null) {
            return isisProcessesList;
        }
        jsonNodes.forEach(jsonNode -> {
            List<IsisInterface> interfaceList = new ArrayList<>();
            for (JsonNode jsonNode1 : jsonNode.path(IsisConstants.INTERFACE)) {
                IsisInterface isisInterface = new DefaultIsisInterface();
                isisInterface.setInterfaceIndex(jsonNode1.path(IsisConstants.INTERFACEINDEX).asInt());
                isisInterface.setInterfaceIpAddress(Ip4Address.valueOf(jsonNode1
                                                                               .path(IsisConstants.INTERFACEIP)
                                                                               .asText()));
                try {
                    isisInterface.setNetworkMask(InetAddress.getByName((jsonNode1
                            .path(IsisConstants.NETWORKMASK).asText())).getAddress());
                } catch (UnknownHostException e) {
                    log.debug("Error:: Parsing network mask");
                }
                isisInterface.setInterfaceMacAddress(MacAddress.valueOf(jsonNode1
                                                                                .path(IsisConstants.MACADDRESS)
                                                                                .asText()));
                isisInterface.setIntermediateSystemName(jsonNode1
                                                                .path(IsisConstants.INTERMEDIATESYSTEMNAME)
                                                                .asText());
                isisInterface.setSystemId(jsonNode1.path(IsisConstants.SYSTEMID).asText());
                isisInterface.setReservedPacketCircuitType(jsonNode1
                                                                   .path(IsisConstants.RESERVEDPACKETCIRCUITTYPE)
                                                                   .asInt());
                if (isisInterface.reservedPacketCircuitType() == IsisRouterType.L1.value()) {
                    isisInterface.setL1LanId(jsonNode1.path(IsisConstants.LANID).asText());
                }
                isisInterface.setIdLength(jsonNode1.path(IsisConstants.IDLENGTH).asInt());
                isisInterface.setMaxAreaAddresses(jsonNode1.path(IsisConstants.MAXAREAADDRESSES).asInt());
                isisInterface.setNetworkType(IsisNetworkType.get(jsonNode1
                                                                         .path(IsisConstants.NETWORKTYPE)
                                                                         .asInt()));
                isisInterface.setAreaAddress(jsonNode1.path(IsisConstants.AREAADDRESS).asText());
                isisInterface.setAreaLength(jsonNode1.path(IsisConstants.AREALENGTH).asInt());
                isisInterface.setLspId(jsonNode1.path(IsisConstants.LSPID).asText());
                isisInterface.setCircuitId(jsonNode1.path(IsisConstants.CIRCUITID).asText());
                isisInterface.setHoldingTime(jsonNode1.path(IsisConstants.HOLDINGTIME).asInt());
                isisInterface.setPriority(jsonNode1.path(IsisConstants.PRIORITY).asInt());
                isisInterface.setHelloInterval(jsonNode1.path(IsisConstants.HELLOINTERVAL).asInt());
                interfaceList.add(isisInterface);
            }
            IsisProcess process = new DefaultIsisProcess();
            process.setProcessId(jsonNode.path(IsisConstants.PROCESSESID).asText());
            process.setIsisInterfaceList(interfaceList);
            isisProcessesList.add(process);
        });

        return isisProcessesList;
    }
}