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

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.packet.Ethernet.TYPE_BSN;
import static org.onlab.packet.Ethernet.TYPE_LLDP;
import static org.onlab.util.GroupedThreadFactory.groupedThreadFactory;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.openflow.controller.Dpid.uri;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.onlab.util.GroupedThreadFactory;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IpAddress;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.packet.DefaultInboundPacket;
import org.onosproject.openflow.controller.DefaultOpenFlowPacketContext;
import org.onosproject.openflow.controller.Dpid;
import org.onosproject.openflow.controller.OpenFlowClassifier;
import org.onosproject.openflow.controller.OpenFlowPacketContext;
import org.onosproject.openflow.controller.OpenFlowService;
import org.onosproject.openflow.controller.OpenFlowSession;
import org.onosproject.openflow.controller.driver.OpenFlowSwitchDriver;
import org.onosproject.openflow.controller.driver.SwitchStateException;
import org.projectfloodlight.openflow.exceptions.OFParseError;
import org.projectfloodlight.openflow.protocol.OFAsyncGetReply;
import org.projectfloodlight.openflow.protocol.OFBadRequestCode;
import org.projectfloodlight.openflow.protocol.OFBarrierReply;
import org.projectfloodlight.openflow.protocol.OFBarrierRequest;
import org.projectfloodlight.openflow.protocol.OFDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFDescStatsRequest;
import org.projectfloodlight.openflow.protocol.OFEchoReply;
import org.projectfloodlight.openflow.protocol.OFEchoRequest;
import org.projectfloodlight.openflow.protocol.OFErrorMsg;
import org.projectfloodlight.openflow.protocol.OFErrorType;
import org.projectfloodlight.openflow.protocol.OFExperimenter;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFeaturesReply;
import org.projectfloodlight.openflow.protocol.OFFlowModFailedCode;
import org.projectfloodlight.openflow.protocol.OFFlowRemoved;
import org.projectfloodlight.openflow.protocol.OFGetConfigReply;
import org.projectfloodlight.openflow.protocol.OFGetConfigRequest;
import org.projectfloodlight.openflow.protocol.OFHello;
import org.projectfloodlight.openflow.protocol.OFHelloElem;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFMeterFeaturesStatsReply;
import org.projectfloodlight.openflow.protocol.OFMeterFeaturesStatsRequest;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFPortDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFPortDescStatsRequest;
import org.projectfloodlight.openflow.protocol.OFPortStatus;
import org.projectfloodlight.openflow.protocol.OFQueueGetConfigReply;
import org.projectfloodlight.openflow.protocol.OFRoleReply;
import org.projectfloodlight.openflow.protocol.OFSetConfig;
import org.projectfloodlight.openflow.protocol.OFStatsReply;
import org.projectfloodlight.openflow.protocol.OFStatsReplyFlags;
import org.projectfloodlight.openflow.protocol.OFStatsType;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.errormsg.OFBadRequestErrorMsg;
import org.projectfloodlight.openflow.protocol.errormsg.OFFlowModFailedErrorMsg;
import org.projectfloodlight.openflow.types.U32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.util.ReferenceCountUtil;

import javax.net.ssl.SSLPeerUnverifiedException;

/**
 * Channel handler deals with the switch connection and dispatches
 * switch messages to the appropriate locations.
 */
class OFChannelHandler extends ChannelInboundHandlerAdapter
    implements OpenFlowSession {

    private static final Logger log = LoggerFactory.getLogger(OFChannelHandler.class);

    private static final String RESET_BY_PEER = "Connection reset by peer";
    private static final String BROKEN_PIPE = "Broken pipe";
    static final int NUM_OF_QUEUES = 8;

    private final Controller controller;
    private OpenFlowSwitchDriver sw;
    private long thisdpid; // channelHandler cached value of connected switch id
    private DeviceId deviceId;
    private Channel channel;
    private String channelId;


    // State needs to be volatile because the HandshakeTimeoutHandler
    // needs to check if the handshake is complete
    private volatile ChannelState state;

    /**
     * Timeout in ms to wait for meter feature reply.
     */
    private static final long METER_TIMEOUT = 60_000;

    private volatile long lastStateChange = System.currentTimeMillis();

    // When a switch with a duplicate dpid is found (i.e we already have a
    // connected switch with the same dpid), the new switch is immediately
    // disconnected. At that point netty callsback channelDisconnected() which
    // proceeds to cleaup switch state - we need to ensure that it does not cleanup
    // switch state for the older (still connected) switch
    private volatile Boolean duplicateDpidFound;

    // Temporary storage for switch-features and port-description
    private OFFeaturesReply featuresReply;
    private List<OFPortDescStatsReply> portDescReplies;
    private OFMeterFeaturesStatsReply meterFeaturesReply;
    //private OFPortDescStatsReply portDescReply;
    // a concurrent ArrayList to temporarily store port status messages
    // before we are ready to deal with them
    private final CopyOnWriteArrayList<OFPortStatus> pendingPortStatusMsg;

    //Indicates the openflow version used by this switch
    protected OFVersion ofVersion;
    protected OFFactory factory;

    /** transaction Ids to use during handshake. Since only one thread
     * calls into an OFChannelHandler instance, we don't need atomic.
     * We will count down
     */
    private int handshakeTransactionIds = -1;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private OpenFlowService openFlowManager;

    // Each IDLE_INTERVAL (see OFChannelInitializer) we will perform a sanity check
    // Idle timeout actions will be performed each MAX_IDLE_RETRY * IDLE_INTERVAL
    private static final int MAX_IDLE_RETRY = 4;
    private int maxIdleRetry = MAX_IDLE_RETRY;

    // Dispatcher buffer/read size
    private static final int BACKLOG_READ_BUFFER_DEFAULT = 1000;

    /**
     * Map with all LinkedBlockingMessagesQueue queues which contains OFMessages.
     */
    private Map<Integer, LinkedBlockingMessagesQueue<OFMessage>> dispatchQueuesMapProducer = new ConcurrentHashMap<>();

    /**
     * OFMessage classifiers map.
     */
    private List<Set<OpenFlowClassifier>> messageClassifiersMapProducer =
            new CopyOnWriteArrayList<Set<OpenFlowClassifier>>();

    /**
     * Lock held by take, poll, etc.
     */
    private final ReentrantLock takeLock = new ReentrantLock();

    /**
     * Wait queue for waiting takes.
     */
    private final Condition notEmpty = takeLock.newCondition();

    /**
     * Current number of elements in enabled sub-queues.
     */
    private final AtomicInteger totalCount = new AtomicInteger();

    /**
     * Single thread executor for OFMessage dispatching.
     *
     * Gets initialized on channelActive, shutdown on channelInactive.
     */
    private ExecutorService dispatcher;

    /**
     * Handle for dispatcher thread.
     * <p>
     * Should only be touched from the Channel I/O thread
     */
    private Future<?> dispatcherHandle = CompletableFuture.completedFuture(null);

    /**
     * Dispatch backlog.
     * <p>
     * Should only be touched from the Channel I/O thread
     */
    private final Deque<OFMessage> dispatchBacklog;

    /**
     * Executor for runtime status events to offload from the main thread the
     * processing of port status, mastership and connection OF messages.
     * Executor is instantiated as a single thread executor guaranteeing processing
     * of device status messages in order.
     */
    // TODO With a huge number of ports per device congestion can be created at the runtimeExecutor,
    //  leading to mastership roles check going into timeout, with issues in multi-instance mastership handling.
    //  An option is to Experiment with priority tasks - to give priority (under certain conditions) to the ROLE_REPLY
    protected ExecutorService runtimeStatusExecutor;

    /**
     * Create a new unconnected OFChannelHandler.
     * @param controller parent controller
     */
    OFChannelHandler(Controller controller) {

        this.controller = controller;
        this.state = ChannelState.INIT;
        this.pendingPortStatusMsg = new CopyOnWriteArrayList<>();
        this.portDescReplies = new ArrayList<>();
        duplicateDpidFound = Boolean.FALSE;
        String groupName = "onos/of-channel-handler";
        String pattern = "runtime-status-%d";
        ThreadFactory factory = new ThreadFactoryBuilder()
                .setThreadFactory(groupedThreadFactory(groupName))
                .setNameFormat(groupName.replace(GroupedThreadFactory.DELIMITER, "-") + "-" + pattern)
                .setUncaughtExceptionHandler((t, e) -> {
                    log.error("Exception on " + t.getName(), e);
                    throw new UncheckedIOException(new IOException(e));
                }).build();
        runtimeStatusExecutor = newSingleThreadExecutor(
                factory);
        //Initialize queues and classifiers
        dispatchBacklog = new LinkedBlockingDeque<>(BACKLOG_READ_BUFFER_DEFAULT);
        for (int i = 0; i < NUM_OF_QUEUES; i++) {
            if (controller.getQueueSize(i) > 0) {
                dispatchQueuesMapProducer.put(i,
                        new LinkedBlockingMessagesQueue<>(i, controller.getQueueSize(i), controller.getBulkSize(i)));
            }
            if (i != NUM_OF_QUEUES) {
                messageClassifiersMapProducer.add(i, new CopyOnWriteArraySet<>());
            }
        }
    }

    // XXX S consider if necessary
    public void disconnectSwitch() {
        sw.disconnectSwitch();
    }

    //*************************
    //  Channel State Machine
    //*************************

    /**
     * The state machine for handling the switch/channel state. All state
     * transitions should happen from within the state machine (and not from other
     * parts of the code)
     */
    enum ChannelState {
        /**
         * Initial state before channel is connected.
         */
        INIT(false) {
            @Override
            void processOFMessage(OFChannelHandler h, OFMessage m)
                    throws IOException, SwitchStateException {
                illegalMessageReceived(h, m);
            }

            @Override
            void processOFError(OFChannelHandler h, OFErrorMsg m)
                    throws IOException {
                // need to implement since its abstract but it will never
                // be called
            }

            @Override
            void processOFPortStatus(OFChannelHandler h, OFPortStatus m)
                    throws IOException {
                unhandledMessageReceived(h, m);
            }
        },

        /**
         * We send a OF 1.3 HELLO to the switch and wait for a Hello from the switch.
         * Once we receive the reply, we decide on OF 1.3 or 1.0 switch - no other
         * protocol version is accepted.
         * We send an OFFeaturesRequest depending on the protocol version selected
         * Next state is WAIT_FEATURES_REPLY
         */
        WAIT_HELLO(false) {
            @Override
            void processOFHello(OFChannelHandler h, OFHello m)
                    throws IOException {
                // TODO We could check for the optional bitmap, but for now
                // we are just checking the version number.
                if (m.getVersion().getWireVersion() >= OFVersion.OF_13.getWireVersion()) {
                    log.debug("Received {} Hello from {} - switching to OF "
                            + "version 1.3+", m.getVersion(),
                            h.channel.remoteAddress());
                    h.ofVersion = m.getVersion();
                    h.factory = OFFactories.getFactory(h.ofVersion);
                    h.sendHandshakeHelloMessage();
                } else if (m.getVersion().getWireVersion() >= OFVersion.OF_10.getWireVersion()) {
                    log.debug("Received {} Hello from {} - switching to OF "
                            + "version 1.0", m.getVersion(),
                            h.channel.remoteAddress());
                    h.ofVersion = m.getVersion();
                    h.factory = OFFactories.getFactory(h.ofVersion);
                    OFHello hi =
                            h.factory.buildHello()
                                    .setXid(h.handshakeTransactionIds--)
                                    .build();
                    h.channel.writeAndFlush(Collections.singletonList(hi));
                } else {
                    log.error("Received Hello of version {} from switch at {}. "
                            + "This controller works with OF1.0 and OF1.3 "
                            + "switches. Disconnecting switch ...",
                            m.getVersion(), h.channel.remoteAddress());
                    h.channel.disconnect();
                    return;
                }
                h.sendHandshakeFeaturesRequestMessage();
                h.setState(WAIT_FEATURES_REPLY);
            }

            @Override
            void processOFFeaturesReply(OFChannelHandler h, OFFeaturesReply  m)
                    throws IOException, SwitchStateException {
                illegalMessageReceived(h, m);
            }
            @Override
            void processOFStatisticsReply(OFChannelHandler h,
                    OFStatsReply  m)
                            throws IOException, SwitchStateException {
                illegalMessageReceived(h, m);
            }
            @Override
            void processOFError(OFChannelHandler h, OFErrorMsg m) {
                logErrorDisconnect(h, m);
            }

            @Override
            void processOFPortStatus(OFChannelHandler h, OFPortStatus m)
                    throws IOException {
                unhandledMessageReceived(h, m);
            }
        },

        /**
         * We are waiting for a features reply message. Once we receive it, the
         * behavior depends on whether this is a 1.0 or 1.3 switch. For 1.0,
         * we send a SetConfig request, barrier, and GetConfig request and the
         * next state is WAIT_CONFIG_REPLY. For 1.3, we send a Port description
         * request and the next state is WAIT_PORT_DESC_REPLY.
         */
        WAIT_FEATURES_REPLY(false) {
            @Override
            void processOFFeaturesReply(OFChannelHandler h, OFFeaturesReply m)
                    throws IOException {
                Long dpid = m.getDatapathId().getLong();
                if (!h.setDpid(dpid, h.channel)) {
                    log.error("Switch presented invalid certificate for dpid {}. Disconnecting",
                            dpid);
                    h.channel.disconnect();
                    return;
                }
                h.deviceId = DeviceId.deviceId(uri(h.thisdpid));
                log.debug("Received features reply for switch at {} with dpid {}",
                        h.getSwitchInfoString(), h.thisdpid);

                h.featuresReply = m; //temp store
                if (h.ofVersion == OFVersion.OF_10) {
                    h.sendHandshakeSetConfig();
                    h.setState(WAIT_CONFIG_REPLY);
                } else {
                    //version is 1.3, must get switchport information
                    h.sendHandshakeOFPortDescRequest();
                    h.setState(WAIT_PORT_DESC_REPLY);
                }
            }
            @Override
            void processOFStatisticsReply(OFChannelHandler h,
                    OFStatsReply  m)
                            throws IOException, SwitchStateException {
                illegalMessageReceived(h, m);
            }
            @Override
            void processOFError(OFChannelHandler h, OFErrorMsg m) {
                logErrorDisconnect(h, m);
            }

            @Override
            void processOFPortStatus(OFChannelHandler h, OFPortStatus m)
                    throws IOException {
                h.pendingPortStatusMsg.add(m);
            }
        },

        /**
         * We are waiting for a description of the 1.3 switch ports.
         * Once received, we send a SetConfig request
         * Next State is WAIT_CONFIG_REPLY
         */
        WAIT_PORT_DESC_REPLY(false) {

            @Override
            void processOFStatisticsReply(OFChannelHandler h, OFStatsReply m)
                    throws SwitchStateException {
                // Read port description
                if (m.getStatsType() != OFStatsType.PORT_DESC) {
                    log.warn("Expecting port description stats but received stats "
                            + "type {} from {}. Ignoring ...", m.getStatsType(),
                            h.channel.remoteAddress());
                    return;
                }
                if (m.getFlags().contains(OFStatsReplyFlags.REPLY_MORE)) {
                    log.debug("Stats reply indicates more stats from sw {} for "
                            + "port description",
                            h.getSwitchInfoString());
                    h.portDescReplies.add((OFPortDescStatsReply) m);
                    return;
                } else {
                    h.portDescReplies.add((OFPortDescStatsReply) m);
                }
                //h.portDescReply = (OFPortDescStatsReply) m; // temp store
                if (log.isDebugEnabled()) {
                    log.debug("Received port desc reply for switch at {}: {}",
                              h.getSwitchInfoString(),
                              ((OFPortDescStatsReply) m).getEntries());
                }
                try {
                    h.sendHandshakeSetConfig();
                } catch (IOException e) {
                    log.error("Unable to send setConfig after PortDescReply. "
                            + "Error: {}", e.getMessage());
                }
                h.setState(WAIT_CONFIG_REPLY);
            }

            @Override
            void processOFError(OFChannelHandler h, OFErrorMsg m)
                    throws IOException, SwitchStateException {
                logErrorDisconnect(h, m);

            }

            @Override
            void processOFPortStatus(OFChannelHandler h, OFPortStatus m)
                    throws IOException, SwitchStateException {
                h.pendingPortStatusMsg.add(m);

            }
        },

        /**
         * We are waiting for a config reply message. Once we receive it
         * we send a DescriptionStatsRequest to the switch.
         * Next state: WAIT_DESCRIPTION_STAT_REPLY
         */
        WAIT_CONFIG_REPLY(false) {
            @Override
            void processOFGetConfigReply(OFChannelHandler h, OFGetConfigReply m)
                    throws IOException {
                if (m.getMissSendLen() == 0xffff) {
                    log.trace("Config Reply from switch {} confirms "
                            + "miss length set to 0xffff",
                            h.getSwitchInfoString());
                } else {
                    // FIXME: we can't really deal with switches that don't send
                    // full packets. Shouldn't we drop the connection here?
                    log.warn("Config Reply from switch {} has "
                            + "miss length set to {}",
                            h.getSwitchInfoString(),
                            m.getMissSendLen());
                }

                nextState(h);
            }

            /**
             * Transition to next state based on OF version.
             *
             * @param h current channel handler
             * @throws IOException
             */
            private void nextState(OFChannelHandler h) throws IOException {
                if (h.ofVersion.getWireVersion() >= OFVersion.OF_13.getWireVersion()) {
                    // Meters were introduced in OpenFlow 1.3
                    h.sendMeterFeaturesRequest();
                    h.setState(WAIT_METER_FEATURES_REPLY);
                } else {
                    h.sendHandshakeDescriptionStatsRequest();
                    h.setState(WAIT_DESCRIPTION_STAT_REPLY);
                }
            }

            @Override
            void processOFBarrierReply(OFChannelHandler h, OFBarrierReply m) {
                // do nothing;
            }

            @Override
            void processOFFeaturesReply(OFChannelHandler h, OFFeaturesReply  m)
                    throws IOException, SwitchStateException {
                illegalMessageReceived(h, m);
            }

            @Override
            void processOFStatisticsReply(OFChannelHandler h,
                    OFStatsReply  m)
                            throws IOException, SwitchStateException {
                log.error("Received multipart(stats) message sub-type {}",
                        m.getStatsType());
                illegalMessageReceived(h, m);
            }

            @Override
            void processOFError(OFChannelHandler h, OFErrorMsg m) {
                if (m.getErrType() == OFErrorType.BAD_REQUEST) {
                    OFBadRequestErrorMsg badRequest = (OFBadRequestErrorMsg) m;
                    if (badRequest.getCode() == OFBadRequestCode.BAD_TYPE) {
                         log.debug("{} does not support GetConfig, moving on", h.getSwitchInfoString());
                         try {
                            nextState(h);
                            return;
                        } catch (IOException e) {
                            log.error("Exception thrown transitioning to next", e);
                            logErrorDisconnect(h, m);
                        }
                    }
                }
                logErrorDisconnect(h, m);
            }

            @Override
            void processOFPortStatus(OFChannelHandler h, OFPortStatus m)
                    throws IOException {
                h.pendingPortStatusMsg.add(m);
            }
        },

        /**
         * We are waiting for a OFDescriptionStat message from the switch.
         * Once we receive any stat message we try to parse it. If it's not
         * a description stats message we disconnect. If its the expected
         * description stats message, we:
         *    - use the switch driver to bind the switch and get an IOFSwitch instance
         *    - setup the IOFSwitch instance
         *    - add switch controller and send the initial role
         *      request to the switch.
         * Next state: WAIT_INITIAL_ROLE
         *      In the typical case, where switches support role request messages
         *      the next state is where we expect the role reply message.
         *      In the special case that where the switch does not support any kind
         *      of role request messages, we don't send a role message, but we do
         *      request mastership from the registry service. This controller
         *      should become master once we hear back from the registry service.
         * All following states will have a h.sw instance!
         */
        WAIT_DESCRIPTION_STAT_REPLY(false) {
            @Override
            void processOFStatisticsReply(OFChannelHandler h, OFStatsReply m)
                    throws SwitchStateException {
                // Read description, if it has been updated
                if (m.getStatsType() != OFStatsType.DESC) {
                    log.warn("Expecting Description stats but received stats "
                            + "type {} from {}. Ignoring ...", m.getStatsType(),
                            h.channel.remoteAddress());
                    return;
                }
                OFDescStatsReply drep = (OFDescStatsReply) m;
                log.info("Received switch description reply {} from switch at {}",
                         drep, h.channel.remoteAddress());
                // Here is where we differentiate between different kinds of switches
                h.sw = h.controller.getOFSwitchInstance(h.thisdpid, drep, h.ofVersion);

                if (h.sw == null) {
                    log.info("Switch not found for {}", h.thisdpid);
                    return;
                }

                // set switch information
                h.sw.setOFVersion(h.ofVersion);
                h.sw.setFeaturesReply(h.featuresReply);
                h.sw.setPortDescReplies(h.portDescReplies);
                h.sw.setMeterFeaturesReply(h.meterFeaturesReply);
                h.sw.setConnected(true);
                h.sw.setChannel(h);

                //Port Description List has served its purpose, clearing.
                h.portDescReplies.clear();

//                boolean success = h.sw.connectSwitch();
//
//                if (!success) {
//                    disconnectDuplicate(h);
//                    return;
//                }

                log.debug("Switch {} bound to class {}, description {}",
                        h.sw, h.sw.getClass(), drep);
                //Put switch in EQUAL mode until we hear back from the global registry
                //log.debug("Setting new switch {} to EQUAL and sending Role request",
                //        h.sw.getStringId());
                //h.sw.activateEqualSwitch();
                //h.setSwitchRole(RoleState.EQUAL);

                h.sw.startDriverHandshake();
                if (h.sw.isDriverHandshakeComplete()) {
                    // We are not able to complete the connection for a dpid collision.
                    // Same device reconnecting or different device configured with
                    // the same dpid.
                    if (!h.sw.connectSwitch()) {
                        // Disconnect from the device and return
                        disconnectDuplicate(h);
                        return;
                    } else {
                        h.initClassifiers();
                    }
                    handlePendingPortStatusMessages(h);
                    h.setState(ACTIVE);
                } else {
                    h.setState(WAIT_SWITCH_DRIVER_SUB_HANDSHAKE);
                }

            }

            @Override
            void processOFError(OFChannelHandler h, OFErrorMsg m) {
                logErrorDisconnect(h, m);
            }

            @Override
            void processOFFeaturesReply(OFChannelHandler h, OFFeaturesReply  m)
                    throws IOException, SwitchStateException {
                illegalMessageReceived(h, m);
            }

            @Override
            void processOFPortStatus(OFChannelHandler h, OFPortStatus m)
                    throws IOException {
                h.pendingPortStatusMsg.add(m);
            }
        },

        /**
         * We are waiting for the respective switch driver to complete its
         * configuration. Notice that we do not consider this to be part of the main
         * switch-controller handshake. But we do consider it as a step that comes
         * before we declare the switch as available to the controller.
         * Next State: depends on the role of this controller for this switch - either
         * MASTER or EQUAL.
         */
        WAIT_SWITCH_DRIVER_SUB_HANDSHAKE(true) {

            @Override
            void processOFError(OFChannelHandler h, OFErrorMsg m)
                    throws IOException {
                // will never be called. We override processOFMessage
            }

            @Override
            void processOFMessage(OFChannelHandler h, OFMessage m)
                    throws IOException, SwitchStateException {

                if (h.sw.isDriverHandshakeComplete()) {
                    moveToActive(h);
                    h.state.processOFMessage(h, m);
                    return;

                }

                if (m.getType() == OFType.ECHO_REQUEST) {
                    processOFEchoRequest(h, (OFEchoRequest) m);
                } else if (m.getType() == OFType.ECHO_REPLY) {
                    processOFEchoReply(h, (OFEchoReply) m);
                } else if (m.getType() == OFType.ROLE_REPLY) {
                    h.sw.handleRole(m);
                } else if (m.getType() == OFType.ERROR) {
                    if (!h.sw.handleRoleError((OFErrorMsg) m)) {
                        h.sw.processDriverHandshakeMessage(m);
                        if (h.sw.isDriverHandshakeComplete()) {
                            moveToActive(h);
                        }
                    }
                } else {
                    if (m.getType() == OFType.EXPERIMENTER &&
                            ((OFExperimenter) m).getExperimenter() ==
                            RoleManager.NICIRA_EXPERIMENTER) {
                        h.sw.handleNiciraRole(m);
                    } else {
                        h.sw.processDriverHandshakeMessage(m);
                        if (h.sw.isDriverHandshakeComplete()) {
                            moveToActive(h);
                        }
                    }
                }
            }

            @Override
            void processOFPortStatus(OFChannelHandler h, OFPortStatus m)
                    throws IOException, SwitchStateException {
                h.pendingPortStatusMsg.add(m);
            }

            private void moveToActive(OFChannelHandler h) {
                boolean success = h.sw.connectSwitch();
                // Disconnect from the device and return
                if (!success) {
                    disconnectDuplicate(h);
                    return;
                }
                handlePendingPortStatusMessages(h);
                h.setState(ACTIVE);
            }

        },

        /**
         * We are expecting a OF Multi Part Meter Features Stats Reply.
         * Notice that this information is only available for switches running
         * OpenFlow 1.3
         */
        WAIT_METER_FEATURES_REPLY(true) {

            @Override
            void processOFEchoRequest(OFChannelHandler h, OFEchoRequest m)
                    throws IOException {
                super.processOFEchoRequest(h, m);
                if (System.currentTimeMillis() - h.lastStateChange > METER_TIMEOUT) {
                    log.info("{} did not respond to MeterFeaturesRequest on time, " +
                            "moving on without it.",
                            h.getSwitchInfoString());
                   h.sendHandshakeDescriptionStatsRequest();
                   h.setState(WAIT_DESCRIPTION_STAT_REPLY);
                }
            }

            @Override
            void processOFError(OFChannelHandler h, OFErrorMsg m)
                    throws IOException {
                // Hardware switches may reply OFErrorMsg if meter is not supported
                log.warn("Received OFError {}. It seems {} does not support Meter.",
                        m.getErrType().name(), Dpid.uri(h.thisdpid));
                log.debug("{}", m);
                h.sendHandshakeDescriptionStatsRequest();
                h.setState(WAIT_DESCRIPTION_STAT_REPLY);
            }

            @Override
            void processOFStatisticsReply(OFChannelHandler h,
                                          OFStatsReply  m)
                    throws IOException, SwitchStateException {
                switch (m.getStatsType()) {
                    case METER_FEATURES:

                        log.debug("Received Meter Features");
                        OFMeterFeaturesStatsReply ofmfsr = (OFMeterFeaturesStatsReply) m;
                        log.info("Received meter features from {} with max meters: {}",
                                h.getSwitchInfoString(),
                                ofmfsr.getFeatures().getMaxMeter());
                        h.meterFeaturesReply = ofmfsr;
                        h.sendHandshakeDescriptionStatsRequest();
                        h.setState(WAIT_DESCRIPTION_STAT_REPLY);
                        break;
                    default:
                        log.error("Unexpected OF Multi Part stats reply");
                        illegalMessageReceived(h, m);
                        break;
                }
            }

            @Override
            void processOFFeaturesReply(OFChannelHandler h, OFFeaturesReply  m)
                    throws IOException, SwitchStateException {
                illegalMessageReceived(h, m);
            }

            @Override
            void processOFPortStatus(OFChannelHandler h, OFPortStatus m)
                    throws IOException {
                h.pendingPortStatusMsg.add(m);
            }

            @Override
            void processIdle(OFChannelHandler h) throws IOException {
                log.info("{} did not respond to MeterFeaturesRequest, " +
                         "moving on without it.",
                         h.getSwitchInfoString());
                h.sendHandshakeDescriptionStatsRequest();
                h.setState(WAIT_DESCRIPTION_STAT_REPLY);
            }
        },

        /**
         * This controller is in MASTER role for this switch. We enter this state
         * after requesting and winning control from the global registry.
         * The main handshake as well as the switch-driver sub-handshake
         * is complete at this point.
         * // XXX S reconsider below
         * In the (near) future we may deterministically assign controllers to
         * switches at startup.
         * We only leave this state if the switch disconnects or
         * if we send a role request for SLAVE /and/ receive the role reply for
         * SLAVE.
         */
        ACTIVE(true) {
            @Override
            void processOFError(OFChannelHandler h, OFErrorMsg m)
                    throws IOException, SwitchStateException {
                // if we get here, then the error message is for something else
                if (m.getErrType() == OFErrorType.BAD_REQUEST &&
                        (((OFBadRequestErrorMsg) m).getCode() == OFBadRequestCode.EPERM ||
                        ((OFBadRequestErrorMsg) m).getCode() == OFBadRequestCode.IS_SLAVE)) {
                    // We are the master controller and the switch returned
                    // a permission error. This is a likely indicator that
                    // the switch thinks we are slave. Reassert our role
                    // FIXME: this could be really bad during role transitions
                    // if two controllers are master (even if its only for
                    // a brief period). We might need to see if these errors
                    // persist before we reassert
                    // Scheduling in the executor to keep in line with other status events.
                    h.runtimeStatusExecutor.submit(() -> {
                        h.sw.reassertRole();
                    });
                } else if (m.getErrType() == OFErrorType.FLOW_MOD_FAILED &&
                        ((OFFlowModFailedErrorMsg) m).getCode() ==
                        OFFlowModFailedCode.ALL_TABLES_FULL) {
                    h.sw.setTableFull(true);
                } else {
                    logError(h, m);
                }
                h.dispatchMessage(m);
            }

            @Override
            void processOFStatisticsReply(OFChannelHandler h,
                    OFStatsReply m) {
                if (m.getStatsType().equals(OFStatsType.PORT_DESC)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Received port desc message from {}: {}",
                                  h.sw.getDpid(),
                                  ((OFPortDescStatsReply) m).getEntries());
                    }
                    if (m.getFlags().contains(OFStatsReplyFlags.REPLY_MORE)) {
                        log.debug("Active Stats reply indicates more stats from sw {} for "
                                          + "port description",
                                  h.getSwitchInfoString());
                        h.portDescReplies.add((OFPortDescStatsReply) m);
                        h.dispatchMessage(m);
                        return;
                    }

                    h.portDescReplies.add((OFPortDescStatsReply) m);
                    if (log.isDebugEnabled()) {
                        log.debug("Adding all Port Desc Active Replies to {}: {}",
                                  h.sw.getDpid(),
                                  h.portDescReplies);
                    }
                    h.sw.setPortDescReplies(h.portDescReplies);
                    //clearing to wait for next full response
                    h.portDescReplies.clear();
                }
                h.dispatchMessage(m);
            }

            @Override
            void processOFExperimenter(OFChannelHandler h, OFExperimenter m)
                    throws SwitchStateException {
                h.sw.handleNiciraRole(m);
            }

            @Override
            void processOFRoleReply(OFChannelHandler h, OFRoleReply m)
                    throws SwitchStateException {
                h.runtimeStatusExecutor.execute(() -> {
                    try {
                        h.sw.handleRole(m);
                    } catch (SwitchStateException e) {
                        log.error("SwitchStateException while processing " +
                                          "role reply message {}", m, e);
                        log.error("Disconnecting switch {} due to switch state error: {}",
                                  h.getSwitchInfoString(), e.getMessage());
                        h.channel.close();
                    }
                });
            }

            @Override
            void processOFPortStatus(OFChannelHandler h, OFPortStatus m)
                    throws SwitchStateException {
                // Handing over processing of port status messages to a thread to avoid
                // getting blocked on the main thread and resulting other OF message being delayed.
                // Ordering of the status messages is guaranteed by runtimeStatsExecutor being a single
                // threaded executor. This executor will execute concurrently to the netty thread; meaning
                // that the order is no  more guaranteed like it was in the past between different
                // status handling messages and other messages: statistics (port, flows, meters, groups)
                // barriers, idle, features, packet-ins handled inline to the netty thread. This executor
                // will only apply to messages during the ACTIVE state of the connection.
                h.runtimeStatusExecutor.execute(() -> {
                    try {
                        handlePortStatusMessage(h, m, true);
                    } catch (SwitchStateException e) {
                        log.error("SwitchStateException while processing " +
                                          "port status message {}", m, e);
                        log.error("Disconnecting switch {} due to switch state error: {}",
                                  h.getSwitchInfoString(), e.getMessage());
                        h.channel.close();
                    }
                });
                //h.dispatchMessage(m);
            }

            @Override
            void processOFPacketIn(OFChannelHandler h, OFPacketIn m) {
//                OFPacketOut out =
//                        h.sw.factory().buildPacketOut()
//                                .setXid(m.getXid())
//                                .setBufferId(m.getBufferId()).build();
//                h.sw.sendMsg(out);
                h.dispatchMessage(m);
            }

            @Override
            void processOFFlowRemoved(OFChannelHandler h,
                    OFFlowRemoved m) {
                h.dispatchMessage(m);
            }

            @Override
            void processOFBarrierReply(OFChannelHandler h, OFBarrierReply m) {
                h.dispatchMessage(m);
            }

            @Override
            void processOFFeaturesReply(OFChannelHandler h, OFFeaturesReply  m) {
                h.sw.setFeaturesReply(m);
                h.dispatchMessage(m);
            }

            @Override
            void processIdle(OFChannelHandler h) throws IOException {
                log.info("{} idle", h.getSwitchInfoString());
            }

        };

        private final boolean handshakeComplete;
        ChannelState(boolean handshakeComplete) {
            this.handshakeComplete = handshakeComplete;
        }

        /**
         * Is this a state in which the handshake has completed?
         * @return true if the handshake is complete
         */
        public boolean isHandshakeComplete() {
            return handshakeComplete;
        }

        /**
         * Get a string specifying the switch connection, state, and
         * message received. To be used as message for SwitchStateException
         * or log messages
         * @param h The channel handler (to get switch information_
         * @param m The OFMessage that has just been received
         * @param details A string giving more details about the exact nature
         * of the problem.
         * @return display string
         */
        // needs to be protected because enum members are actually subclasses
        protected String getSwitchStateMessage(OFChannelHandler h,
                OFMessage m,
                String details) {
            return String.format("Switch: [%s], State: [%s], received: [%s]"
                    + ", details: %s",
                    h.getSwitchInfoString(),
                    this.toString(),
                    m.getType().toString(),
                    details);
        }

        /**
         * We have an OFMessage we didn't expect given the current state and
         * we want to treat this as an error.
         * We currently throw an exception that will terminate the connection
         * However, we could be more forgiving
         * @param h the channel handler that received the message
         * @param m the message
         * @throws SwitchStateException we always throw the exception
         */
        // needs to be protected because enum members are actually subclasses
        protected void illegalMessageReceived(OFChannelHandler h, OFMessage m)
                throws SwitchStateException {
            String msg = getSwitchStateMessage(h, m,
                    "Switch should never send this message in the current state");
            throw new SwitchStateException(msg);

        }

        /**
         * We have an OFMessage we didn't expect given the current state and
         * we want to ignore the message.
         * @param h the channel handler the received the message
         * @param m the message
         */
        protected void unhandledMessageReceived(OFChannelHandler h,
                OFMessage m) {
            if (log.isDebugEnabled()) {
                String msg = getSwitchStateMessage(h, m,
                        "Ignoring unexpected message");
                log.debug(msg);
            }
        }

        /**
         * Log an OpenFlow error message from a switch.
         * @param h The switch that sent the error
         * @param error The error message
         */
        protected void logError(OFChannelHandler h, OFErrorMsg error) {
            log.error("{} from switch {} in state {}",
                    error,
                    h.getSwitchInfoString(),
                    this);
        }

        /**
         * Log an OpenFlow error message from a switch and disconnect the
         * channel.
         *
         * @param h the IO channel for this switch.
         * @param error The error message
         */
        protected void logErrorDisconnect(OFChannelHandler h, OFErrorMsg error) {
            logError(h, error);
            log.error("Disconnecting switch {}", h.getSwitchInfoString());
            h.channel.disconnect();
        }

        /**
         * log an error message for a duplicate dpid and disconnect this channel.
         * @param h the IO channel for this switch.
         */
        protected void disconnectDuplicate(OFChannelHandler h) {
            log.error("Duplicated dpid or incompleted cleanup - "
                    + "disconnecting channel {}", h.getSwitchInfoString());
            h.duplicateDpidFound = Boolean.TRUE;
            h.channel.disconnect();
        }

        /**
         * Handles all pending port status messages before a switch is declared
         * activated in MASTER or EQUAL role. Note that since this handling
         * precedes the activation (and therefore notification to IOFSwitchListerners)
         * the changes to ports will already be visible once the switch is
         * activated. As a result, no notifications are sent out for these
         * pending portStatus messages.
         *
         * @param h the channel handler that received the message
         */
        protected void handlePendingPortStatusMessages(OFChannelHandler h) {
            try {
                handlePendingPortStatusMessages(h, 0);
            } catch (SwitchStateException e) {
                log.error(e.getMessage());
            }
        }

        private void handlePendingPortStatusMessages(OFChannelHandler h, int index)
                throws SwitchStateException {
            if (h.sw == null) {
                String msg = "State machine error: switch is null. Should never " +
                        "happen";
                throw new SwitchStateException(msg);
            }
            log.info("Processing {} pending port status messages for {}",
                     h.pendingPortStatusMsg.size(), h.sw.getStringId());

            ArrayList<OFPortStatus> temp  = new ArrayList<>();
            for (OFPortStatus ps: h.pendingPortStatusMsg) {
                temp.add(ps);
                handlePortStatusMessage(h, ps, false);
            }
            // expensive but ok - we don't expect too many port-status messages
            // note that we cannot use clear(), because of the reasons below
            h.pendingPortStatusMsg.removeAll(temp);
            temp.clear();
            // the iterator above takes a snapshot of the list - so while we were
            // dealing with the pending port-status messages, we could have received
            // newer ones. Handle them recursively, but break the recursion after
            // five steps to avoid an attack.
            if (!h.pendingPortStatusMsg.isEmpty() && ++index < 5) {
                handlePendingPortStatusMessages(h, index);
            }
        }

        /**
         * Handle a port status message.
         *
         * Handle a port status message by updating the port maps in the
         * IOFSwitch instance and notifying Controller about the change so
         * it can dispatch a switch update.
         *
         * @param h The OFChannelHhandler that received the message
         * @param m The PortStatus message we received
         * @param doNotify if true switch port changed events will be
         * dispatched
         * @throws SwitchStateException if the switch is not bound to the channel
         *
         */
        protected void handlePortStatusMessage(OFChannelHandler h, OFPortStatus m,
                boolean doNotify) throws SwitchStateException {
            if (h.sw == null) {
                String msg = getSwitchStateMessage(h, m,
                        "State machine error: switch is null. Should never " +
                        "happen");
                throw new SwitchStateException(msg);
            }
            log.info("Received port status message from {}/{}: {}",
                     h.sw.getDpid(), m.getDesc().getPortNo(), m);

            h.sw.handleMessage(m);
        }

        /**
         * Process an OF message received on the channel and
         * update state accordingly.
         *
         * The main "event" of the state machine. Process the received message,
         * send follow up message if required and update state if required.
         *
         * Switches on the message type and calls more specific event handlers
         * for each individual OF message type. If we receive a message that
         * is supposed to be sent from a controller to a switch we throw
         * a SwitchStateExeption.
         *
         * The more specific handlers can also throw SwitchStateExceptions
         *
         * @param h The OFChannelHandler that received the message
         * @param m The message we received.
         * @throws SwitchStateException if the switch is not bound to the channel
         * @throws IOException if unable to send message back to the switch
         */
        void processOFMessage(OFChannelHandler h, OFMessage m)
                throws IOException, SwitchStateException {
            switch (m.getType()) {
            case HELLO:
                processOFHello(h, (OFHello) m);
                break;
            case BARRIER_REPLY:
                processOFBarrierReply(h, (OFBarrierReply) m);
                break;
            case ECHO_REPLY:
                processOFEchoReply(h, (OFEchoReply) m);
                break;
            case ECHO_REQUEST:
                processOFEchoRequest(h, (OFEchoRequest) m);
                break;
            case ERROR:
                processOFError(h, (OFErrorMsg) m);
                break;
            case FEATURES_REPLY:
                processOFFeaturesReply(h, (OFFeaturesReply) m);
                break;
            case FLOW_REMOVED:
                processOFFlowRemoved(h, (OFFlowRemoved) m);
                break;
            case GET_CONFIG_REPLY:
                processOFGetConfigReply(h, (OFGetConfigReply) m);
                break;
            case PACKET_IN:
                processOFPacketIn(h, (OFPacketIn) m);
                break;
            case PORT_STATUS:
                processOFPortStatus(h, (OFPortStatus) m);
                break;
            case QUEUE_GET_CONFIG_REPLY:
                processOFQueueGetConfigReply(h, (OFQueueGetConfigReply) m);
                break;
            case STATS_REPLY: // multipart_reply in 1.3
                processOFStatisticsReply(h, (OFStatsReply) m);
                break;
            case EXPERIMENTER:
                processOFExperimenter(h, (OFExperimenter) m);
                break;
            case ROLE_REPLY:
                processOFRoleReply(h, (OFRoleReply) m);
                break;
            case GET_ASYNC_REPLY:
                processOFGetAsyncReply(h, (OFAsyncGetReply) m);
                break;

                // The following messages are sent to switches. The controller
                // should never receive them
            case SET_CONFIG:
            case GET_CONFIG_REQUEST:
            case PACKET_OUT:
            case PORT_MOD:
            case QUEUE_GET_CONFIG_REQUEST:
            case BARRIER_REQUEST:
            case STATS_REQUEST: // multipart request in 1.3
            case FEATURES_REQUEST:
            case FLOW_MOD:
            case GROUP_MOD:
            case TABLE_MOD:
            case GET_ASYNC_REQUEST:
            case SET_ASYNC:
            case METER_MOD:
            default:
                illegalMessageReceived(h, m);
                break;
            }
        }

        /*-----------------------------------------------------------------
         * Default implementation for message handlers in any state.
         *
         * Individual states must override these if they want a behavior
         * that differs from the default.
         *
         * In general, these handlers simply ignore the message and do
         * nothing.
         *
         * There are some exceptions though, since some messages really
         * are handled the same way in every state (e.g., ECHO_REQUST) or
         * that are only valid in a single state (e.g., HELLO, GET_CONFIG_REPLY
         -----------------------------------------------------------------*/

        void processOFHello(OFChannelHandler h, OFHello m)
                throws IOException, SwitchStateException {
            // we only expect hello in the WAIT_HELLO state
            log.warn("Received Hello outside WAIT_HELLO state; switch {} is not complaint.",
                     h.channel.remoteAddress());
        }

        void processOFBarrierReply(OFChannelHandler h, OFBarrierReply m)
                throws IOException {
            // Silently ignore.
        }

        void processOFEchoRequest(OFChannelHandler h, OFEchoRequest m)
                throws IOException {
            if (h.ofVersion == null) {
                log.error("No OF version set for {}. Not sending Echo REPLY",
                        h.channel.remoteAddress());
                return;
            }
            OFEchoReply reply = h.factory
                    .buildEchoReply()
                    .setXid(m.getXid())
                    .setData(m.getData())
                    .build();
            h.channel.writeAndFlush(Collections.singletonList(reply));
        }

        void processOFEchoReply(OFChannelHandler h, OFEchoReply m)
                throws IOException {
            // Do nothing with EchoReplies !!
        }

        // no default implementation for OFError
        // every state must override it
        abstract void processOFError(OFChannelHandler h, OFErrorMsg m)
                throws IOException, SwitchStateException;


        void processOFFeaturesReply(OFChannelHandler h, OFFeaturesReply  m)
                throws IOException, SwitchStateException {
            unhandledMessageReceived(h, m);
        }

        void processOFFlowRemoved(OFChannelHandler h, OFFlowRemoved m)
                throws IOException {
            unhandledMessageReceived(h, m);
        }

        void processOFGetConfigReply(OFChannelHandler h, OFGetConfigReply m)
                throws IOException, SwitchStateException {
            // we only expect config replies in the WAIT_CONFIG_REPLY state
            illegalMessageReceived(h, m);
        }

        void processOFPacketIn(OFChannelHandler h, OFPacketIn m)
                throws IOException {
            unhandledMessageReceived(h, m);
        }

        // no default implementation. Every state needs to handle it.
        abstract void processOFPortStatus(OFChannelHandler h, OFPortStatus m)
                throws IOException, SwitchStateException;

        void processOFQueueGetConfigReply(OFChannelHandler h,
                OFQueueGetConfigReply m)
                        throws IOException {
            unhandledMessageReceived(h, m);
        }

        void processOFStatisticsReply(OFChannelHandler h, OFStatsReply m)
                throws IOException, SwitchStateException {
            unhandledMessageReceived(h, m);
        }

        void processOFExperimenter(OFChannelHandler h, OFExperimenter m)
                throws IOException, SwitchStateException {
            // TODO: it might make sense to parse the vendor message here
            // into the known vendor messages we support and then call more
            // specific event handlers
            unhandledMessageReceived(h, m);
        }

        void processOFRoleReply(OFChannelHandler h, OFRoleReply m)
                throws SwitchStateException, IOException {
            unhandledMessageReceived(h, m);
        }

        void processOFGetAsyncReply(OFChannelHandler h,
                OFAsyncGetReply m) {
            unhandledMessageReceived(h, m);
        }

        void processIdle(OFChannelHandler h) throws IOException {
            // disconnect channel which did no complete handshake
            log.error("{} idle in state {}, disconnecting", h.getSwitchInfoString(), this);
            h.channel.disconnect();
        }
    }

    //*************************
    //  Channel handler methods
    //*************************

    @Override
    public void channelActive(ChannelHandlerContext ctx)
            throws Exception {

        channel = ctx.channel();
        log.info("New switch connection from {}",
                 channel.remoteAddress());

        SocketAddress address = channel.remoteAddress();
        if (address instanceof InetSocketAddress) {
            final InetSocketAddress inetAddress = (InetSocketAddress) address;
            final IpAddress ipAddress = IpAddress.valueOf(inetAddress.getAddress());
            if (ipAddress.isIp4()) {
                channelId = ipAddress.toString() + ':' + inetAddress.getPort();
            } else {
                channelId = '[' + ipAddress.toString() + "]:" + inetAddress.getPort();
            }
        } else {
            channelId = channel.toString();
        }

        dispatcher = Executors.newSingleThreadExecutor(groupedThreads("onos/of/dispatcher", channelId, log));

        /*
            hack to wait for the switch to tell us what it's
            max version is. This is not spec compliant and should
            be removed as soon as switches behave better.
         */
        //sendHandshakeHelloMessage();
        setState(ChannelState.WAIT_HELLO);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx)
            throws Exception {
        log.info("Switch disconnected callback for sw:{}. Cleaning up ...",
                 getSwitchInfoString());

        if (dispatcher != null) {
            dispatcher.shutdownNow();
            dispatcher = null;
        }

        if (thisdpid != 0) {
            if (!duplicateDpidFound) {
                // if the disconnected switch (on this ChannelHandler)
                // was not one with a duplicate-dpid, it is safe to remove all
                // state for it at the controller. Notice that if the disconnected
                // switch was a duplicate-dpid, calling the method below would clear
                // all state for the original switch (with the same dpid),
                // which we obviously don't want.
                runtimeStatusExecutor.submit(() -> {
                    log.info("{}:removal called", getSwitchInfoString());
                    if (sw != null) {
                        sw.removeConnectedSwitch();
                    }
                });
            } else {
                // A duplicate was disconnected on this ChannelHandler,
                // this is the same switch reconnecting, but the original state was
                // not cleaned up - XXX check liveness of original ChannelHandler
                log.info("{}:duplicate found", getSwitchInfoString());
                duplicateDpidFound = Boolean.FALSE;
            }
        } else {
            log.warn("no dpid in channelHandler registered for "
                             + "disconnected switch {}", getSwitchInfoString());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx,
                                Throwable cause)
            throws Exception {

        if (cause instanceof ReadTimeoutException) {
            // switch timeout
            log.error("Disconnecting switch {} due to read timeout",
                    getSwitchInfoString());
            ctx.channel().close();
        } else if (cause instanceof HandshakeTimeoutException) {
            log.error("Disconnecting switch {}: failed to complete handshake",
                    getSwitchInfoString());
            ctx.channel().close();
        } else if (cause instanceof ClosedChannelException) {
            log.debug("Channel for sw {} already closed", getSwitchInfoString());
        } else if (cause instanceof IOException) {
            if (!cause.getMessage().equals(RESET_BY_PEER) &&
                    !cause.getMessage().equals(BROKEN_PIPE)) {
                log.error("Disconnecting switch {} due to IO Error: {}",
                          getSwitchInfoString(), cause.getMessage());
                if (log.isDebugEnabled()) {
                    // still print stack trace if debug is enabled
                    log.debug("StackTrace for previous Exception: ", cause);
                }
            }
            ctx.channel().close();
        } else if (cause instanceof SwitchStateException) {
            log.error("Disconnecting switch {} due to switch state error: {}",
                    getSwitchInfoString(), cause.getMessage());
            if (log.isDebugEnabled()) {
                // still print stack trace if debug is enabled
                log.debug("StackTrace for previous Exception: ", cause);
            }
            ctx.channel().close();
        } else if (cause instanceof OFParseError) {
            log.error("Disconnecting switch "
                    + getSwitchInfoString() +
                    " due to message parse failure",
                    cause);
            ctx.channel().close();
        } else if (cause instanceof RejectedExecutionException) {
            log.warn("Could not process message: queue full");
        } else {
            log.error("Error while processing message from switch "
                    + getSwitchInfoString()
                    + "state " + this.state, cause);
            ctx.channel().close();
        }
    }

    @Override
    public String toString() {
        return getSwitchInfoString();
    }

    // We have reduced the idle period, the idea is to use
    // the IdleHandler to perform also some sanity checks.
    // Previous code is still executed with the same frequency
    // which is IDLE_INTERVAL * MAX_IDLE_RETRY of inactivity
    private void channelIdle(ChannelHandlerContext ctx,
                               IdleStateEvent e)
            throws IOException {
        // dispatcher terminated for some reason, restart
        if (dispatcherHandle.isDone()) {
            dispatcherHandle = dispatcher.submit(new Dispatcher());
        }
        // drain the backlog
        processDispatchBacklogQueue();
        // Original timeout reached
        if (--maxIdleRetry == 0) {
            maxIdleRetry = MAX_IDLE_RETRY;
            // Factory can be null if the channel goes idle during initial handshake. Since the switch
            // is not even initialized properly, we just skip this and disconnect the channel.
            if (factory != null) {
                // send an echo request each time idle_timeout * TICK
                OFMessage m = factory.buildEchoRequest().build();
                log.info("Sending Echo Request on idle channel: {}", ctx.channel());
                // XXX S some problems here -- echo request has no transaction id, and
                // echo reply is not correlated to the echo request.
                ctx.writeAndFlush(Collections.singletonList(m), ctx.voidPromise());
            }
            state.processIdle(this);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx,
                                   Object evt)
            throws Exception {
        // If the connection is READER/WRITER idle try to send an echo request
        if (evt instanceof IdleStateEvent) {
            log.debug("Channel {} is {}", ctx.channel(), ((IdleStateEvent) evt).state());
            channelIdle(ctx, (IdleStateEvent) evt);
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    // SimpleChannelInboundHandler without dependency to TypeParameterMatcher
    @Override
    public void channelRead(ChannelHandlerContext ctx,
                            Object msg) throws Exception {

        boolean release = true;
        maxIdleRetry = MAX_IDLE_RETRY;
        try {
            if (msg instanceof OFMessage) {
                // channelRead0 inlined
                state.processOFMessage(this, (OFMessage) msg);
            } else {
                release = false;
                ctx.fireChannelRead(msg);
            }
        } finally {
            if (release) {
                ReferenceCountUtil.release(msg);
            }
        }
    }




    //*************************
    //  Channel utility methods
    //*************************

    /**
     * Is this a state in which the handshake has completed?
     *
     * @return true if the handshake is complete
     */
    public boolean isHandshakeComplete() {
        return this.state.isHandshakeComplete();
    }

    /**
     * Increment totalCount variable and send signal to executor.
     */
    private void incrementAndSignal() {
        try {
            totalCount.incrementAndGet();
            takeLock.lockInterruptibly();
            try {
                notEmpty.signal();
            } finally {
                takeLock.unlock();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Try to push OpenFlow message to queue.
     *
     * @param message OpenFlow message
     * @param idQueue id of Queue
     * @return true if message was successful added to queue
     */
    private boolean pushMessageToQueue(OFMessage message, int idQueue) {
        if (!dispatchQueuesMapProducer.get(idQueue).offer(message)) {
            return false;
        } else {
            incrementAndSignal();
            return true;
        }
    }

    /**
     * Process backlog - move messages from backlog to default queue.
     *
     * @return true if whole backlog was processed, otherwise false
     */
    private boolean processDispatchBacklogQueue() {
        while (!dispatchBacklog.isEmpty()) {
            OFMessage msgFromBacklog = dispatchBacklog.removeFirst();
            if (!pushMessageToQueue(msgFromBacklog, NUM_OF_QUEUES - 1)) {
                dispatchBacklog.addFirst(msgFromBacklog);
                return false;
            }
        }
        return true;

    }

    /**
     * Parse OpenFlow message context for get Ethernet packet.
     *
     * @param message OpenFlow message
     * @return parsed Ethernet packet
     */
    private Ethernet parsePacketInMessage(OFMessage message) {
        OpenFlowPacketContext pktCtx = DefaultOpenFlowPacketContext
                .packetContextFromPacketIn(sw, (OFPacketIn) message);
        DeviceId id = DeviceId.deviceId(Dpid.uri(pktCtx.dpid().value()));
        DefaultInboundPacket inPkt = new DefaultInboundPacket(
                new ConnectPoint(id, PortNumber.portNumber(pktCtx.inPort())),
                pktCtx.parsed(), ByteBuffer.wrap(pktCtx.unparsed()),
                pktCtx.cookie());
        return inPkt.parsed();
    }

    /**
     * Classify the Ethernet packet for membership on one of the queues.
     *
     * @param packet ethernet packet
     * @return Id of destination Queue
     */
    private int classifyEthernetPacket(Ethernet packet) {
        for (Set<OpenFlowClassifier> classifiers : this.messageClassifiersMapProducer) {
            for (OpenFlowClassifier classifier : classifiers) {
                if (classifier.ethernetType() == packet.getEtherType()) {
                    return classifier.idQueue();
                }
            }
        }
        return NUM_OF_QUEUES - 1;
    }

    /**
     * Process messages from dispatch queues.
     *
     * @param queuesSize count of messages in all queues
     */
    private void processMessages(int queuesSize) {
        List<OFMessage> msgs = new ArrayList<>();
        int processed;
        do {
            processed = 0;
            while (processed < queuesSize) {
                for (LinkedBlockingMessagesQueue<OFMessage> queue :
                        dispatchQueuesMapProducer.values()) {
                    processed += queue.drainTo(msgs);
                }
            }

            msgs.forEach(sw::handleMessage);
            msgs.clear();
            /* Decrement conditional variable */
            queuesSize = totalCount.addAndGet(-1 * processed);
        } while (queuesSize > 0);
    }

    private void dispatchMessage(OFMessage m) {
        log.debug("Begin dispatch OpenFlow Message");
        boolean backlogEmpty = processDispatchBacklogQueue();
        if (m.getType() == OFType.PACKET_IN) {
            Ethernet pkt = parsePacketInMessage(m);
            pushMessageToQueue(m, classifyEthernetPacket(pkt));
        } else {
            if (!backlogEmpty || !pushMessageToQueue(m, NUM_OF_QUEUES - 1)) {
                dispatchBacklog.offer(m);
            }
        }

        if (dispatcherHandle.isDone()) {
            // dispatcher terminated for some reason, restart
            dispatcherHandle = dispatcher.submit(new Dispatcher());
        }
    }

    private final class Dispatcher implements Runnable {
        // dispatch loop
        @Override
        public void run() {
            try {
                for (;;) {
                    int tc = 0;
                    takeLock.lockInterruptibly();
                    try {
                        while ((tc = totalCount.get()) == 0) {
                            notEmpty.await();
                        }
                    } finally {
                        takeLock.unlock();
                    }

                    processMessages(tc);
                }
            } catch (InterruptedException e) {
                log.warn("Dispatcher interrupted");
                Thread.currentThread().interrupt();
                // interrupted. gracefully shutting down
                return;
            }
        }
    }

    /**
     * Return a string describing this switch based on the already available
     * information (DPID and/or remote socket).
     * @return display string
     */
    private String getSwitchInfoString() {
        if (sw != null) {
            return sw.toString();
        }
        String channelString;
        if (channel == null || channel.remoteAddress() == null) {
            channelString = "?";
        } else {
            channelString = channel.remoteAddress().toString();
        }
        String dpidString;
        if (featuresReply == null) {
            dpidString = "?";
        } else {
            dpidString = featuresReply.getDatapathId().toString();
        }
        return String.format("[%s DPID[%s]]", channelString, dpidString);
    }

    /**
     * Update the channels state. Only called from the state machine.
     * TODO: enforce restricted state transitions
     * @param state new state
     */
    void setState(ChannelState state) {
        this.state = state;
        this.lastStateChange = System.currentTimeMillis();
    }

    private boolean setDpid(Long dpid, Channel channel) {
        ChannelHandlerContext sslContext = channel.pipeline().context(SslHandler.class);
        if (sslContext != null) {
            try {
                SslHandler sslHandler = (SslHandler) sslContext.handler();
                Certificate[] certs = sslHandler.engine().getSession().getPeerCertificates();
                Certificate cert = certs.length > 0 ? certs[0] : null;
                if (!controller.isValidCertificate(dpid, cert)) {
                    return false;
                }
            } catch (SSLPeerUnverifiedException e) {
                log.info("Switch with dpid {} is an unverified SSL peer.", dpid, e);
                return false;
            }
        }
        this.thisdpid = dpid;
        return true;
    }

    /**
     * Send hello message to the switch using the handshake transactions ids.
     * @throws IOException
     */
    private void sendHandshakeHelloMessage() throws IOException {
        // The OF protocol requires us to start things off by sending the highest
        // version of the protocol supported.

        // bitmap represents OF1.0, OF1.3, OF1.4, and OF1.5
        // see Sec. 7.5.1 of the OF1.3.4 spec
        U32 bitmap = U32.ofRaw((0b1 << OFVersion.OF_10.getWireVersion()) |
                               (0b1 << OFVersion.OF_13.getWireVersion()) |
                               (0b1 << OFVersion.OF_14.getWireVersion()) |
                               (0b1 << OFVersion.OF_15.getWireVersion()));
        OFVersion version = Optional.ofNullable(ofVersion).orElse(OFVersion.OF_13);
        OFHelloElem hem = OFFactories.getFactory(version)
                .buildHelloElemVersionbitmap()
                .setBitmaps(Collections.singletonList(bitmap))
                .build();
        OFMessage.Builder mb = OFFactories.getFactory(version)
                .buildHello()
                .setXid(this.handshakeTransactionIds--)
                .setElements(Collections.singletonList(hem));
        log.info("Sending {} Hello to {}", version, channel.remoteAddress());
        channel.writeAndFlush(Collections.singletonList(mb.build()));
    }

    /**
     * Send featuresRequest msg to the switch using the handshake transactions ids.
     * @throws IOException
     */
    private void sendHandshakeFeaturesRequestMessage() throws IOException {
        log.debug("Sending FEATURES_REQUEST to {}", channel.remoteAddress());
        OFMessage m = factory.buildFeaturesRequest()
                .setXid(this.handshakeTransactionIds--)
                .build();
        channel.writeAndFlush(Collections.singletonList(m));
    }

    /**
     * Send the configuration requests to tell the switch we want full
     * packets.
     * @throws IOException
     */
    private void sendHandshakeSetConfig() throws IOException {
        log.debug("Sending CONFIG_REQUEST to {}", channel.remoteAddress());
        List<OFMessage> msglist = new ArrayList<>(3);

        // Ensure we receive the full packet via PacketIn
        // FIXME: We don't set the reassembly flags.
        // Only send config to switches to send full packets, if they have a buffer.
        // Saves a packet & OFSetConfig can't be handled by certain switches.
        if (this.featuresReply.getNBuffers() > 0) {
            OFSetConfig sc = factory
                    .buildSetConfig()
                    .setMissSendLen((short) 0xffff)
                    .setXid(this.handshakeTransactionIds--)
                    .build();
            msglist.add(sc);
        }

        // Barrier
        OFBarrierRequest br = factory
                .buildBarrierRequest()
                .setXid(this.handshakeTransactionIds--)
                .build();
        msglist.add(br);

        // Verify (need barrier?)
        OFGetConfigRequest gcr = factory
                .buildGetConfigRequest()
                .setXid(this.handshakeTransactionIds--)
                .build();
        msglist.add(gcr);
        channel.writeAndFlush(msglist);
    }

    /**
     * send a description state request.
     * @throws IOException
     */
    private void sendHandshakeDescriptionStatsRequest() throws IOException {
        // Get Description to set switch-specific flags
        log.debug("Sending DESC_STATS_REQUEST to {}", channel.remoteAddress());
        OFDescStatsRequest dreq = factory
                .buildDescStatsRequest()
                .setXid(handshakeTransactionIds--)
                .build();
        channel.writeAndFlush(Collections.singletonList(dreq));
    }

    /**
     * send a meter features request.
     *
     * @throws IOException
     */
    private void sendMeterFeaturesRequest() throws IOException {
        // Get meter features including the MaxMeters value available for the device
        OFFactory factory = OFFactories.getFactory(ofVersion);
        log.debug("Sending METER_FEATURES_REQUEST to {}", channel.remoteAddress());
        OFMeterFeaturesStatsRequest mfreq = factory
                .buildMeterFeaturesStatsRequest()
                .setXid(handshakeTransactionIds--)
                .build();
        channel.writeAndFlush(Collections.singletonList(mfreq));
    }

    private void sendHandshakeOFPortDescRequest() throws IOException {
        log.debug("Sending OF_PORT_DESC_REQUEST to {}", channel.remoteAddress());
        // Get port description for 1.3+ switch
        OFPortDescStatsRequest preq = factory
                .buildPortDescStatsRequest()
                .setXid(handshakeTransactionIds--)
                .build();
        channel.writeAndFlush(Collections.singletonList(preq));
    }

    ChannelState getStateForTesting() {
        return state;
    }


    @Override
    public boolean isActive() {
        if (channel != null) {
            return channel.isActive();
        }
        return false;
    }

    @Override
    public void closeSession() {
        if (channel != null) {
            channel.close();
        }
    }

    @Override
    public boolean sendMsg(Iterable<OFMessage> msgs) {
        if (channel.isActive()) {
            if (log.isTraceEnabled()) {
                log.trace("Sending messages for switch {} via openflow channel: {}", getSwitchInfoString(), msgs);
            }
            channel.writeAndFlush(msgs, channel.voidPromise());
            return true;
        } else {
            log.warn("Dropping messages for switch {} because channel is not connected: {}",
                     getSwitchInfoString(), msgs);
            return false;
        }
    }

    @Override
    public CharSequence sessionInfo() {
        return channelId;
    }

    @Override
    public void addClassifier(OpenFlowClassifier classifier) {
        if (this.deviceId.equals(classifier.deviceId())) {
            log.debug("Add OpenFlow Classifier for switch {} to queue {} with type {}",
                     classifier.deviceId().toString(), classifier.idQueue(), classifier.ethernetType());
            this.messageClassifiersMapProducer.get(classifier.idQueue()).add(classifier);
        }
    }

    @Override
    public void removeClassifier(OpenFlowClassifier classifier) {
        if (this.deviceId.equals(classifier.deviceId())) {
            log.debug("Remove OpenFlow Classifier for switch {} from queue {} with type {}",
                      classifier.deviceId().toString(), classifier.idQueue(), classifier.ethernetType());
            this.messageClassifiersMapProducer.get(classifier.idQueue()).remove(classifier);
        }
    }

    /**
     * Init classifier configuration for the switch. Use stored configuration if exist.
     * Otherwise add LLDP and BDDP classifiers for Queue N0.
     */
    private void initClassifiers() {
        try {
            openFlowManager = DefaultServiceDirectory.getService(OpenFlowService.class);
            DeviceId did = DeviceId.deviceId(uri(thisdpid));
            Set<OpenFlowClassifier> classifiers = openFlowManager.getClassifiersByDeviceId(did);
            if (classifiers == null) {
                OpenFlowClassifier classifier =
                        new OpenFlowClassifier.Builder(did, 0).ethernetType(TYPE_LLDP).build();
                openFlowManager.add(classifier);
                classifier = new OpenFlowClassifier.Builder(did, 0).ethernetType(TYPE_BSN).build();
                openFlowManager.add(classifier);
            } else {
                this.messageClassifiersMapProducer.forEach((v) -> {
                        v.clear();
                });
                classifiers.forEach((c) -> {
                        messageClassifiersMapProducer.get(c.idQueue()).add(c);
                });
            }
        } catch (Exception e) {
            log.error("Initialize default classifier failed: {}", e.toString());
            e.printStackTrace();
        }
    }
}
