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

package org.onosproject.openflow.controller.impl;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
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

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.onlab.util.Tools.get;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.openflow.controller.Dpid.uri;


/**
 * The main controller class.  Handles all setup and network listeners
 */
public class Controller {

    protected static final Logger log = LoggerFactory.getLogger(Controller.class);

    protected static final OFFactory FACTORY13 = OFFactories.getFactory(OFVersion.OF_13);
    protected static final OFFactory FACTORY10 = OFFactories.getFactory(OFVersion.OF_10);
    private static final boolean TLS_DISABLED = false;
    private static final short MIN_KS_LENGTH = 6;

    protected HashMap<String, String> controllerNodeIPsCache;

    private ChannelGroup cg;

    // Configuration options
    protected List<Integer> openFlowPorts = ImmutableList.of(6633, 6653);
    protected int workerThreads = 0;

    // Start time of the controller
    protected long systemStartTime;

    private OpenFlowAgent agent;

    private NioServerSocketChannelFactory execFactory;

    protected String ksLocation;
    protected String tsLocation;
    protected char[] ksPwd;
    protected char[] tsPwd;
    protected SSLContext sslContext;

    // Perf. related configuration
    protected static final int SEND_BUFFER_SIZE = 4 * 1024 * 1024;
    private DriverService driverService;
    private boolean enableOfTls = TLS_DISABLED;

    // ***************
    // Getters/Setters
    // ***************

    public OFFactory getOFMessageFactory10() {
        return FACTORY10;
    }


    public OFFactory getOFMessageFactory13() {
        return FACTORY13;
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
                    new OpenflowPipelineFactory(this, null, sslContext);
            bootstrap.setPipelineFactory(pfact);
            cg = new DefaultChannelGroup();
            openFlowPorts.forEach(port -> {
                InetSocketAddress sa = new InetSocketAddress(port);
                cg.add(bootstrap.bind(sa));
                log.info("Listening for switch connections on {}", sa);
            });

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private ServerBootstrap createServerBootStrap() {

        if (workerThreads == 0) {
            execFactory = new NioServerSocketChannelFactory(
                    Executors.newCachedThreadPool(groupedThreads("onos/of", "boss-%d", log)),
                    Executors.newCachedThreadPool(groupedThreads("onos/of", "worker-%d", log)));
            return new ServerBootstrap(execFactory);
        } else {
            execFactory = new NioServerSocketChannelFactory(
                    Executors.newCachedThreadPool(groupedThreads("onos/of", "boss-%d", log)),
                    Executors.newCachedThreadPool(groupedThreads("onos/of", "worker-%d", log)), workerThreads);
            return new ServerBootstrap(execFactory);
        }
    }

    public void setConfigParams(Dictionary<?, ?> properties) {
        String ports = get(properties, "openflowPorts");
        if (!Strings.isNullOrEmpty(ports)) {
            this.openFlowPorts = Stream.of(ports.split(","))
                                       .map(s -> Integer.parseInt(s))
                                       .collect(Collectors.toList());
        }
        log.debug("OpenFlow ports set to {}", this.openFlowPorts);

        String threads = get(properties, "workerThreads");
        if (!Strings.isNullOrEmpty(threads)) {
            this.workerThreads = Integer.parseInt(threads);
        }
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

        try {
            getTlsParameters();
            if (enableOfTls) {
                initSsl();
            }
        } catch (Exception ex) {
            log.error("SSL init failed: {}", ex.getMessage());
        }
    }

    private void getTlsParameters() {
        String tempString = System.getProperty("enableOFTLS");
        enableOfTls = Strings.isNullOrEmpty(tempString) ? TLS_DISABLED : Boolean.parseBoolean(tempString);
        log.info("OpenFlow Security is {}", enableOfTls ? "enabled" : "disabled");
        if (enableOfTls) {
            ksLocation = System.getProperty("javax.net.ssl.keyStore");
            if (Strings.isNullOrEmpty(ksLocation)) {
                enableOfTls = TLS_DISABLED;
                return;
            }
            tsLocation = System.getProperty("javax.net.ssl.trustStore");
            if (Strings.isNullOrEmpty(tsLocation)) {
                enableOfTls = TLS_DISABLED;
                return;
            }
            ksPwd = System.getProperty("javax.net.ssl.keyStorePassword").toCharArray();
            if (MIN_KS_LENGTH > ksPwd.length) {
                enableOfTls = TLS_DISABLED;
                return;
            }
            tsPwd = System.getProperty("javax.net.ssl.trustStorePassword").toCharArray();
            if (MIN_KS_LENGTH > tsPwd.length) {
                enableOfTls = TLS_DISABLED;
                return;
            }
        }
    }

    private void initSsl() throws Exception {
        TrustManagerFactory tmFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        KeyStore ts = KeyStore.getInstance("JKS");
        ts.load(new FileInputStream(tsLocation), tsPwd);
        tmFactory.init(ts);

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream(ksLocation), ksPwd);
        kmf.init(ks, ksPwd);

        sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), tmFactory.getTrustManagers(), null);
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


    public Long getSystemUptime() {
        RuntimeMXBean rb = ManagementFactory.getRuntimeMXBean();
        return rb.getUptime();
    }

    public long getSystemStartTime() {
        return (this.systemStartTime);
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

        if (driver == null) {
            log.error("No OpenFlow driver for {} : {}", dpid, desc);
            return null;
        }

        log.info("Driver {} assigned to device {}", driver.name(), dpidObj);

        if (!driver.hasBehaviour(OpenFlowSwitchDriver.class)) {
            log.error("Driver {} does not support OpenFlowSwitchDriver behaviour", driver.name());
            return null;
        }

        DefaultDriverHandler handler =
                new DefaultDriverHandler(new DefaultDriverData(driver, deviceId(uri(dpidObj))));
        OpenFlowSwitchDriver ofSwitchDriver =
                driver.createBehaviour(handler, OpenFlowSwitchDriver.class);
        ofSwitchDriver.init(dpidObj, desc, ofv);
        ofSwitchDriver.setAgent(agent);
        ofSwitchDriver.setRoleHandler(new RoleManager(ofSwitchDriver));
        log.info("OpenFlow handshaker found for device {}: {}", dpid, ofSwitchDriver);
        return ofSwitchDriver;
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
