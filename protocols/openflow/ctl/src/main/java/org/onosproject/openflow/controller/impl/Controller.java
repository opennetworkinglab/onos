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

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.GlobalEventExecutor;

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
import java.security.KeyStore;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

   /**
    * @deprecated in 1.10.0
    */
    @Deprecated
    protected static final OFFactory FACTORY13 = OFFactories.getFactory(OFVersion.OF_13);
    /**
     * @deprecated in 1.10.0
     */
    @Deprecated
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

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

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

    /**
     * @return OF1.0 factory
     * @deprecated in 1.10.0
     */
    @Deprecated
    public OFFactory getOFMessageFactory10() {
        return FACTORY10;
    }


    /**
     * @return OF1.3 factory
     * @deprecated in 1.10.0
     */
    @Deprecated
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
            bootstrap.option(ChannelOption.SO_REUSEADDR, true);
            bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
            bootstrap.childOption(ChannelOption.TCP_NODELAY, true);
            bootstrap.childOption(ChannelOption.SO_SNDBUF, Controller.SEND_BUFFER_SIZE);
//            bootstrap.childOption(ChannelOption.WRITE_BUFFER_WATER_MARK,
//                                  new WriteBufferWaterMark(8 * 1024, 32 * 1024));

            bootstrap.childHandler(new OFChannelInitializer(this, null, sslContext));

            openFlowPorts.forEach(port -> {
                // TODO revisit if this is best way to listen to multiple ports
                cg.add(bootstrap.bind(port).syncUninterruptibly().channel());
                log.info("Listening for switch connections on {}", port);
            });

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private ServerBootstrap createServerBootStrap() {

        int bossThreads = Math.max(1, openFlowPorts.size());
        try {
            bossGroup = new EpollEventLoopGroup(bossThreads, groupedThreads("onos/of", "boss-%d", log));
            workerGroup = new EpollEventLoopGroup(workerThreads, groupedThreads("onos/of", "worker-%d", log));
            ServerBootstrap bs = new ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    .channel(EpollServerSocketChannel.class);
            log.info("Using Epoll transport");
            return bs;
        } catch (Throwable e) {
            log.debug("Failed to initialize native (epoll) transport: {}", e.getMessage());
        }

// Requires 4.1.11 or later
//        try {
//            bossGroup = new KQueueEventLoopGroup(bossThreads, groupedThreads("onos/of", "boss-%d", log));
//            workerGroup = new KQueueEventLoopGroup(workerThreads, groupedThreads("onos/of", "worker-%d", log));
//            ServerBootstrap bs = new ServerBootstrap()
//                    .group(bossGroup, workerGroup)
//                    .channel(KQueueServerSocketChannel.class);
//            log.info("Using Kqueue transport");
//            return bs;
//        } catch (Throwable e) {
//            log.debug("Failed to initialize native (kqueue) transport. ", e.getMessage());
//        }

        bossGroup = new NioEventLoopGroup(bossThreads, groupedThreads("onos/of", "boss-%d", log));
        workerGroup = new NioEventLoopGroup(workerThreads, groupedThreads("onos/of", "worker-%d", log));
        log.info("Using Nio transport");
        return new ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class);
    }

    public void setConfigParams(Dictionary<?, ?> properties) {
        // TODO should be possible to reconfigure ports without restart,
        // by updating ChannelGroup
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

        cg = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

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
            log.error("No OpenFlow driver for {} : {}", dpidObj, desc);
            return null;
        }

        log.info("Driver '{}' assigned to device {}", driver.name(), dpidObj);

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

        // Shut down all event loops to terminate all threads.
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();

        // Wait until all threads are terminated.
        try {
            bossGroup.terminationFuture().sync();
            workerGroup.terminationFuture().sync();
        } catch (InterruptedException e) {
            log.warn("Interrupted while stopping", e);
            Thread.currentThread().interrupt();
        }
    }

}
