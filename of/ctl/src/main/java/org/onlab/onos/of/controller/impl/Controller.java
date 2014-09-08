/**
 *    Copyright 2011, Big Switch Networks, Inc.
 *    Originally created by David Erickson, Stanford University
 *
 *    Licensed under the Apache License, Version 2.0 (the "License"); you may
 *    not use this file except in compliance with the License. You may obtain
 *    a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *    License for the specific language governing permissions and limitations
 *    under the License.
 **/

package org.onlab.onos.of.controller.impl;

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
import org.onlab.onos.of.controller.Dpid;
import org.onlab.onos.of.controller.driver.OpenFlowAgent;
import org.onlab.onos.of.controller.driver.OpenFlowSwitchDriver;
import org.onlab.onos.of.controller.impl.annotations.LogMessageDoc;
import org.onlab.onos.of.controller.impl.annotations.LogMessageDocs;
import org.onlab.onos.of.drivers.impl.DriverManager;
import org.projectfloodlight.openflow.protocol.OFDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The main controller class.  Handles all setup and network listeners
 * - Distributed ownership control of switch through IControllerRegistryService
 */
public class Controller {

    protected static final Logger log = LoggerFactory.getLogger(Controller.class);
    static final String ERROR_DATABASE =
            "The controller could not communicate with the system database.";
    protected static final OFFactory FACTORY13 = OFFactories.getFactory(OFVersion.OF_13);
    protected static final OFFactory FACTORY10 = OFFactories.getFactory(OFVersion.OF_10);



    // The controllerNodeIPsCache maps Controller IDs to their IP address.
    // It's only used by handleControllerNodeIPsChanged
    protected HashMap<String, String> controllerNodeIPsCache;

    private ChannelGroup cg;

    // Configuration options
    protected int openFlowPort = 6633;
    protected int workerThreads = 0;

    // Start time of the controller
    protected long systemStartTime;

    // Flag to always flush flow table on switch reconnect (HA or otherwise)
    protected boolean alwaysClearFlowsOnSwAdd = false;
    private OpenFlowAgent agent;

    private NioServerSocketChannelFactory execFactory;

    // Perf. related configuration
    protected static final int SEND_BUFFER_SIZE = 4 * 1024 * 1024;
    protected static final int BATCH_MAX_SIZE = 100;
    protected static final boolean ALWAYS_DECODE_ETH = true;

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
    @LogMessageDocs({
        @LogMessageDoc(message = "Listening for switch connections on {address}",
                explanation = "The controller is ready and listening for new" +
                " switch connections"),
                @LogMessageDoc(message = "Storage exception in controller " +
                        "updates loop; terminating process",
                        explanation = ERROR_DATABASE,
                        recommendation = LogMessageDoc.CHECK_CONTROLLER),
                        @LogMessageDoc(level = "ERROR",
                        message = "Exception in controller updates loop",
                        explanation = "Failed to dispatch controller event",
                        recommendation = LogMessageDoc.GENERIC_ACTION)
    })
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
            execFactory =  new NioServerSocketChannelFactory(
                    Executors.newCachedThreadPool(),
                    Executors.newCachedThreadPool());
            return new ServerBootstrap(execFactory);
        } else {
            execFactory = new NioServerSocketChannelFactory(
                    Executors.newCachedThreadPool(),
                    Executors.newCachedThreadPool(), workerThreads);
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
        if (threads != null) {
            this.workerThreads = Integer.parseInt(threads);
        }
        log.debug("Number of worker threads set to {}", this.workerThreads);
    }


    /**
     * Initialize internal data structures.
     */
    public void init(Map<String, String> configParams) {
        // These data structures are initialized here because other
        // module's startUp() might be called before ours
        this.controllerNodeIPsCache = new HashMap<String, String>();

        setConfigParams(configParams);
        this.systemStartTime = System.currentTimeMillis();


    }

    // **************
    // Utility methods
    // **************

    public Map<String, Long> getMemory() {
        Map<String, Long> m = new HashMap<String, Long>();
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
     * @param desc
     * @return switch instance
     */
    protected OpenFlowSwitchDriver getOFSwitchInstance(long dpid,
            OFDescStatsReply desc, OFVersion ofv) {
        OpenFlowSwitchDriver sw = DriverManager.getSwitch(new Dpid(dpid),
                desc, ofv);
        sw.setAgent(agent);
        sw.setRoleHandler(new RoleManager(sw));
        return sw;
    }

    public void start(OpenFlowAgent ag) {
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
