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
package org.onosproject.openflow.controller.impl;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
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
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.driver.DefaultDriverData;
import org.onosproject.net.driver.DefaultDriverHandler;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.DriverService;
import org.onosproject.openflow.config.OpenFlowDeviceConfig;
import org.onosproject.openflow.controller.Dpid;
import org.onosproject.openflow.controller.driver.OpenFlowAgent;
import org.onosproject.openflow.controller.driver.OpenFlowSwitchDriver;
import org.projectfloodlight.openflow.protocol.OFDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.DigestInputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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

    private static final Logger log = LoggerFactory.getLogger(Controller.class);

    private static final short MIN_KS_LENGTH = 6;

    //Default queues settings
    private static final short DEFAULT_QUEUE_SIZE = 5000;
    private static final short FIRST_QUEUE_SIZE = 1000;
    private static final short DEFAULT_BULK_SIZE = 100;
    private static final short DEFAULT_QUEUE_ID = 7;

    protected HashMap<String, String> controllerNodeIPsCache;

    private ChannelGroup cg;

    // Configuration options
    protected List<Integer> openFlowPorts = ImmutableList.of(6633, 6653);
    protected int workerThreads = 0;
    protected int[] cfgQueueSizes = {FIRST_QUEUE_SIZE, 0, 0, 0, 0, 0, 0, DEFAULT_QUEUE_SIZE};
    protected int[] cfgBulkSizes = new int[8];

    // Start time of the controller
    protected long systemStartTime;

    private OpenFlowAgent agent;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    enum TlsMode {
        DISABLED, // TLS is not used for OpenFlow connections
        ENABLED,  // Clients are required use TLS and present a client certificate
        STRICT,   // Clients must use TLS, and certificate must match the one specified in netcfg
    }
    private static final EnumSet<TlsMode> TLS_ENABLED = EnumSet.of(TlsMode.ENABLED, TlsMode.STRICT);

    protected TlsParams tlsParams;
    protected SSLContext sslContext;
    protected KeyStore keyStore;

    // Perf. related configuration
    protected static final int SEND_BUFFER_SIZE = 4 * 1024 * 1024;

    private DriverService driverService;
    private NetworkConfigRegistry netCfgService;

    public Controller() {
        Arrays.fill(cfgBulkSizes, DEFAULT_BULK_SIZE);
    }

    public int getQueueSize(int queueId) {
        return cfgQueueSizes[queueId];
    }

    public int getBulkSize(int queueId) {
        return cfgBulkSizes[queueId];
    }

    // **************
    // Initialization
    // **************

    private void addListeningPorts(Collection<Integer> ports) {
        if (cg == null) {
            return;
        }
        final ServerBootstrap bootstrap = createServerBootStrap();
        bootstrap.option(ChannelOption.SO_REUSEADDR, true);
        bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.childOption(ChannelOption.TCP_NODELAY, true);
        bootstrap.childOption(ChannelOption.SO_SNDBUF, Controller.SEND_BUFFER_SIZE);
//            bootstrap.childOption(ChannelOption.WRITE_BUFFER_WATER_MARK,
//                                  new WriteBufferWaterMark(8 * 1024, 32 * 1024));

        bootstrap.childHandler(new OFChannelInitializer(this, null, sslContext));

        Set<Integer> existingPorts = cg.stream()
                .map(Channel::localAddress)
                .filter(InetSocketAddress.class::isInstance)
                .map(InetSocketAddress.class::cast)
                .map(InetSocketAddress::getPort)
                .collect(Collectors.toSet());
        ports.removeAll(existingPorts);

        ports.forEach(port -> {
            // TODO revisit if this is best way to listen to multiple ports
            cg.add(bootstrap.bind(port).syncUninterruptibly().channel());
            log.info("Listening for OF switch connections on {}", port);
        });
    }

    private void removeListeningPorts(Collection<Integer> ports) {
        if (cg == null) {
            return;
        }
        Iterator<Channel> itr = cg.iterator();
        while (itr.hasNext()) {
            Channel c = itr.next();
            SocketAddress addr = c.localAddress();
            if (addr instanceof InetSocketAddress) {
                InetSocketAddress inetAddr = (InetSocketAddress) addr;
                Integer port = inetAddr.getPort();
                if (ports.contains(port)) {
                    log.info("No longer listening for OF switch connections on {}", port);
                    c.close();
                    itr.remove();
                }
            }

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

    public void setQueueParams(Dictionary<?, ?> properties, String sizeParamName, String bulkParamName, int queueId) {
        String queueSize = get(properties, sizeParamName);
        if (!Strings.isNullOrEmpty(queueSize)) {
            int size = Integer.parseInt(queueSize);
            if (size > 0) {
                this.cfgQueueSizes[queueId] = size;
            } else {
                throw new IllegalArgumentException(
                    String.format("%s value must be either a positive integer value", sizeParamName));
            }
        }
        String bulkSize = get(properties, bulkParamName);
        if (!Strings.isNullOrEmpty(bulkSize)) {
            int bulk = Integer.parseInt(bulkSize);
            if (bulk > 0) {
                this.cfgBulkSizes[queueId] = bulk;
            } else {
                throw new IllegalArgumentException(
                    String.format("%s value must be either a positive integer value", bulkParamName));
            }
        }
    }

    public void setConfigParams(Dictionary<?, ?> properties) {
        boolean restartRequired = setOpenFlowPorts(properties);
        restartRequired |= setWorkerThreads(properties);
        restartRequired |= setTlsParameters(properties);
        if (restartRequired) {
            restart();
        }
    }

    /**
     * Gets the list of listening ports from property dict.
     *
     * @param properties dictionary
     * @return true if restart is required
     */
    private boolean setWorkerThreads(Dictionary<?, ?> properties) {
        List<Integer> oldPorts = this.openFlowPorts;
        String ports = get(properties, "openflowPorts");
        List<Integer> newPorts = Collections.emptyList();
        if (!Strings.isNullOrEmpty(ports)) {
            newPorts = Stream.of(ports.split(","))
                    .map(s -> Integer.parseInt(s))
                    .collect(Collectors.toList());
        }

        Set<Integer> portsToAdd = Sets.newHashSet(newPorts);
        portsToAdd.removeAll(oldPorts);
        addListeningPorts(portsToAdd);

        Set<Integer> portsToRemove = Sets.newHashSet(oldPorts);
        portsToRemove.removeAll(newPorts);
        removeListeningPorts(portsToRemove);

        this.openFlowPorts = newPorts;
        log.debug("OpenFlow ports set to {}", this.openFlowPorts);
        return false; // restart is never required
    }

    /**
     * Gets the number of worker threads from property dict.
     *
     * @param properties dictionary
     * @return true if restart is required
     */
    private boolean setOpenFlowPorts(Dictionary<?, ?> properties) {
        Integer oldValue = this.workerThreads;

        String threads = get(properties, "workerThreads");
        if (!Strings.isNullOrEmpty(threads)) {
            this.workerThreads = Integer.parseInt(threads);
        }
        log.debug("Number of worker threads set to {}", this.workerThreads);

        setQueueParams(properties, "defaultQueueSize", "defaultBulkSize", DEFAULT_QUEUE_ID);
        setQueueParams(properties, "queueSizeN0", "bulkSizeN0", 0);
        setQueueParams(properties, "queueSizeN1", "bulkSizeN1", 1);
        setQueueParams(properties, "queueSizeN2", "bulkSizeN2", 2);
        setQueueParams(properties, "queueSizeN3", "bulkSizeN3", 3);
        setQueueParams(properties, "queueSizeN4", "bulkSizeN4", 4);
        setQueueParams(properties, "queueSizeN5", "bulkSizeN5", 5);
        setQueueParams(properties, "queueSizeN6", "bulkSizeN6", 6);

        return oldValue != this.workerThreads; // restart if number of threads has changed
    }

    static class TlsParams {
        final TlsMode mode;
        final String ksLocation;
        final String tsLocation;
        final String ksPwd;
        final String tsPwd;
        final byte[] ksSignature;
        final byte[] tsSignature;

        TlsParams(TlsMode mode, String ksLocation, String tsLocation,
                  String ksPwd, String tsPwd) {
            this.mode = mode;
            this.ksLocation = ksLocation;
            this.tsLocation = tsLocation;
            this.ksPwd = ksPwd;
            this.tsPwd = tsPwd;
            this.ksSignature = getSha1Checksum(ksLocation);
            this.tsSignature = getSha1Checksum(tsLocation);
        }

        public char[] ksPwd() {
            return ksPwd.toCharArray();
        }

        public char[] tsPwd() {
            return tsPwd.toCharArray();
        }

        public boolean isTlsEnabled() {
            return TLS_ENABLED.contains(mode);
        }

        public byte[] getSha1Checksum(String filepath) {
            if (filepath == null) {
                return new byte[0];
            }
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA1");
                File f = new File(filepath);
                FileInputStream is = new FileInputStream(f);
                DigestInputStream dis = new DigestInputStream(is, digest);
                byte[] buffer = new byte[1024];
                while (dis.read(buffer) > 0) {
                    // nothing to do :)
                }
                is.close();
                return dis.getMessageDigest().digest();
            } catch (NoSuchAlgorithmException ignored) {
            } catch (IOException e) {
                log.info("Error reading file file: {}", filepath);
            }
            return new byte[0];
        }

        @Override
        public int hashCode() {
            if (mode == TlsMode.DISABLED) {
                return Objects.hash(mode);
            }
            return Objects.hash(mode, ksLocation, tsLocation,
                    ksPwd, tsPwd,
                    Arrays.hashCode(ksSignature),
                    Arrays.hashCode(tsSignature));
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof TlsParams) {
                final TlsParams that = (TlsParams) obj;
                if (this.getClass() != that.getClass()) {
                    return false;
                } else if (this.mode == that.mode && this.mode == TlsMode.DISABLED) {
                    // All disabled objects should be equal regardless of other params
                    return true;
                }
                return this.mode == that.mode &&
                        Objects.equals(this.ksLocation, that.ksLocation) &&
                        Objects.equals(this.tsLocation, that.tsLocation) &&
                        Objects.equals(this.ksPwd, that.ksPwd) &&
                        Objects.equals(this.tsPwd, that.tsPwd) &&
                        Arrays.equals(this.ksSignature, that.ksSignature) &&
                        Arrays.equals(this.tsSignature, that.tsSignature);
            }
            return false;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("tlsMode", mode.toString().toLowerCase())
                    .add("ksLocation", ksLocation)
                    .add("tsLocation", tsLocation)
                    .toString();
        }
    }

    /**
     * Gets the TLS parameters from the properties dict, but fallback to the old approach of
     * system properties if the parameters are missing from the dict.
     *
     * @param properties dictionary
     * @return true if restart is required
     */
    private boolean setTlsParameters(Dictionary<?, ?> properties) {
        TlsParams oldParams = this.tlsParams;

        TlsMode mode = null;
        String tlsString = get(properties, "tlsMode");
        if (!Strings.isNullOrEmpty(tlsString)) {
            try {
                mode = TlsMode.valueOf(tlsString.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.info("Invalid TLS mode {}. TLS is disabled.", tlsString);
                mode = TlsMode.DISABLED;
            }
        } else {
            // Fallback to system properties
            // TODO this method of configuring TLS is deprecated and should be removed eventually
            tlsString = System.getProperty("enableOFTLS");
            mode = !Strings.isNullOrEmpty(tlsString) && Boolean.parseBoolean(tlsString) ?
                    TlsMode.ENABLED : TlsMode.DISABLED;
        }

        String ksLocation = null, tsLocation = null, ksPwd = null, tsPwd = null;
        if (TLS_ENABLED.contains(mode)) {
            ksLocation = get(properties, "keyStore");
            if (Strings.isNullOrEmpty(ksLocation)) {
                // Fallback to system properties
                // TODO remove this eventually
                ksLocation = System.getProperty("javax.net.ssl.keyStore");
            }
            if (Strings.isNullOrEmpty(ksLocation)) {
                mode = TlsMode.DISABLED;
            }

            tsLocation = get(properties, "trustStore");
            if (Strings.isNullOrEmpty(tsLocation)) {
                // Fallback to system properties
                // TODO remove this eventually
                tsLocation = System.getProperty("javax.net.ssl.trustStore");
            }
            if (Strings.isNullOrEmpty(tsLocation)) {
                mode = TlsMode.DISABLED;
            }

            ksPwd = get(properties, "keyStorePassword");
            if (Strings.isNullOrEmpty(ksPwd)) {
                // Fallback to system properties
                // TODO remove this eventually
                ksPwd = System.getProperty("javax.net.ssl.keyStorePassword");
            }
            if (Strings.isNullOrEmpty(ksPwd) || MIN_KS_LENGTH > ksPwd.length()) {
                mode = TlsMode.DISABLED;
            }

            tsPwd = get(properties, "trustStorePassword");
            if (Strings.isNullOrEmpty(tsPwd)) {
                // Fallback to system properties
                // TODO remove this eventually
                tsPwd = System.getProperty("javax.net.ssl.trustStorePassword");
            }
            if (Strings.isNullOrEmpty(tsPwd) || MIN_KS_LENGTH > tsPwd.length()) {
                mode = TlsMode.DISABLED;
            }
        }
        this.tlsParams = new TlsParams(mode, ksLocation, tsLocation, ksPwd, tsPwd);
        log.info("OpenFlow TLS Params: {}", tlsParams);
        return !Objects.equals(this.tlsParams, oldParams); // restart if TLS params change
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

        if (tlsParams.isTlsEnabled()) {
            initSsl();
        }
    }

    private void initSsl() {
        try {
            TrustManagerFactory tmFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            KeyStore ts = KeyStore.getInstance("JKS");
            ts.load(new FileInputStream(tlsParams.tsLocation), tlsParams.tsPwd());
            tmFactory.init(ts);

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyStore = KeyStore.getInstance("JKS");
            keyStore.load(new FileInputStream(tlsParams.ksLocation), tlsParams.ksPwd());
            kmf.init(keyStore, tlsParams.ksPwd());

            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), tmFactory.getTrustManagers(), null);
        } catch (NoSuchAlgorithmException | KeyStoreException | CertificateException |
                IOException | KeyManagementException | UnrecoverableKeyException ex) {
            log.error("SSL init failed: {}", ex.getMessage());
        }
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

    public boolean isValidCertificate(Long dpid, Certificate peerCert) {
        if (!tlsParams.isTlsEnabled()) {
            return true;
        }

        if (netCfgService == null) {
            // netcfg service not available; accept any cert if not in strict mode
            return tlsParams.mode == TlsMode.ENABLED;
        }

        DeviceId deviceId = DeviceId.deviceId(Dpid.uri(new Dpid(dpid)));
        OpenFlowDeviceConfig config =
                netCfgService.getConfig(deviceId, OpenFlowDeviceConfig.class);
        if (config == null) {
            // Config not set for device, accept any cert if not in strict mode
            return tlsParams.mode == TlsMode.ENABLED;
        }

        Optional<String> alias = config.keyAlias();
        if (!alias.isPresent()) {
            // Config for device does not specify a certificate chain, accept any cert if not in strict mode
            return tlsParams.mode == TlsMode.ENABLED;
        }

        try {
            Certificate configuredCert = keyStore.getCertificate(alias.get());
            //TODO there's probably a better way to compare these
            return Objects.deepEquals(peerCert, configuredCert);
        } catch (KeyStoreException e) {
            log.info("failed to load key", e);
        }
        return false;
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

    public void start(OpenFlowAgent ag, DriverService driverService,
                      NetworkConfigRegistry netCfgService) {
        log.info("Starting OpenFlow IO");
        this.agent = ag;
        this.driverService = driverService;
        this.netCfgService = netCfgService;
        this.init();
        this.addListeningPorts(this.openFlowPorts);
    }


    public void stop() {
        log.info("Stopping OpenFlow IO");
        if (cg != null) {
            cg.close();
            cg = null;
        }

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

    private void restart() {
        // only restart if we are already running
        if (cg != null) {
            stop();
            start(this.agent, this.driverService, this.netCfgService);
        }
    }

}
