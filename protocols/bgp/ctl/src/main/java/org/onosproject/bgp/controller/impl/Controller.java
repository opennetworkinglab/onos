/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.bgp.controller.impl;

import static org.onlab.util.Tools.groupedThreads;
import io.netty.util.internal.PlatformDependent;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.onosproject.bgp.controller.BgpController;
import org.onosproject.bgpio.protocol.BgpFactories;
import org.onosproject.bgpio.protocol.BgpFactory;
import org.onosproject.bgpio.protocol.BgpVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main controller class. Handles all setup and network listeners - Distributed ownership control of bgp peer
 * through IControllerRegistryService
 */
public class Controller {

    private static final Logger log = LoggerFactory.getLogger(Controller.class);

    private static final BgpFactory FACTORY4 = BgpFactories.getFactory(BgpVersion.BGP_4);

    private ChannelGroup cg;
    private Channel serverChannel;

    // Configuration options
    protected static final short BGP_PORT_NUM = 179;
    private static final short BGP_PRIVILEGED_PORT = 1790; // server port used for non root users in linux
    private static final short PORT_NUM_ZERO = 0;
    private static boolean isPortNumSet = false;
    private static short portNumber = BGP_PORT_NUM;
    private final int workerThreads = 16;
    private final int peerWorkerThreads = 16;

    // Start time of the controller
    private long systemStartTime;

    private NioServerSocketChannelFactory serverExecFactory;
    private NioClientSocketChannelFactory peerExecFactory;
    private static ClientBootstrap peerBootstrap;
    private BgpController bgpController;

    // Perf. related configuration
    private static final int SEND_BUFFER_SIZE = 4 * 1024 * 1024;

    /**
     * Constructor to initialize the values.
     *
     * @param bgpController bgp controller instance
     */
    public Controller(BgpController bgpController) {
        this.bgpController = bgpController;
    }

    /**
     * Returns factory version for processing BGP messages.
     *
     * @return instance of factory version
     */
    static BgpFactory getBgpMessageFactory4() {
        return FACTORY4;
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
     * Tell controller that we're ready to accept bgp peer connections.
     */
    public void run() {

        try {

            peerBootstrap = createPeerBootStrap();

            peerBootstrap.setOption("reuseAddr", true);
            peerBootstrap.setOption("child.keepAlive", true);
            peerBootstrap.setOption("child.tcpNoDelay", true);
            peerBootstrap.setOption("child.sendBufferSize", Controller.SEND_BUFFER_SIZE);

            final ServerBootstrap bootstrap = createServerBootStrap();

            bootstrap.setOption("reuseAddr", true);
            bootstrap.setOption("child.keepAlive", true);
            bootstrap.setOption("child.tcpNoDelay", true);
            bootstrap.setOption("child.sendBufferSize", Controller.SEND_BUFFER_SIZE);

            ChannelPipelineFactory pfact = new BgpPipelineFactory(bgpController, true);

            bootstrap.setPipelineFactory(pfact);
            InetSocketAddress sa = new InetSocketAddress(getBgpPortNum());
            cg = new DefaultChannelGroup();
            serverChannel = bootstrap.bind(sa);
            cg.add(serverChannel);
            log.info("Listening for Peer connection on {}", sa);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Creates server boot strap.
     *
     * @return ServerBootStrap
     */
    private ServerBootstrap createServerBootStrap() {

        if (workerThreads == 0) {
            serverExecFactory = new NioServerSocketChannelFactory(
                                    Executors.newCachedThreadPool(groupedThreads("onos/bgp", "boss-%d")),
                                    Executors.newCachedThreadPool(groupedThreads("onos/bgp", "worker-%d")));
            return new ServerBootstrap(serverExecFactory);
        } else {
            serverExecFactory = new NioServerSocketChannelFactory(
                                    Executors.newCachedThreadPool(groupedThreads("onos/bgp", "boss-%d")),
                                    Executors.newCachedThreadPool(groupedThreads("onos/bgp", "worker-%d")),
                                                                  workerThreads);
            return new ServerBootstrap(serverExecFactory);
        }
    }

    /**
     * Creates peer boot strap.
     *
     * @return ClientBootstrap
     */
    private ClientBootstrap createPeerBootStrap() {

        if (peerWorkerThreads == 0) {
            peerExecFactory = new NioClientSocketChannelFactory(
                              Executors.newCachedThreadPool(groupedThreads("onos/bgp", "boss-%d")),
                             Executors.newCachedThreadPool(groupedThreads("onos/bgp", "worker-%d")));
            return new ClientBootstrap(peerExecFactory);
        } else {
            peerExecFactory = new NioClientSocketChannelFactory(
                              Executors.newCachedThreadPool(groupedThreads("onos/bgp",  "boss-%d")),
                              Executors.newCachedThreadPool(groupedThreads("onos/bgp", "worker-%d")),
                                                                          peerWorkerThreads);
            return new ClientBootstrap(peerExecFactory);
        }
    }

    /**
     * Gets peer bootstrap.
     *
     * @return peer  bootstrap
     */
    public static ClientBootstrap peerBootstrap() {
        return peerBootstrap;
    }

    /**
     * Initialize internal data structures.
     */
    public void init() {
        // These data structures are initialized here because other
        // module's startUp() might be called before ours
        this.systemStartTime = System.currentTimeMillis();
    }

    /**
     * Gets run time memory.
     *
     * @return m run time memory
     */
    public Map<String, Long> getMemory() {
        Map<String, Long> m = new HashMap<>();
        Runtime runtime = Runtime.getRuntime();
        m.put("total", runtime.totalMemory());
        m.put("free", runtime.freeMemory());
        return m;
    }

    /**
     * Gets UP time.
     *
     * @return UP time
     */
    public Long getUptime() {
        RuntimeMXBean rb = ManagementFactory.getRuntimeMXBean();
        return rb.getUptime();
    }

    /**
     * Starts the BGP controller.
     */
    public void start() {
        log.info("Started");
        if (!PlatformDependent.isWindows() && !PlatformDependent.maybeSuperUser()) {
            portNumber = BGP_PRIVILEGED_PORT;
        } else {
            portNumber = BGP_PORT_NUM;
        }
        this.init();
        this.run();
    }

    /**
     * Stops the BGP controller.
     */
    public void stop() {
        log.info("Stopped");
        serverExecFactory.shutdown();
        peerExecFactory.shutdown();
        cg.close();
    }

    /**
     * Returns port number.
     *
     * @return port number
     */
    public static short getBgpPortNum() {
        if (isPortNumSet) {
            return PORT_NUM_ZERO;
        }

        return portNumber;
    }

    /**
     * sets the isPortNumSet as true.
     */
    public void setBgpPortNum() {
        isPortNumSet = true;
    }
}
