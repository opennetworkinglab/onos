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
import org.jboss.netty.channel.ChannelFutureListener;
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
import org.onosproject.isis.controller.topology.IsisAgent;
import org.onosproject.isis.controller.topology.IsisLink;
import org.onosproject.isis.controller.topology.IsisRouter;
import org.onosproject.isis.io.util.IsisConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.onlab.util.Tools.groupedThreads;

/**
 * Representation of an ISIS controller.
 */
public class Controller {
    protected static final int BUFFER_SIZE = 4 * 1024 * 1024;
    private static final Logger log = LoggerFactory.getLogger(Controller.class);
    private static final int RETRY_INTERVAL = 4;
    private final int peerWorkerThreads = 16;
    byte[] configPacket = null;
    private List<IsisProcess> processes = null;
    private IsisChannelHandler isisChannelHandler;
    private NioClientSocketChannelFactory peerExecFactory;
    private ClientBootstrap peerBootstrap = null;
    private TpPort isisPort = TpPort.tpPort(IsisConstants.SPORT);
    private ScheduledExecutorService connectExecutor = null;
    private int connectRetryCounter = 0;
    private int connectRetryTime;
    private ScheduledFuture future = null;
    private IsisAgent agent;

    /**
     * Deactivates ISIS controller.
     */
    public void isisDeactivate() {
        disconnectExecutor();
        processes = null;
        peerExecFactory.shutdown();
    }

    /**
     * Sets ISIS agent.
     *
     * @param agent ISIS agent instance
     */
    public void setAgent(IsisAgent agent) {
        this.agent = agent;
    }


    /**
     * Updates the processes configuration.
     *
     * @param jsonNode json node instance
     * @throws Exception might throws parse exception
     */
    public void updateConfig(JsonNode jsonNode) throws Exception {
        log.debug("Controller::UpdateConfig called");
        configPacket = new byte[IsisConstants.CONFIG_LENGTH];
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
            if (!isisProcesses.isEmpty()) {
                processes = isisProcesses;
                connectPeer();
            }
        } else {
            isisChannelHandler.updateInterfaceMap(isisProcesses);
            //Send the config packet
            isisChannelHandler.sentConfigPacket(configPacket);
        }
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
                String index = jsonNode1.path(IsisConstants.INTERFACEINDEX).asText();
                if (isPrimitive(index)) {
                    int input = Integer.parseInt(index);
                    if (input < 1 || input > 255) {
                        log.debug("Wrong interface index: {}", index);
                        continue;
                    }
                    isisInterface.setInterfaceIndex(Integer.parseInt(index));
                } else {
                    log.debug("Wrong interface index {}", index);
                    continue;
                }
                Ip4Address ipAddress = getInterfaceIp(isisInterface.interfaceIndex());
                if (ipAddress != null && !ipAddress.equals(IsisConstants.DEFAULTIP)) {
                    isisInterface.setInterfaceIpAddress(ipAddress);
                } else {
                    log.debug("Wrong interface index {}. No matching interface in system.", index);
                    continue;
                }
                MacAddress macAddress = getInterfaceMac(isisInterface.interfaceIndex());
                if (macAddress != null) {
                    isisInterface.setInterfaceMacAddress(macAddress);
                } else {
                    log.debug("Wrong interface index {}. No matching interface in system.", index);
                    continue;
                }
                String mask = getInterfaceMask(isisInterface.interfaceIndex());
                if (mask != null) {
                    try {
                        isisInterface.setNetworkMask(InetAddress.getByName(mask).getAddress());
                    } catch (UnknownHostException e) {
                        log.debug("Wrong interface index {}. Error while getting network mask.", index);
                    }
                } else {
                    log.debug("Wrong interface index {}. Error while getting network mask.", index);
                    continue;
                }
                isisInterface.setIntermediateSystemName(jsonNode1
                        .path(IsisConstants.INTERMEDIATESYSTEMNAME)
                        .asText());
                String systemId = jsonNode1.path(IsisConstants.SYSTEMID).asText();
                if (isValidSystemId(systemId)) {
                    isisInterface.setSystemId(systemId);
                } else {
                    log.debug("Wrong systemId: {} for interface index {}.", systemId, index);
                    continue;
                }
                String circuitType = jsonNode1.path(IsisConstants.RESERVEDPACKETCIRCUITTYPE).asText();
                if (isPrimitive(circuitType)) {
                    int input = Integer.parseInt(circuitType);
                    if (input < 1 || input > 3) {
                        log.debug("Wrong ReservedPacketCircuitType: {} for interface index {}.", circuitType, index);
                        continue;
                    }
                    isisInterface.setReservedPacketCircuitType(input);
                } else {
                    log.debug("Wrong ReservedPacketCircuitType: {} for interface index {}.", circuitType, index);
                    continue;
                }
                String networkType = jsonNode1.path(IsisConstants.NETWORKTYPE).asText();
                if (isPrimitive(networkType)) {
                    int input = Integer.parseInt(networkType);
                    if (input < 1 || input > 2) {
                        log.debug("Wrong networkType: {} for interface index {}.", networkType, index);
                        continue;
                    }
                    isisInterface.setNetworkType(IsisNetworkType.get(input));
                } else {
                    log.debug("Wrong networkType: {} for interface index {}.", networkType, index);
                    continue;
                }
                String areaAddress = jsonNode1.path(IsisConstants.AREAADDRESS).asText();
                if (isPrimitive(areaAddress)) {
                    if (areaAddress.length() > 7) {
                        log.debug("Wrong areaAddress: {} for interface index {}.", areaAddress, index);
                        continue;
                    }
                    isisInterface.setAreaAddress(areaAddress);
                } else {
                    log.debug("Wrong areaAddress: {} for interface index {}.", areaAddress, index);
                    continue;
                }
                String circuitId = jsonNode1.path(IsisConstants.CIRCUITID).asText();
                if (isPrimitive(circuitId)) {
                    int input = Integer.parseInt(circuitId);
                    if (input < 1) {
                        log.debug("Wrong circuitId: {} for interface index {}.", circuitId, index);
                        continue;
                    }
                    isisInterface.setCircuitId(circuitId);
                } else {
                    log.debug("Wrong circuitId: {} for interface index {}.", circuitId, index);
                    continue;
                }
                String holdingTime = jsonNode1.path(IsisConstants.HOLDINGTIME).asText();
                if (isPrimitive(holdingTime)) {
                    int input = Integer.parseInt(holdingTime);
                    if (input < 1 || input > 255) {
                        log.debug("Wrong holdingTime: {} for interface index {}.", holdingTime, index);
                        continue;
                    }
                    isisInterface.setHoldingTime(input);
                } else {
                    log.debug("Wrong holdingTime: {} for interface index {}.", holdingTime, index);
                    continue;
                }
                String helloInterval = jsonNode1.path(IsisConstants.HELLOINTERVAL).asText();
                if (isPrimitive(helloInterval)) {
                    int interval = Integer.parseInt(helloInterval);
                    if (interval > 0 && interval <= 255) {
                        isisInterface.setHelloInterval(interval);
                    } else {
                        log.debug("Wrong hello interval: {} for interface index {}.", helloInterval, index);
                        continue;
                    }
                } else {
                    log.debug("Wrong hello interval: {} for interface index {}.", helloInterval, index);
                    continue;
                }
                interfaceList.add(isisInterface);
            }
            if (!interfaceList.isEmpty()) {
                IsisProcess process = new DefaultIsisProcess();
                process.setProcessId(jsonNode.path(IsisConstants.PROCESSESID).asText());
                process.setIsisInterfaceList(interfaceList);
                isisProcessesList.add(process);
            }
        });

        return isisProcessesList;
    }

    /**
     * Returns interface MAC by index.
     *
     * @param interfaceIndex interface index
     * @return interface IP by index
     */
    private MacAddress getInterfaceMac(int interfaceIndex) {
        MacAddress macAddress = null;
        try {
            NetworkInterface networkInterface = NetworkInterface.getByIndex(interfaceIndex);
            macAddress = MacAddress.valueOf(networkInterface.getHardwareAddress());
        } catch (Exception e) {
            log.debug("Error while getting Interface IP by index");
            return macAddress;
        }

        return macAddress;
    }

    /**
     * Returns interface IP by index.
     *
     * @param interfaceIndex interface index
     * @return interface IP by index
     */
    private Ip4Address getInterfaceIp(int interfaceIndex) {
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
            return IsisConstants.DEFAULTIP;
        }
        return ipAddress;
    }

    /**
     * Returns interface MAC by index.
     *
     * @param interfaceIndex interface index
     * @return interface IP by index
     */
    private String getInterfaceMask(int interfaceIndex) {
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
     * Checks if primitive or not.
     *
     * @param value input value
     * @return true if number else false
     */
    private boolean isPrimitive(String value) {
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
     * Checks if system id is valid or not.
     *
     * @param value input value
     * @return true if valid else false
     */
    private boolean isValidSystemId(String value) {
        value = value.trim();
        boolean status = true;
        if (value.length() != 14) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!Character.isDigit(c)) {
                if (!((i == 4 || i == 9) && c == '.')) {
                    status = false;
                    break;
                }
            }
        }

        return status;
    }

    /**
     * Disconnects the executor.
     */
    public void disconnectExecutor() {
        if (connectExecutor != null) {
            future.cancel(true);
            connectExecutor.shutdownNow();
            connectExecutor = null;
        }
    }

    /**
     * Connects to peer.
     */
    public void connectPeer() {
        scheduleConnectionRetry(this.connectRetryTime);
    }

    /**
     * Retry connection with exponential back-off mechanism.
     *
     * @param retryDelay retry delay
     */
    private void scheduleConnectionRetry(long retryDelay) {
        if (connectExecutor == null) {
            connectExecutor = Executors.newSingleThreadScheduledExecutor();
        }
        future = connectExecutor.schedule(new ConnectionRetry(), retryDelay, TimeUnit.MINUTES);
    }

    /**
     * Adds device details.
     *
     * @param isisRouter ISIS router instance
     */
    public void addDeviceDetails(IsisRouter isisRouter) {
        agent.addConnectedRouter(isisRouter);
    }

    /**
     * Removes device details.
     *
     * @param isisRouter Isis router instance
     */
    public void removeDeviceDetails(IsisRouter isisRouter) {
        agent.removeConnectedRouter(isisRouter);
    }

    /**
     * Adds link details.
     *
     * @param isisLink ISIS link instance
     */
    public void addLinkDetails(IsisLink isisLink) {
        agent.addLink(isisLink);
    }

    /**
     * Removes link details.
     *
     * @param isisLink ISIS link instance
     */
    public void removeLinkDetails(IsisLink isisLink) {
        agent.deleteLink(isisLink);
    }

    /**
     * Returns the isisAgent instance.
     *
     * @return agent
     */
    public IsisAgent agent() {
        return this.agent;
    }

    /**
     * Implements ISIS connection and manages connection to peer with back-off mechanism in case of failure.
     */
    class ConnectionRetry implements Runnable {
        @Override
        public void run() {
            log.debug("Connect to peer {}", IsisConstants.SHOST);
            initConnection();
            isisChannelHandler.sentConfigPacket(configPacket);
            InetSocketAddress connectToSocket = new InetSocketAddress(IsisConstants.SHOST, isisPort.toInt());
            try {
                peerBootstrap.connect(connectToSocket).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (!future.isSuccess()) {
                            connectRetryCounter++;
                            log.error("Connection failed, ConnectRetryCounter {} remote host {}", connectRetryCounter,
                                    IsisConstants.SHOST);
                            /*
                             * Reconnect to peer on failure is exponential till 4 mins, later on retry after every 4
                             * mins.
                             */
                            if (connectRetryTime < RETRY_INTERVAL) {
                                connectRetryTime = (connectRetryTime != 0) ? connectRetryTime * 2 : 1;
                            }
                            scheduleConnectionRetry(connectRetryTime);
                        } else {
                            //Send the config packet
                            isisChannelHandler.sentConfigPacket(configPacket);
                            connectRetryCounter++;
                            log.info("Connected to remote host {}, Connect Counter {}", IsisConstants.SHOST,
                                    connectRetryCounter);
                            disconnectExecutor();

                            return;
                        }
                    }
                });
            } catch (Exception e) {
                log.info("Connect peer exception : " + e.toString());
                disconnectExecutor();
            }
        }
    }
}