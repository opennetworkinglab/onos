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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.basics.SubjectFactories;
import org.onosproject.net.driver.DriverService;
import org.onosproject.openflow.config.OpenFlowDeviceConfig;
import org.onosproject.openflow.controller.DefaultOpenFlowPacketContext;
import org.onosproject.openflow.controller.Dpid;
import org.onosproject.openflow.controller.OpenFlowClassifierListener;
import org.onosproject.openflow.controller.OpenFlowController;
import org.onosproject.openflow.controller.OpenFlowEventListener;
import org.onosproject.openflow.controller.OpenFlowMessageListener;
import org.onosproject.openflow.controller.OpenFlowPacketContext;
import org.onosproject.openflow.controller.OpenFlowSwitch;
import org.onosproject.openflow.controller.OpenFlowSwitchListener;
import org.onosproject.openflow.controller.OpenFlowListener;
import org.onosproject.openflow.controller.OpenFlowService;
import org.onosproject.openflow.controller.OpenFlowEvent;
import org.onosproject.openflow.controller.PacketListener;
import org.onosproject.openflow.controller.RoleState;
import org.onosproject.openflow.controller.driver.OpenFlowAgent;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.projectfloodlight.openflow.protocol.OFCalientFlowStatsEntry;
import org.projectfloodlight.openflow.protocol.OFCalientFlowStatsReply;
import org.projectfloodlight.openflow.protocol.OFCircuitPortStatus;
import org.projectfloodlight.openflow.protocol.OFExperimenter;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFlowLightweightStatsEntry;
import org.projectfloodlight.openflow.protocol.OFFlowLightweightStatsReply;
import org.projectfloodlight.openflow.protocol.OFFlowStatsEntry;
import org.projectfloodlight.openflow.protocol.OFFlowStatsReply;
import org.projectfloodlight.openflow.protocol.OFGroupDescStatsEntry;
import org.projectfloodlight.openflow.protocol.OFGroupDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFGroupStatsEntry;
import org.projectfloodlight.openflow.protocol.OFGroupStatsReply;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFPortStatsEntry;
import org.projectfloodlight.openflow.protocol.OFPortStatus;
import org.projectfloodlight.openflow.protocol.OFQueueStatsEntry;
import org.projectfloodlight.openflow.protocol.OFQueueStatsReply;
import org.projectfloodlight.openflow.protocol.OFStatsReply;
import org.projectfloodlight.openflow.protocol.OFStatsReplyFlags;
import org.projectfloodlight.openflow.protocol.OFTableStatsEntry;
import org.projectfloodlight.openflow.protocol.OFTableStatsReply;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.instruction.OFInstruction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.openflow.controller.impl.OsgiPropertyConstants.*;

@Component(
        immediate = true,
        service = OpenFlowController.class,
        property = {
                OFPORTS + "=" + OFPORTS_DEFAULT,
                WORKER_THREADS + ":Integer=" + WORKER_THREADS_DEFAULT,
                TLS_MODE + "=" + TLS_MODE_DEFAULT,
                KEY_STORE + "=" + KEY_STORE_DEFAULT,
                KEY_STORE_PASSWORD + "=" + KEY_STORE_PASSWORD_DEFAULT,
                TRUST_STORE + "=" + TRUST_STORE_DEFAULT,
                TRUST_STORE_PASSWORD + "=" + TRUST_STORE_PASSWORD_DEFAULT,
                DEFAULT_QUEUE_SIZE + ":Integer=" + DEFAULT_QUEUE_SIZE_DEFAULT,
                DEBAULT_BULK_SIZE + ":Integer=" + BULK_SIZE_DEFAULT,
                QUEUE_SIZE_N0 + ":Integer=" + QUEUE_SIZE_N0_DEFAULT,
                BULK_SIZE_N0 + ":Integer=" + BULK_SIZE_DEFAULT,
                QUEUE_SIZE_N1 + ":Integer=" + QUEUE_SIZE_DEFAULT,
                BULK_SIZE_N1 + ":Integer=" + BULK_SIZE_DEFAULT,
                QUEUE_SIZE_N2 + ":Integer=" + QUEUE_SIZE_DEFAULT,
                BULK_SIZE_N2 + ":Integer=" + BULK_SIZE_DEFAULT,
                QUEUE_SIZE_N3 + ":Integer=" + QUEUE_SIZE_DEFAULT,
                BULK_SIZE_N3 + ":Integer=" + BULK_SIZE_DEFAULT,
                QUEUE_SIZE_N4 + ":Integer=" + QUEUE_SIZE_DEFAULT,
                BULK_SIZE_N4 + ":Integer=" + BULK_SIZE_DEFAULT,
                QUEUE_SIZE_N5 + ":Integer=" + QUEUE_SIZE_DEFAULT,
                BULK_SIZE_N5 + ":Integer=" + BULK_SIZE_DEFAULT,
                QUEUE_SIZE_N6 + ":Integer=" + QUEUE_SIZE_DEFAULT,
                BULK_SIZE_N6 + ":Integer=" + BULK_SIZE_DEFAULT,
        }
)

public class OpenFlowControllerImpl implements OpenFlowController {
    private static final String APP_ID = "org.onosproject.openflow-base";
    protected static final String SCHEME = "of";

    private static final Logger log =
            LoggerFactory.getLogger(OpenFlowControllerImpl.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DriverService driverService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected NetworkConfigRegistry netCfgService;

    /** Port numbers (comma separated) used by OpenFlow protocol; default is 6633,6653. */
    private String openflowPorts = OFPORTS_DEFAULT;

    /** Number of controller worker threads. */
    private int workerThreads = WORKER_THREADS_DEFAULT;

    /** TLS mode for OpenFlow channel; options are: disabled [default], enabled, strict. */
    private String tlsMode;

    /** File path to key store for TLS connections. */
    private String keyStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OpenFlowService openFlowManager;

    private final OpenFlowListener openFlowListener = new InternalOpenFlowListener();

    /** Key store password. */
    private String keyStorePassword;

    /** File path to trust store for TLS connections. */
    private String trustStore;

    /** Trust store password. */
    private String trustStorePassword;

    /** Size of deafult queue. */
    private int defaultQueueSize = DEFAULT_QUEUE_SIZE_DEFAULT;

    /** Size of deafult bulk. */
    private int defaultBulkSize = BULK_SIZE_DEFAULT;

    /** Size of queue N0. */
    private int queueSizeN0 = QUEUE_SIZE_N0_DEFAULT;

    /** Size of bulk N0. */
    private int bulkSizeN0 = BULK_SIZE_DEFAULT;

    /** Size of queue N1. */
    private int queueSizeN1 = QUEUE_SIZE_DEFAULT;

    /** Size of bulk N1. */
    private int bulkSizeN1 = BULK_SIZE_DEFAULT;

    /** Size of queue N2. */
    private int queueSizeN2 = QUEUE_SIZE_DEFAULT;

    /** Size of bulk N2. */
    private int bulkSizeN2 = BULK_SIZE_DEFAULT;

    /** Size of queue N3. */
    private int queueSizeN3 = QUEUE_SIZE_DEFAULT;

    /** Size of bulk N3. */
    private int bulkSizeN3 = BULK_SIZE_DEFAULT;

    /** Size of queue N4. */
    private int queueSizeN4 = QUEUE_SIZE_DEFAULT;

    /** Size of bulk N4. */
    private int bulkSizeN4 = BULK_SIZE_DEFAULT;

    /** Size of queue N5. */
    private int queueSizeN5 = QUEUE_SIZE_DEFAULT;

    /** Size of bulk N5. */
    private int bulkSizeN5 = BULK_SIZE_DEFAULT;

    /** Size of queue N6. */
    private int queueSizeN6 = QUEUE_SIZE_DEFAULT;

    /** Size of bulk N6. */
    private int bulkSizeN6 = BULK_SIZE_DEFAULT;

    protected ExecutorService executorMsgs =
        Executors.newFixedThreadPool(32, groupedThreads("onos/of", "event-stats-%d", log));

    private final ExecutorService executorBarrier =
        Executors.newFixedThreadPool(4, groupedThreads("onos/of", "event-barrier-%d", log));

    //Separate executor thread for handling error messages and barrier replies for same failed
    // transactions to avoid context switching of thread
    protected ExecutorService executorErrorMsgs =
            Executors.newSingleThreadExecutor(groupedThreads("onos/of", "event-error-msg-%d", log));

    //concurrent hashmap to track failed transactions
    protected ConcurrentMap<Long, Boolean> errorMsgs =
            new ConcurrentHashMap<>();
    protected ConcurrentMap<Dpid, OpenFlowSwitch> connectedSwitches =
            new ConcurrentHashMap<>();
    protected ConcurrentMap<Dpid, OpenFlowSwitch> activeMasterSwitches =
            new ConcurrentHashMap<>();
    protected ConcurrentMap<Dpid, OpenFlowSwitch> activeEqualSwitches =
            new ConcurrentHashMap<>();

    // Key: dpid, value: map with key: long (XID), value: completable future
    protected ConcurrentMap<Dpid, ConcurrentMap<Long, CompletableFuture<OFMessage>>> responses =
            new ConcurrentHashMap<>();

    protected OpenFlowSwitchAgent agent = new OpenFlowSwitchAgent();
    protected Set<OpenFlowSwitchListener> ofSwitchListener = new CopyOnWriteArraySet<>();

    protected Multimap<Integer, PacketListener> ofPacketListener =
            ArrayListMultimap.create();

    protected Set<OpenFlowEventListener> ofEventListener = new CopyOnWriteArraySet<>();

    protected Set<OpenFlowClassifierListener> ofClassifierListener = new CopyOnWriteArraySet<>();

    protected Set<OpenFlowMessageListener> ofMessageListener = new CopyOnWriteArraySet<>();

    protected Multimap<Dpid, OFFlowStatsEntry> fullFlowStats =
            ArrayListMultimap.create();

    protected Multimap<Dpid, OFFlowLightweightStatsEntry> fullFlowLightweightStats =
            ArrayListMultimap.create();

    protected Multimap<Dpid, OFTableStatsEntry> fullTableStats =
            ArrayListMultimap.create();

    protected Multimap<Dpid, OFGroupStatsEntry> fullGroupStats =
            ArrayListMultimap.create();

    protected Multimap<Dpid, OFGroupDescStatsEntry> fullGroupDescStats =
            ArrayListMultimap.create();

    // deprecated in 1.11.0, no longer referenced from anywhere
    @Deprecated
    protected Multimap<Dpid, OFPortStatsEntry> fullPortStats =
            ArrayListMultimap.create();

    protected Multimap<Dpid, OFQueueStatsEntry> fullQueueStats =
            ArrayListMultimap.create();

    protected final ConfigFactory factory =
            new ConfigFactory<DeviceId, OpenFlowDeviceConfig>(
                    SubjectFactories.DEVICE_SUBJECT_FACTORY,
                    OpenFlowDeviceConfig.class, OpenFlowDeviceConfig.CONFIG_KEY) {
                @Override
                public OpenFlowDeviceConfig createConfig() {
                    return new OpenFlowDeviceConfig();
                }
            };

    private final Controller ctrl = new Controller();

    private final NetworkConfigListener netCfgListener = new NetworkConfigListener() {
        @Override
        public boolean isRelevant(NetworkConfigEvent event) {
            return OpenFlowDeviceConfig.class.equals(event.configClass());
        }

        @Override
        public void event(NetworkConfigEvent event) {
            // We only receive NetworkConfigEvents
            OpenFlowDeviceConfig prevConfig = null;
            if (event.prevConfig().isPresent()) {
                prevConfig = (OpenFlowDeviceConfig) event.prevConfig().get();
            }

            OpenFlowDeviceConfig newConfig = null;
            if (event.config().isPresent()) {
                newConfig = (OpenFlowDeviceConfig) event.config().get();
            }

            boolean closeConnection = false;
            if (prevConfig != null && newConfig != null) {
                if (!Objects.equals(prevConfig.keyAlias(), newConfig.keyAlias())) {
                    closeConnection = true;
                }
            } else if (prevConfig != null) {
                // config was removed
                closeConnection = true;
            }
            if (closeConnection) {
                if (event.subject() instanceof DeviceId) {
                    DeviceId deviceId = (DeviceId) event.subject();
                    Dpid dpid = Dpid.dpid(deviceId.uri());
                    OpenFlowSwitch sw = getSwitch(dpid);
                    if (sw != null && ctrl.tlsParams.mode == Controller.TlsMode.STRICT) {
                        sw.disconnectSwitch();
                        log.info("Disconnecting switch {} because key has been updated or removed", dpid);
                    }
                }
            }
        }
    };

    @Activate
    public void activate(ComponentContext context) {
        coreService.registerApplication(APP_ID, this::cleanup);
        cfgService.registerProperties(getClass());
        netCfgService.registerConfigFactory(factory);
        netCfgService.addListener(netCfgListener);
        ctrl.setConfigParams(context.getProperties());
        ctrl.start(agent, driverService, netCfgService);
        openFlowManager.addListener(openFlowListener);
    }

    private void cleanup() {
        // Close listening channel and all OF channels. Clean information about switches
        // before deactivating
        ctrl.stop();
        connectedSwitches.values().forEach(OpenFlowSwitch::disconnectSwitch);
        connectedSwitches.clear();
        activeMasterSwitches.clear();
        activeEqualSwitches.clear();
        openFlowManager.removeListener(openFlowListener);
    }

    @Deactivate
    public void deactivate() {
        cleanup();
        cfgService.unregisterProperties(getClass(), false);
        netCfgService.removeListener(netCfgListener);
        netCfgService.unregisterConfigFactory(factory);
    }

    @Modified
    public void modified(ComponentContext context) {
        ctrl.setConfigParams(context.getProperties());
    }

    @Override
    public Iterable<OpenFlowSwitch> getSwitches() {
        return connectedSwitches.values();
    }

    @Override
    public Iterable<OpenFlowSwitch> getMasterSwitches() {
        return activeMasterSwitches.values();
    }

    @Override
    public Iterable<OpenFlowSwitch> getEqualSwitches() {
        return activeEqualSwitches.values();
    }

    @Override
    public OpenFlowSwitch getSwitch(Dpid dpid) {
        return connectedSwitches.get(dpid);
    }

    @Override
    public OpenFlowSwitch getMasterSwitch(Dpid dpid) {
        return activeMasterSwitches.get(dpid);
    }

    @Override
    public OpenFlowSwitch getEqualSwitch(Dpid dpid) {
        return activeEqualSwitches.get(dpid);
    }

    @Override
    public void addListener(OpenFlowSwitchListener listener) {
        if (!ofSwitchListener.contains(listener)) {
            this.ofSwitchListener.add(listener);
        }
    }

    @Override
    public void removeListener(OpenFlowSwitchListener listener) {
        this.ofSwitchListener.remove(listener);
    }

    @Override
    public void addClassifierListener(OpenFlowClassifierListener listener) {
        this.ofClassifierListener.add(listener);
    }

    @Override
    public void removeClassifierListener(OpenFlowClassifierListener listener) {
        this.ofClassifierListener.remove(listener);
    }

    @Override
    public void addMessageListener(OpenFlowMessageListener listener) {
        ofMessageListener.add(listener);
    }

    @Override
    public void removeMessageListener(OpenFlowMessageListener listener) {
        ofMessageListener.remove(listener);
    }

    @Override
    public void addPacketListener(int priority, PacketListener listener) {
        ofPacketListener.put(priority, listener);
    }

    @Override
    public void removePacketListener(PacketListener listener) {
        ofPacketListener.values().remove(listener);
    }

    @Override
    public void addEventListener(OpenFlowEventListener listener) {
        ofEventListener.add(listener);
    }

    @Override
    public void removeEventListener(OpenFlowEventListener listener) {
        ofEventListener.remove(listener);
    }

    @Override
    public void write(Dpid dpid, OFMessage msg) {
        this.getSwitch(dpid).sendMsg(msg);
    }

    @Override
    public CompletableFuture<OFMessage> writeResponse(Dpid dpid, OFMessage msg) {
        write(dpid, msg);

        ConcurrentMap<Long, CompletableFuture<OFMessage>> xids =
                responses.computeIfAbsent(dpid, k -> new ConcurrentHashMap<>());

        CompletableFuture<OFMessage> future = new CompletableFuture<>();
        xids.put(msg.getXid(), future);

        return future;
    }

    @Override
    public void processPacket(Dpid dpid, OFMessage msg) {
        OpenFlowSwitch sw = this.getSwitch(dpid);
        if (log.isTraceEnabled()) {
            log.trace("Processing message from switch {} via openflow: {}", dpid, msg);
        }

        // Check if someone is waiting for this message
        ConcurrentMap<Long, CompletableFuture<OFMessage>> xids = responses.get(dpid);
        if (xids != null) {
            CompletableFuture<OFMessage> future = xids.remove(msg.getXid());
            if (future != null) {
                future.complete(msg);
            }
        }

        switch (msg.getType()) {
        case PORT_STATUS:
            for (OpenFlowSwitchListener l : ofSwitchListener) {
                l.portChanged(dpid, (OFPortStatus) msg);
            }
            break;
        case FEATURES_REPLY:
            for (OpenFlowSwitchListener l : ofSwitchListener) {
                l.switchChanged(dpid);
            }
            break;
        case PACKET_IN:
            if (sw == null) {
                log.error("Ignoring PACKET_IN, switch {} is not found", dpid);
                break;
            }
            OpenFlowPacketContext pktCtx = DefaultOpenFlowPacketContext
                .packetContextFromPacketIn(sw, (OFPacketIn) msg);
            for (PacketListener p : ofPacketListener.values()) {
                p.handlePacket(pktCtx);
            }
            break;
        // TODO: Consider using separate threadpool for sensitive messages.
        //    ie. Back to back error could cause us to starve.
        case FLOW_REMOVED:
            executorMsgs.execute(new OFMessageHandler(dpid, msg));
            break;
        case ERROR:
            log.debug("Received error message from {}: {}", dpid, msg);
            errorMsgs.putIfAbsent(msg.getXid(), true);
            executorErrorMsgs.execute(new OFMessageHandler(dpid, msg));
            break;
        case STATS_REPLY:
            processStatsReply(dpid, (OFStatsReply) msg);
            break;
        case BARRIER_REPLY:
            if (errorMsgs.containsKey(msg.getXid())) {
                //To make oferror msg handling and corresponding barrier reply serialized,
                // executorErrorMsgs is used for both transaction
                errorMsgs.remove(msg.getXid());
                executorErrorMsgs.execute(new OFMessageHandler(dpid, msg));
            } else {
                executorBarrier.execute(new OFMessageHandler(dpid, msg));
            }
            break;
        case EXPERIMENTER:
            if (sw == null) {
                log.error("Switch {} is not found", dpid);
                break;
            }
            long experimenter = ((OFExperimenter) msg).getExperimenter();
            if (experimenter == 0x748771) {
                // LINC-OE port stats
                OFCircuitPortStatus circuitPortStatus = (OFCircuitPortStatus) msg;
                OFPortStatus.Builder portStatus = sw.factory().buildPortStatus();
                OFPortDesc.Builder portDesc = sw.factory().buildPortDesc();
                portDesc.setPortNo(circuitPortStatus.getPortNo())
                        .setHwAddr(circuitPortStatus.getHwAddr())
                        .setName(circuitPortStatus.getName())
                        .setConfig(circuitPortStatus.getConfig())
                        .setState(circuitPortStatus.getState());
                portStatus.setReason(circuitPortStatus.getReason()).setDesc(portDesc.build());
                for (OpenFlowSwitchListener l : ofSwitchListener) {
                    l.portChanged(dpid, portStatus.build());
                }
            } else {
                log.warn("Handling experimenter type {} not yet implemented",
                        ((OFExperimenter) msg).getExperimenter(), msg);
            }
            break;
        default:
            log.warn("Handling message type {} not yet implemented {}",
                    msg.getType(), msg);
        }
    }

    private void processStatsReply(Dpid dpid, OFStatsReply reply) {
        switch (reply.getStatsType()) {
            case QUEUE:
                Collection<OFQueueStatsEntry> queueStatsEntries = publishQueueStats(dpid, (OFQueueStatsReply) reply);
                if (queueStatsEntries != null) {
                    OFQueueStatsReply.Builder rep =
                            OFFactories.getFactory(reply.getVersion()).buildQueueStatsReply();
                    rep.setEntries(ImmutableList.copyOf(queueStatsEntries));
                    rep.setXid(reply.getXid());
                    executorMsgs.execute(new OFMessageHandler(dpid, rep.build()));
                }
                break;

            case PORT_DESC:
                for (OpenFlowSwitchListener l : ofSwitchListener) {
                    l.switchChanged(dpid);
                }
                break;

            case FLOW:
                Collection<OFFlowStatsEntry> flowStats = publishFlowStats(dpid, (OFFlowStatsReply) reply);
                if (flowStats != null) {
                    OFFlowStatsReply.Builder rep =
                            OFFactories.getFactory(reply.getVersion()).buildFlowStatsReply();
                    rep.setEntries(ImmutableList.copyOf(flowStats));
                    rep.setXid(reply.getXid());
                    executorMsgs.execute(new OFMessageHandler(dpid, rep.build()));
                }
                break;
            case FLOW_LIGHTWEIGHT:
                Collection<OFFlowLightweightStatsEntry> flowLightweightStats =
                        publishFlowStatsLightweight(dpid, (OFFlowLightweightStatsReply) reply);
                if (flowLightweightStats != null) {
                    OFFlowLightweightStatsReply.Builder rep =
                            OFFactories.getFactory(reply.getVersion()).buildFlowLightweightStatsReply();
                    rep.setEntries(ImmutableList.copyOf(flowLightweightStats));
                    rep.setXid(reply.getXid());
                    executorMsgs.execute(new OFMessageHandler(dpid, rep.build()));
                }
                break;
            case TABLE:
                Collection<OFTableStatsEntry> tableStats = publishTableStats(dpid, (OFTableStatsReply) reply);
                if (tableStats != null) {
                    OFTableStatsReply.Builder rep =
                            OFFactories.getFactory(reply.getVersion()).buildTableStatsReply();
                    rep.setEntries(ImmutableList.copyOf(tableStats));
                    executorMsgs.execute(new OFMessageHandler(dpid, rep.build()));
                }
                break;

            case GROUP:
                Collection<OFGroupStatsEntry> groupStats = publishGroupStats(dpid, (OFGroupStatsReply) reply);
                if (groupStats != null) {
                    OFGroupStatsReply.Builder rep =
                            OFFactories.getFactory(reply.getVersion()).buildGroupStatsReply();
                    rep.setEntries(ImmutableList.copyOf(groupStats));
                    rep.setXid(reply.getXid());
                    executorMsgs.execute(new OFMessageHandler(dpid, rep.build()));
                }
                break;

            case GROUP_DESC:
                Collection<OFGroupDescStatsEntry> groupDescStats = publishGroupDescStats(dpid,
                        (OFGroupDescStatsReply) reply);
                if (groupDescStats != null) {
                    OFGroupDescStatsReply.Builder rep =
                            OFFactories.getFactory(reply.getVersion()).buildGroupDescStatsReply();
                    rep.setEntries(ImmutableList.copyOf(groupDescStats));
                    rep.setXid(reply.getXid());
                    executorMsgs.execute(new OFMessageHandler(dpid, rep.build()));
                }
                break;

            case PORT:
                executorMsgs.execute(new OFMessageHandler(dpid, reply));
                break;

            case METER:
                executorMsgs.execute(new OFMessageHandler(dpid, reply));
                break;

            case EXPERIMENTER:
                if (reply instanceof OFCalientFlowStatsReply) {
                    OpenFlowSwitch sw = this.getSwitch(dpid);
                    // Convert Calient flow statistics to regular flow stats
                    // TODO: parse remaining fields such as power levels etc. when we have proper monitoring API
                    if (sw == null) {
                        log.error("Switch {} is not found", dpid);
                        break;
                    }
                    OFFlowStatsReply.Builder fsr = sw.factory().buildFlowStatsReply();
                    List<OFFlowStatsEntry> entries = new ArrayList<>();
                    for (OFCalientFlowStatsEntry entry : ((OFCalientFlowStatsReply) reply).getEntries()) {

                        // Single instruction, i.e., output to port
                        OFActionOutput action = sw.factory()
                                .actions()
                                .buildOutput()
                                .setPort(entry.getOutPort())
                                .build();
                        OFInstruction instruction = sw.factory()
                                .instructions()
                                .applyActions(Collections.singletonList(action));
                        OFFlowStatsEntry fs = sw.factory().buildFlowStatsEntry()
                                .setMatch(entry.getMatch())
                                .setTableId(entry.getTableId())
                                .setDurationSec(entry.getDurationSec())
                                .setDurationNsec(entry.getDurationNsec())
                                .setPriority(entry.getPriority())
                                .setIdleTimeout(entry.getIdleTimeout())
                                .setHardTimeout(entry.getHardTimeout())
                                .setFlags(entry.getFlags())
                                .setCookie(entry.getCookie())
                                .setInstructions(Collections.singletonList(instruction))
                                .build();
                        entries.add(fs);
                    }
                    fsr.setEntries(entries);

                    flowStats = publishFlowStats(dpid, fsr.build());
                    if (flowStats != null) {
                        OFFlowStatsReply.Builder rep =
                                sw.factory().buildFlowStatsReply();
                        rep.setEntries(ImmutableList.copyOf(flowStats));
                        executorMsgs.execute(new OFMessageHandler(dpid, rep.build()));
                    }
                } else {
                    executorMsgs.execute(new OFMessageHandler(dpid, reply));
                }
                break;
            default:
                log.warn("Discarding unknown stats reply type {}", reply.getStatsType());
                break;
        }
    }

    private synchronized Collection<OFFlowStatsEntry> publishFlowStats(Dpid dpid,
                                                                       OFFlowStatsReply reply) {
        //TODO: Get rid of synchronized
        fullFlowStats.putAll(dpid, reply.getEntries());
        if (!reply.getFlags().contains(OFStatsReplyFlags.REPLY_MORE)) {
            return fullFlowStats.removeAll(dpid);
        }
        return null;
    }

    private synchronized Collection<OFFlowLightweightStatsEntry> publishFlowStatsLightweight(
            Dpid dpid,
            OFFlowLightweightStatsReply reply) {
        //TODO: Get rid of synchronized
        fullFlowLightweightStats.putAll(dpid, reply.getEntries());
        if (!reply.getFlags().contains(OFStatsReplyFlags.REPLY_MORE)) {
            return fullFlowLightweightStats.removeAll(dpid);
        }
        return null;
    }

    private synchronized Collection<OFTableStatsEntry> publishTableStats(Dpid dpid,
                                                                       OFTableStatsReply reply) {
        //TODO: Get rid of synchronized
        fullTableStats.putAll(dpid, reply.getEntries());
        if (!reply.getFlags().contains(OFStatsReplyFlags.REPLY_MORE)) {
            return fullTableStats.removeAll(dpid);
        }
        return null;
    }

    private synchronized Collection<OFGroupStatsEntry> publishGroupStats(Dpid dpid,
                                                                      OFGroupStatsReply reply) {
        //TODO: Get rid of synchronized
        fullGroupStats.putAll(dpid, reply.getEntries());
        if (!reply.getFlags().contains(OFStatsReplyFlags.REPLY_MORE)) {
            return fullGroupStats.removeAll(dpid);
        }
        return null;
    }

    private synchronized Collection<OFGroupDescStatsEntry> publishGroupDescStats(Dpid dpid,
                                                                  OFGroupDescStatsReply reply) {
        //TODO: Get rid of synchronized
        fullGroupDescStats.putAll(dpid, reply.getEntries());
        if (!reply.getFlags().contains(OFStatsReplyFlags.REPLY_MORE)) {
            return fullGroupDescStats.removeAll(dpid);
        }
        return null;
    }

    private synchronized Collection<OFQueueStatsEntry> publishQueueStats(Dpid dpid, OFQueueStatsReply reply) {
        fullQueueStats.putAll(dpid, reply.getEntries());
        if (!reply.getFlags().contains(OFStatsReplyFlags.REPLY_MORE)) {
            return fullQueueStats.removeAll(dpid);
        }
        return null;
    }

    @Override
    public void setRole(Dpid dpid, RoleState role) {
        final OpenFlowSwitch sw = getSwitch(dpid);
        if (sw == null) {
            log.debug("Switch not connected. Ignoring setRole({}, {})", dpid, role);
            return;
        }
        sw.setRole(role);
    }

    /**
     * Implementation of an OpenFlow Agent which is responsible for
     * keeping track of connected switches and the state in which
     * they are.
     */
    public class OpenFlowSwitchAgent implements OpenFlowAgent {

        private final Logger log = LoggerFactory.getLogger(OpenFlowSwitchAgent.class);
        private final Lock switchLock = new ReentrantLock();

        @Override
        public boolean addConnectedSwitch(Dpid dpid, OpenFlowSwitch sw) {

            if (connectedSwitches.get(dpid) != null) {
                log.error("Trying to add connectedSwitch but found a previous "
                        + "value for dpid: {}", dpid);
                return false;
            } else {
                log.info("Added switch {}", dpid);
                connectedSwitches.put(dpid, sw);
                for (OpenFlowSwitchListener l : ofSwitchListener) {
                    l.switchAdded(dpid);
                }
                return true;
            }
        }

        @Override
        public boolean validActivation(Dpid dpid) {
            if (connectedSwitches.get(dpid) == null) {
                log.error("Trying to activate switch but is not in "
                        + "connected switches: dpid {}. Aborting ..",
                        dpid);
                return false;
            }
            if (activeMasterSwitches.get(dpid) != null ||
                    activeEqualSwitches.get(dpid) != null) {
                log.error("Trying to activate switch but it is already "
                        + "activated: dpid {}. Found in activeMaster: {} "
                        + "Found in activeEqual: {}. Aborting ..",
                          dpid,
                          (activeMasterSwitches.get(dpid) == null) ? 'N' : 'Y',
                          (activeEqualSwitches.get(dpid) == null) ? 'N' : 'Y');
                return false;
            }
            return true;
        }


        @Override
        public boolean addActivatedMasterSwitch(Dpid dpid, OpenFlowSwitch sw) {
            switchLock.lock();
            try {
                if (!validActivation(dpid)) {
                    return false;
                }
                activeMasterSwitches.put(dpid, sw);
                return true;
            } finally {
                switchLock.unlock();
            }
        }

        @Override
        public boolean addActivatedEqualSwitch(Dpid dpid, OpenFlowSwitch sw) {
            switchLock.lock();
            try {
                if (!validActivation(dpid)) {
                    return false;
                }
                activeEqualSwitches.put(dpid, sw);
                log.info("Added Activated EQUAL Switch {}", dpid);
                return true;
            } finally {
                switchLock.unlock();
            }
        }

        @Override
        public void transitionToMasterSwitch(Dpid dpid) {
            switchLock.lock();
            try {
                if (activeMasterSwitches.containsKey(dpid)) {
                    return;
                }
                OpenFlowSwitch sw = activeEqualSwitches.remove(dpid);
                if (sw == null) {
                    sw = getSwitch(dpid);
                    if (sw == null) {
                        log.error("Transition to master called on sw {}, but switch "
                                + "was not found in controller-cache", dpid);
                        return;
                    }
                }
                log.info("Transitioned switch {} to MASTER", dpid);
                activeMasterSwitches.put(dpid, sw);
                // purge pending stats
                log.info("Purged pending stats {}", dpid);
                purgeStatsSwitch(dpid);
            } finally {
                switchLock.unlock();
            }
        }

        private void purgeStatsSwitch(Dpid dpid) {
            fullFlowStats.removeAll(dpid);
            fullFlowLightweightStats.removeAll(dpid);
            fullTableStats.removeAll(dpid);
            fullGroupStats.removeAll(dpid);
            fullGroupDescStats.removeAll(dpid);
            fullQueueStats.removeAll(dpid);
        }

        @Override
        public void transitionToEqualSwitch(Dpid dpid) {
            switchLock.lock();
            try {
                if (activeEqualSwitches.containsKey(dpid)) {
                    return;
                }
                OpenFlowSwitch sw = activeMasterSwitches.remove(dpid);
                if (sw == null) {
                    sw = getSwitch(dpid);
                    if (sw == null) {
                        log.error("Transition to equal called on sw {}, but switch "
                                + "was not found in controller-cache", dpid);
                        return;
                    }
                }
                log.info("Transitioned switch {} to EQUAL", dpid);
                activeEqualSwitches.put(dpid, sw);
            } finally {
                switchLock.unlock();
            }

        }

        @Override
        public void removeConnectedSwitch(Dpid dpid) {
            connectedSwitches.remove(dpid);
            OpenFlowSwitch sw = activeMasterSwitches.remove(dpid);
            if (sw == null) {
                log.debug("sw was null for {}", dpid);
                sw = activeEqualSwitches.remove(dpid);
            }
            for (OpenFlowSwitchListener l : ofSwitchListener) {
                l.switchRemoved(dpid);
            }
        }

        @Override
        public void processDownstreamMessage(Dpid dpid, List<OFMessage> m) {
            for (OpenFlowMessageListener listener : ofMessageListener) {
                listener.handleOutgoingMessage(dpid, m);
            }
        }


        @Override
        public void processMessage(Dpid dpid, OFMessage m) {
            processPacket(dpid, m);

            for (OpenFlowMessageListener listener : ofMessageListener) {
                listener.handleIncomingMessage(dpid, m);
            }
        }

        @Override
        public void returnRoleReply(Dpid dpid, RoleState requested, RoleState response) {
            for (OpenFlowSwitchListener l : ofSwitchListener) {
                l.receivedRoleReply(dpid, requested, response);
            }
        }

        @Override
        public void addClassifierListener(OpenFlowClassifierListener listener) {
            ofClassifierListener.add(listener);
        }

        @Override
        public void removeClassifierListener(OpenFlowClassifierListener listener) {
            ofClassifierListener.remove(listener);
        }

        @Override
        public void roleChangedToMaster(Dpid dpid) {
            for (OpenFlowSwitchListener l : ofSwitchListener) {
                l.roleChangedToMaster(dpid);
            }
        }
    }

    /**
     * OpenFlow message handler.
     */
    protected final class OFMessageHandler implements Runnable {

        final OFMessage msg;
        final Dpid dpid;

        public OFMessageHandler(Dpid dpid, OFMessage msg) {
            this.msg = msg;
            this.dpid = dpid;
        }

        @Override
        public void run() {
            for (OpenFlowEventListener listener : ofEventListener) {
                listener.handleMessage(dpid, msg);
            }
        }
    }

    private class InternalOpenFlowListener implements OpenFlowListener {
        public void event(OpenFlowEvent event) {
            try {
                switch (event.type()) {
                case INSERT:
                    for (OpenFlowClassifierListener listener : ofClassifierListener) {
                        listener.handleClassifiersAdd(event.subject());
                    }
                    break;
                case REMOVE:
                    for (OpenFlowClassifierListener listener : ofClassifierListener) {
                        listener.handleClassifiersRemove(event.subject());
                    }
                    break;
                default:
                    log.warn("Unknown OpenFlow classifier event type: {}", event.type());
                    break;
                }
            } catch (Exception e) {
                log.error("Internal OpenFlowListener exception: {}", e.getMessage());
            }
        }
    }
}
