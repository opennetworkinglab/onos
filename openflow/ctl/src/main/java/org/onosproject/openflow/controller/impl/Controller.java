/*
 * Copyright 2014-2015 Open Networking Laboratory
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

package org.onosproject.openflow.controller.impl;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.onlab.util.ItemNotFoundException;
import org.onosproject.net.DeviceId;
import org.onosproject.net.driver.DefaultDriverData;
import org.onosproject.net.driver.DefaultDriverHandler;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.DriverService;
import org.onosproject.openflow.controller.Dpid;
import org.onosproject.openflow.controller.driver.OpenFlowAgent;
import org.onosproject.openflow.controller.driver.OpenFlowSwitchDriver;
import org.projectfloodlight.openflow.protocol.OFDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.openflow.controller.Dpid.uri;


/**
 * The main controller class.  Handles all setup and network listeners
 * - Distributed ownership control of switch through IControllerRegistryService
 */
public class Controller {

    protected static final Logger log = LoggerFactory.getLogger(Controller.class);

    protected static final OFFactory FACTORY13 = OFFactories.getFactory(OFVersion.OF_13);
    protected static final OFFactory FACTORY10 = OFFactories.getFactory(OFVersion.OF_10);

    protected HashMap<String, String> controllerNodeIPsCache;

    private ChannelGroup cg;

    // Configuration options
    protected int openFlowPort = 6633;
    protected int workerThreads = 0;

    // Start time of the controller
    protected long systemStartTime;

    private OpenFlowAgent agent;

    private NioServerSocketChannelFactory execFactory;

    // Perf. related configuration
    protected static final int SEND_BUFFER_SIZE = 4 * 1024 * 1024;
    private DriverService driverService;

    // ***************
    // Getters/Setters
    // ***************

    public OFFactory getOFMessageFactory10() {
        return FACTORY10;
    }


    public OFFactory getOFMessageFactory13() {
        return FACTORY13;
    }


    public Map<String, String> getControllerNodeIPs() {
        // We return a copy of the mapping so we can guarantee that
        // the mapping return is the same as one that will be (or was)
        // dispatched to IHAListeners
        HashMap<String, String> retval = new HashMap<>();
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
                    new OpenflowPipelineFactory(this, null);
            bootstrap.setPipelineFactory(pfact);
            InetSocketAddress sa = new InetSocketAddress(openFlowPort);
            cg = new DefaultChannelGroup();
            cg.add(bootstrap.bind(sa));

            log.info("Listening for switch connections on {}", sa);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private ServerBootstrap createServerBootStrap() {

        if (workerThreads == 0) {
            execFactory = new NioServerSocketChannelFactory(
                    Executors.newCachedThreadPool(groupedThreads("onos/of", "boss-%d")),
                    Executors.newCachedThreadPool(groupedThreads("onos/of", "worker-%d")));
            return new ServerBootstrap(execFactory);
        } else {
            execFactory = new NioServerSocketChannelFactory(
                    Executors.newCachedThreadPool(groupedThreads("onos/of", "boss-%d")),
                    Executors.newCachedThreadPool(groupedThreads("onos/of", "worker-%d")), workerThreads);
            return new ServerBootstrap(execFactory);
        }
    }

    public void setConfigParams(Map<String, String> configParams) {
        String ofPort = configParams.get("openflowport");
        if (ofPort != null) {
            this.openFlowPort = Integer.parseInt(ofPort);
        }

        log.debug("OpenFlow port set to {}", this.openFlowPort);
        String threads = configParams.get("workerthreads");
        this.workerThreads = threads != null ? Integer.parseInt(threads) : 16;
        log.debug("Number of worker threads set to {}", this.workerThreads);
    }


    /**
     * Initialize internal data structures.
     */
    public void init() {
        // These data structures are initialized here because other
        // module's startUp() might be called before ours
        this.controllerNodeIPsCache = new HashMap<>();

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
     * Forward to the driver-manager to get an IOFSwitch instance.
     *
     * @param dpid data path id
     * @param desc switch description
     * @param ofv  OpenFlow version
     * @return switch instance
     */
    protected OpenFlowSwitchDriver getOFSwitchInstance(long dpid,
                                                       OFDescStatsReply desc,
                                                       OFVersion ofv) {
        Dpid dpidObj = new Dpid(dpid);

        Driver driver;
        try {
             driver = driverService.getDriver(DeviceId.deviceId(Dpid.uri(dpidObj)));
        } catch (ItemNotFoundException e) {
            driver = driverService.getDriver(desc.getMfrDesc(), desc.getHwDesc(), desc.getSwDesc());
        }

        if (driver != null && driver.hasBehaviour(OpenFlowSwitchDriver.class)) {
            Dpid did = new Dpid(dpid);
            DefaultDriverHandler handler =
                    new DefaultDriverHandler(new DefaultDriverData(driver, deviceId(uri(did))));
            OpenFlowSwitchDriver ofSwitchDriver =
                    driver.createBehaviour(handler, OpenFlowSwitchDriver.class);
            ofSwitchDriver.init(did, desc, ofv);
            ofSwitchDriver.setAgent(agent);
            ofSwitchDriver.setRoleHandler(new RoleManager(ofSwitchDriver));
            log.info("OpenFlow handshaker found for device {}: {}", dpid, ofSwitchDriver);
            return ofSwitchDriver;
        }
        log.error("No OpenFlow driver for {} : {}", dpid, desc);
        return null;

    }

    public void start(OpenFlowAgent ag, DriverService driverService) {
        log.info("Starting OpenFlow IO");
        this.agent = ag;
        this.driverService = driverService;
        this.init();
        this.run();
    }


    public void stop() {
        log.info("Stopping OpenFlow IO");
        cg.close();
        execFactory.shutdown();
    }

}
