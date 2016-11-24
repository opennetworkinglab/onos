/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.pcep.controller.impl;

import static org.onlab.util.Tools.groupedThreads;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.List;
import java.util.HashMap;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.onosproject.pcep.controller.PccId;
import org.onosproject.pcep.controller.PcepCfg;
import org.onosproject.pcep.controller.PcepPacketStats;
import org.onosproject.pcep.controller.driver.PcepAgent;
import org.onosproject.pcep.controller.driver.PcepClientDriver;
import org.onosproject.pcepio.protocol.PcepFactories;
import org.onosproject.pcepio.protocol.PcepFactory;
import org.onosproject.pcepio.protocol.PcepVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main controller class. Handles all setup and network listeners -
 * Distributed ownership control of pcc through IControllerRegistryService
 */
public class Controller {

    private static final Logger log = LoggerFactory.getLogger(Controller.class);

    private static final PcepFactory FACTORY1 = PcepFactories.getFactory(PcepVersion.PCEP_1);
    private PcepCfg pcepConfig = new PcepConfig();
    private ChannelGroup cg;

    // Configuration options
    private int pcepPort = 4189;
    private int workerThreads = 10;

    // Start time of the controller
    private long systemStartTime;

    private PcepAgent agent;

    private Map<String, String> peerMap = new TreeMap<>();
    private Map<String, List<String>> pcepExceptionMap = new TreeMap<>();
    private Map<Integer, Integer> pcepErrorMsg = new TreeMap<>();
    private Map<String, Byte> sessionMap = new TreeMap<>();
    private LinkedList<String> pcepExceptionList = new LinkedList<String>();

    private NioServerSocketChannelFactory execFactory;

    // Perf. related configuration
    private static final int SEND_BUFFER_SIZE = 4 * 1024 * 1024;

    /**
     * pcep session information.
     *
     * @param peerId id of the peer with which the session is being formed
     * @param status pcep session status
     * @param sessionId pcep session id
     */
    public void peerStatus(String peerId, String status, byte sessionId) {
        if (peerId != null) {
            peerMap.put(peerId, status);
            sessionMap.put(peerId, sessionId);
        } else {
            log.debug("Peer Id is null");
        }
    }

    /**
     * Pcep session exceptions information.
     *
     * @param peerId id of the peer which has generated the exception
     * @param exception pcep session exception
     */
    public void peerExceptions(String peerId, String exception) {
        if (peerId != null) {
            pcepExceptionList.add(exception);
            pcepExceptionMap.put(peerId, pcepExceptionList);
        } else {
            log.debug("Peer Id is null");
        }
        if (pcepExceptionList.size() > 10) {
            pcepExceptionList.clear();
            pcepExceptionList.add(exception);
            pcepExceptionMap.put(peerId, pcepExceptionList);
        }
    }

    /**
     * Create a map of pcep error messages received.
     *
     * @param peerId id of the peer which has sent the error message
     * @param errorType error type of pcep error messgae
     * @param errValue error value of pcep error messgae
     */
    public void peerErrorMsg(String peerId, Integer errorType, Integer errValue) {
        if (peerId == null) {
            pcepErrorMsg.put(errorType, errValue);
        } else {
            if (pcepErrorMsg.size() > 10) {
                pcepErrorMsg.clear();
            }
            pcepErrorMsg.put(errorType, errValue);
        }
    }

    /**
     * Returns the pcep session details.
     *
     * @return pcep session details
     */
    public Map<String, Byte> mapSession() {
        return this.sessionMap;
    }



    /**
     * Returns factory version for processing pcep messages.
     *
     * @return instance of factory version
     */
    public PcepFactory getPcepMessageFactory1() {
        return FACTORY1;
    }

    /**
     * To get system start time.
     *
     * @return system start time in milliseconds
     */
    public long getSystemStartTime() {
        return (this.systemStartTime);
    }

    /**
     * Returns the list of pcep peers with session information.
     *
     * @return pcep peer information
     */
    public  Map<String, String> mapPeer() {
        return this.peerMap;
    }

    /**
     * Returns the list of pcep exceptions per peer.
     *
     * @return pcep exceptions
     */
    public  Map<String, List<String>> exceptionsMap() {
        return this.pcepExceptionMap;
    }

    /**
     * Returns the type and value of pcep error messages.
     *
     * @return pcep error message
     */
    public Map<Integer, Integer> mapErrorMsg() {
        return this.pcepErrorMsg;
    }

    /**
     * Tell controller that we're ready to accept pcc connections.
     */
    public void run() {
        try {
            final ServerBootstrap bootstrap = createServerBootStrap();

            bootstrap.setOption("reuseAddr", true);
            bootstrap.setOption("child.keepAlive", true);
            bootstrap.setOption("child.tcpNoDelay", true);
            bootstrap.setOption("child.sendBufferSize", Controller.SEND_BUFFER_SIZE);

            ChannelPipelineFactory pfact = new PcepPipelineFactory(this);

            bootstrap.setPipelineFactory(pfact);
            InetSocketAddress sa = new InetSocketAddress(pcepPort);
            cg = new DefaultChannelGroup();
            cg.add(bootstrap.bind(sa));
            log.debug("Listening for PCC connection on {}", sa);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates server boot strap.
     *
     * @return ServerBootStrap
     */
    private ServerBootstrap createServerBootStrap() {
        if (workerThreads == 0) {
            execFactory = new NioServerSocketChannelFactory(
                    Executors.newCachedThreadPool(groupedThreads("onos/pcep", "boss-%d")),
                    Executors.newCachedThreadPool(groupedThreads("onos/pcep", "worker-%d")));
            return new ServerBootstrap(execFactory);
        } else {
            execFactory = new NioServerSocketChannelFactory(
                    Executors.newCachedThreadPool(groupedThreads("onos/pcep", "boss-%d")),
                    Executors.newCachedThreadPool(groupedThreads("onos/pcep", "worker-%d")), workerThreads);
            return new ServerBootstrap(execFactory);
        }
    }

    /**
     * Initialize internal data structures.
     */
    public void init() {
        // These data structures are initialized here because other
        // module's startUp() might be called before ours
        this.systemStartTime = System.currentTimeMillis();
    }

    public Map<String, Long> getMemory() {
        Map<String, Long> m = new HashMap<>();
        Runtime runtime = Runtime.getRuntime();
        m.put("total", runtime.totalMemory());
        m.put("free", runtime.freeMemory());
        return m;
    }

    public Long getUptime() {
        RuntimeMXBean rb = ManagementFactory.getRuntimeMXBean();
        return rb.getUptime();
    }

    /**
     * Creates instance of Pcep client.
     *
     * @param pccId pcc identifier
     * @param sessionID session id
     * @param pv pcep version
     * @param pktStats pcep packet statistics
     * @return instance of PcepClient
     */
    protected PcepClientDriver getPcepClientInstance(PccId pccId, int sessionID, PcepVersion pv,
            PcepPacketStats pktStats) {
        PcepClientDriver pcepClientDriver = new PcepClientImpl();
        pcepClientDriver.init(pccId, pv, pktStats);
        pcepClientDriver.setAgent(agent);
        return pcepClientDriver;
    }

    /**
     * Starts the pcep controller.
     *
     * @param ag Pcep agent
     */
    public void start(PcepAgent ag) {
        log.info("Started");
        this.agent = ag;
        this.init();
        this.run();
    }

    /**
     * Stops the pcep controller.
     */
    public void stop() {
        log.info("Stopped");
        execFactory.shutdown();
        cg.close();
    }
}
