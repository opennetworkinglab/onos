/*
 * Copyright 2014 Open Networking Laboratory
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

package org.onosproject.igp.controller.impl;

import static org.onlab.util.Tools.namedThreads;

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
import org.onosproject.igp.controller.driver.IgpAgent;
import org.onosproject.igp.controller.driver.IgpSwitchDriver;
import org.onosproject.igp.drivers.DriverManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The main controller class.  Handles all setup and network listeners
 * - Distributed ownership control of switch through IControllerRegistryService
 */
public class Controller {

    protected static final Logger log = LoggerFactory.getLogger(Controller.class);
    
    protected HashMap<String, String> controllerNodeIPsCache;

    private ChannelGroup cg;

    // Configuration options
    protected int igpPort = 6634;
    protected int workerThreads = 0;

    // Start time of the controller
    protected long systemStartTime;

    private IgpAgent agent;

    private NioServerSocketChannelFactory execFactory;

    // Perf. related configuration
    protected static final int SEND_BUFFER_SIZE = 4 * 1024 * 1024;

    public Map<String, String> getControllerNodeIPs() {
        // We return a copy of the mapping so we can guarantee that
        // the mapping return is the same as one that will be (or was)
        // dispatched to IHAListeners
        HashMap<String, String> retval = new HashMap<String, String>();
        synchronized (controllerNodeIPsCache) {
            retval.putAll(controllerNodeIPsCache);
        }
        return retval;
    }


    public long getSystemStartTime() {
        return (this.systemStartTime);
    }

    // **************
    // Initialization
    // **************

    /**
     * Tell controller that we're ready to accept switches loop.
     */
    public void run() {

        try {
            final ServerBootstrap bootstrap = createServerBootStrap();

            bootstrap.setOption("reuseAddr", true);
            bootstrap.setOption("child.keepAlive", true);
            bootstrap.setOption("child.tcpNoDelay", true);
            bootstrap.setOption("child.sendBufferSize", Controller.SEND_BUFFER_SIZE);

            ChannelPipelineFactory pfact =
                    new IgpPipelineFactory(this, null);
            bootstrap.setPipelineFactory(pfact);
            InetSocketAddress sa = new InetSocketAddress(igpPort);
            cg = new DefaultChannelGroup();
            cg.add(bootstrap.bind(sa));

            log.info("Listening for switch connections on {}", sa);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private ServerBootstrap createServerBootStrap() {

        if (workerThreads == 0) {
            execFactory =  new NioServerSocketChannelFactory(
                    Executors.newCachedThreadPool(namedThreads("Controller-boss-%d")),
                    Executors.newCachedThreadPool(namedThreads("Controller-worker-%d")));
            return new ServerBootstrap(execFactory);
        } else {
            execFactory = new NioServerSocketChannelFactory(
                    Executors.newCachedThreadPool(namedThreads("Controller-boss-%d")),
                    Executors.newCachedThreadPool(namedThreads("Controller-worker-%d")), workerThreads);
            return new ServerBootstrap(execFactory);
        }
    }

    public void setConfigParams(Map<String, String> configParams) {
        String igpPort = configParams.get("openflowport");
        if (igpPort != null) {
            this.igpPort = Integer.parseInt(igpPort);
        }
        log.debug("OpenFlow port set to {}", this.igpPort);
        String threads = configParams.get("workerthreads");
        this.workerThreads = threads != null ? Integer.parseInt(threads) : 16;
        log.debug("Number of worker threads set to {}", this.workerThreads);
    }


    /**
     * Initialize internal data structures.
     * @param configParams configuration parameters
     */
    public void init(Map<String, String> configParams) {
        // These data structures are initialized here because other
        // module's startUp() might be called before ours
        this.controllerNodeIPsCache = new HashMap<String, String>();

        setConfigParams(configParams);
        this.systemStartTime = System.currentTimeMillis();


    }

    public Long getUptime() {
        RuntimeMXBean rb = ManagementFactory.getRuntimeMXBean();
        return rb.getUptime();
    }

    /**
     * Forward to the driver-manager to get an IOFSwitch instance.
     * @param dpid data path id
     * @param desc switch description
     * @param ofv OpenFlow version
     * @return switch instance
     */
    protected IgpSwitchDriver getOFSwitchInstance(int dpid) {
    	IgpSwitchDriver sw = DriverManager.getSwitch(dpid);
        sw.setAgent(agent);
        return sw;
    }

    public void start(IgpAgent ag) {
        log.info("Starting OpenFlow IO");
        this.agent = ag;
        this.init(new HashMap<String, String>());
        this.run();
    }


    public void stop() {
        log.info("Stopping OpenFlow IO");
        execFactory.shutdown();
        cg.close();
    }

}
