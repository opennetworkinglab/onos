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

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.AdaptiveReceiveBufferSizePredictor;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.FixedReceiveBufferSizePredictorFactory;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.TpPort;
import org.onosproject.net.driver.DriverService;
import org.onosproject.ospf.controller.OspfAgent;
import org.onosproject.ospf.controller.OspfArea;
import org.onosproject.ospf.controller.OspfInterface;
import org.onosproject.ospf.controller.OspfLinkTed;
import org.onosproject.ospf.controller.OspfProcess;
import org.onosproject.ospf.controller.OspfRouter;
import org.onosproject.ospf.protocol.util.OspfUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.onlab.util.Tools.groupedThreads;

/**
 * Representation of an OSPF controller.
 */
public class Controller {
    protected static final int BUFFER_SIZE = 4 * 1024 * 1024;
    private static final Logger log = LoggerFactory.getLogger(Controller.class);
    private static final int RETRY_INTERVAL = 4;
    private final int peerWorkerThreads = 16;
    protected long systemStartTime;
    byte[] configPacket = null;
    private List<OspfProcess> processes = null;
    private OspfInterfaceChannelHandler ospfChannelHandler;
    private NioClientSocketChannelFactory peerExecFactory;
    private ClientBootstrap peerBootstrap = null;
    private TpPort ospfPort = TpPort.tpPort(OspfUtil.SPORT);
    private ScheduledExecutorService connectExecutor = null;
    private int connectRetryCounter = 0;
    private int connectRetryTime;
    private DriverService driverService;
    private OspfAgent agent;

    /**
     * Deactivates OSPF controller.
     */
    public void ospfDeactivate() {
        peerExecFactory.shutdown();
    }

    /**
     * Updates the processes configuration.
     *
     * @param ospfProcesses list of OSPF process instances
     * @throws Exception might throws parse exception
     */
    public void updateConfig(List<OspfProcess> ospfProcesses) throws Exception {
        log.debug("Controller::UpdateConfig called");
        configPacket = new byte[OspfUtil.CONFIG_LENGTH];
        byte numberOfInterface = 0; // number of interfaces to configure
        configPacket[0] = (byte) 0xFF; // its a conf packet - identifier
        for (OspfProcess ospfProcess : ospfProcesses) {
            log.debug("OspfProcessDetails : " + ospfProcess);
            for (OspfArea ospfArea : ospfProcess.areas()) {
                for (OspfInterface ospfInterface : ospfArea.ospfInterfaceList()) {
                    log.debug("OspfInterfaceDetails : " + ospfInterface);
                    numberOfInterface++;
                    configPacket[2 * numberOfInterface] = (byte) ospfInterface.interfaceIndex();
                    configPacket[(2 * numberOfInterface) + 1] = (byte) 4;
                }
            }
        }
        configPacket[1] = numberOfInterface;
        //First time configuration
        if (processes == null) {
            if (ospfProcesses.size() > 0) {
                processes = ospfProcesses;
                connectPeer();
            }
        } else {
            ospfChannelHandler.updateInterfaceMap(ospfProcesses);
            //Send the config packet
            ospfChannelHandler.sentConfigPacket(configPacket);
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
                                new AdaptiveReceiveBufferSizePredictor(64, 4096, 65536));
        peerBootstrap.setOption("child.keepAlive", true);
        peerBootstrap.setOption("child.tcpNoDelay", true);
        peerBootstrap.setOption("child.sendBufferSize", Controller.BUFFER_SIZE);
        peerBootstrap.setOption("child.receiveBufferSize", Controller.BUFFER_SIZE);
        peerBootstrap.setOption("child.receiveBufferSizePredictorFactory",
                                new FixedReceiveBufferSizePredictorFactory(
                                        Controller.BUFFER_SIZE));
        peerBootstrap.setOption("child.reuseAddress", true);

        ospfChannelHandler = new OspfInterfaceChannelHandler(this, processes);
        ChannelPipelineFactory pfact = new OspfPipelineFactory(ospfChannelHandler);
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
                    Executors.newCachedThreadPool(groupedThreads("onos/ospf", "boss-%d")),
                    Executors.newCachedThreadPool(groupedThreads("onos/ospf", "worker-%d")));
            return new ClientBootstrap(peerExecFactory);
        } else {
            peerExecFactory = new NioClientSocketChannelFactory(
                    Executors.newCachedThreadPool(groupedThreads("onos/ospf", "boss-%d")),
                    Executors.newCachedThreadPool(groupedThreads("onos/ospf", "worker-%d")),
                    peerWorkerThreads);
            return new ClientBootstrap(peerExecFactory);
        }
    }

    /**
     * Gets all configured processes.
     *
     * @return all configured processes
     */
    public List<OspfProcess> getAllConfiguredProcesses() {
        return processes;
    }

    /**
     * Adds device details.
     *
     * @param ospfRouter OSPF router instance
     */
    public void addDeviceDetails(OspfRouter ospfRouter) {
        agent.addConnectedRouter(ospfRouter);
    }

    /**
     * Removes device details.
     *
     * @param ospfRouter OSPF router instance
     */
    public void removeDeviceDetails(OspfRouter ospfRouter) {
        agent.removeConnectedRouter(ospfRouter);
    }

    /**
     * Adds link details.
     *
     * @param ospfRouter  OSPF router instance
     * @param ospfLinkTed OSPF link ted instance
     */
    public void addLinkDetails(OspfRouter ospfRouter, OspfLinkTed ospfLinkTed) {
        agent.addLink(ospfRouter, ospfLinkTed);
    }

    /**
     * Removes link details.
     *
     * @param ospfRouter  OSPF router instance
     * @param ospfLinkTed OSPF link ted instance
     */
    public void removeLinkDetails(OspfRouter ospfRouter, OspfLinkTed ospfLinkTed) {
        agent.deleteLink(ospfRouter, ospfLinkTed);
    }

    /**
     * Initializes internal data structures.
     */
    public void init() {
        this.systemStartTime = System.currentTimeMillis();
    }

    /**
     * Starts the controller.
     *
     * @param ag            OSPF agent instance
     * @param driverService driver service instance
     */
    public void start(OspfAgent ag, DriverService driverService) {
        log.info("Starting OSPF controller...!!!");
        this.agent = ag;
        this.driverService = driverService;
        this.init();
    }

    /**
     * Stops the Controller.
     */
    public void stop() {
        log.info("Stopping OSPF controller...!!!");
        ospfDeactivate();
        processes.clear();
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
            return OspfUtil.DEFAULTIP;
        }

        return ipAddress;
    }

    /**
     * Returns interface mask by index.
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
     * Disconnects the executor.
     */
    public void disconnectExecutor() {
        if (connectExecutor != null) {
            connectExecutor.shutdown();
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
        if (this.connectExecutor == null) {
            this.connectExecutor = Executors.newSingleThreadScheduledExecutor();
        }
        this.connectExecutor.schedule(new ConnectionRetry(), retryDelay, TimeUnit.MINUTES);
    }

    /**
     * Implements OSPF connection and manages connection to peer with back-off mechanism in case of failure.
     */
    class ConnectionRetry implements Runnable {
        @Override
        public void run() {
            log.debug("Connect to peer {}", OspfUtil.SHOST);
            initConnection();
            ospfChannelHandler.sentConfigPacket(configPacket);
            InetSocketAddress connectToSocket = new InetSocketAddress(OspfUtil.SHOST, ospfPort.toInt());
            try {
                peerBootstrap.connect(connectToSocket).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (!future.isSuccess()) {
                            connectRetryCounter++;
                            log.error("Connection failed, ConnectRetryCounter {} remote host {}", connectRetryCounter,
                                      OspfUtil.SHOST);
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
                            ospfChannelHandler.sentConfigPacket(configPacket);
                            connectRetryCounter++;
                            log.info("Connected to remote host {}, Connect Counter {}", OspfUtil.SHOST,
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