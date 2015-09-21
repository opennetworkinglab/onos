/*
 * Copyright 2015 Open Networking Laboratory
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

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main controller class. Handles all setup and network listeners - Distributed ownership control of bgp peer
 * through IControllerRegistryService
 */
public class Controller {

    protected static final Logger log = LoggerFactory.getLogger(Controller.class);

    private ChannelGroup cg;

    // Configuration options
    private static final short BGP_PORT_NUM = 179;
    private int workerThreads = 16;

    // Start time of the controller
    protected long systemStartTime;

    private NioServerSocketChannelFactory serverExecFactory;

    // Perf. related configuration
    protected static final int SEND_BUFFER_SIZE = 4 * 1024 * 1024;

    BGPControllerImpl bgpCtrlImpl;

    /**
     * Constructor to initialize parameter.
     *
     * @param bgpCtrlImpl BGP controller Impl instance
     */
    public Controller(BGPControllerImpl bgpCtrlImpl) {
        this.bgpCtrlImpl = bgpCtrlImpl;
    }

    // ***************
    // Getters/Setters
    // ***************

    /**
     * To get system start time.
     *
     * @return system start time in milliseconds
     */
    public long getSystemStartTime() {
        return (this.systemStartTime);
    }

    // **************
    // Initialization
    // **************

    /**
     * Tell controller that we're ready to accept bgp peer connections.
     */
    public void run() {

        try {
            final ServerBootstrap bootstrap = createServerBootStrap();

            bootstrap.setOption("reuseAddr", true);
            bootstrap.setOption("child.keepAlive", true);
            bootstrap.setOption("child.tcpNoDelay", true);
            bootstrap.setOption("child.sendBufferSize", Controller.SEND_BUFFER_SIZE);

            ChannelPipelineFactory pfact = new BGPPipelineFactory(bgpCtrlImpl, true);

            bootstrap.setPipelineFactory(pfact);
            InetSocketAddress sa = new InetSocketAddress(getBgpPortNum());
            cg = new DefaultChannelGroup();
            cg.add(bootstrap.bind(sa));
            log.info("Listening for Peer connection on {}", sa);
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
     * Initialize internal data structures.
     */
    public void init() {
        // These data structures are initialized here because other
        // module's startUp() might be called before ours
        this.systemStartTime = System.currentTimeMillis();
    }

    // **************
    // Utility methods
    // **************

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
     * Starts the BGP controller.
     */
    public void start() {
        log.info("Started");
        this.init();
        this.run();
    }

    /**
     * Stops the BGP controller.
     */
    public void stop() {
        log.info("Stopped");
        serverExecFactory.shutdown();
        cg.close();
    }

    /**
     * Returns port number.
     *
     * @return port number
     */
    public static short getBgpPortNum() {
        return BGP_PORT_NUM;
    }
}