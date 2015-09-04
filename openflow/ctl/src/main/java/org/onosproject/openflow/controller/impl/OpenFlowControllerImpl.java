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

import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.net.driver.DefaultDriverProviderService;
import org.onosproject.net.driver.DriverService;
import org.onosproject.openflow.controller.DefaultOpenFlowPacketContext;
import org.onosproject.openflow.controller.Dpid;
import org.onosproject.openflow.controller.OpenFlowController;
import org.onosproject.openflow.controller.OpenFlowEventListener;
import org.onosproject.openflow.controller.OpenFlowPacketContext;
import org.onosproject.openflow.controller.OpenFlowSwitch;
import org.onosproject.openflow.controller.OpenFlowSwitchListener;
import org.onosproject.openflow.controller.PacketListener;
import org.onosproject.openflow.controller.RoleState;
import org.onosproject.openflow.controller.driver.OpenFlowAgent;
import org.osgi.service.component.ComponentContext;
import org.projectfloodlight.openflow.protocol.OFCalientFlowStatsEntry;
import org.projectfloodlight.openflow.protocol.OFCalientFlowStatsReply;
import org.projectfloodlight.openflow.protocol.OFCircuitPortStatus;
import org.projectfloodlight.openflow.protocol.OFExperimenter;
import org.projectfloodlight.openflow.protocol.OFFactories;
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
import org.projectfloodlight.openflow.protocol.OFPortStatsReply;
import org.projectfloodlight.openflow.protocol.OFPortStatus;
import org.projectfloodlight.openflow.protocol.OFStatsReply;
import org.projectfloodlight.openflow.protocol.OFStatsReplyFlags;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.instruction.OFInstruction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.onlab.util.Tools.get;
import static org.onlab.util.Tools.groupedThreads;

@Component(immediate = true)
@Service
public class OpenFlowControllerImpl implements OpenFlowController {
    private static final int DEFAULT_OFPORT = 6633;
    private static final int DEFAULT_WORKER_THREADS = 16;

    private static final Logger log =
            LoggerFactory.getLogger(OpenFlowControllerImpl.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DriverService driverService;

    // References exists merely for sequencing purpose to assure drivers are loaded
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DefaultDriverProviderService defaultDriverProviderService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService cfgService;

    @Property(name = "openflowPort", intValue = DEFAULT_OFPORT,
            label = "Port number used by OpenFlow protocol; default is 6633")
    private int openflowPort = DEFAULT_OFPORT;

    @Property(name = "workerThreads", intValue = DEFAULT_WORKER_THREADS,
            label = "Number of controller worker threads; default is 16")
    private int workerThreads = DEFAULT_WORKER_THREADS;

    private final ExecutorService executorMsgs =
        Executors.newFixedThreadPool(32, groupedThreads("onos/of", "event-stats-%d"));

    private final ExecutorService executorBarrier =
        Executors.newFixedThreadPool(4, groupedThreads("onos/of", "event-barrier-%d"));

    protected ConcurrentHashMap<Dpid, OpenFlowSwitch> connectedSwitches =
            new ConcurrentHashMap<>();
    protected ConcurrentHashMap<Dpid, OpenFlowSwitch> activeMasterSwitches =
            new ConcurrentHashMap<>();
    protected ConcurrentHashMap<Dpid, OpenFlowSwitch> activeEqualSwitches =
            new ConcurrentHashMap<>();

    protected OpenFlowSwitchAgent agent = new OpenFlowSwitchAgent();
    protected Set<OpenFlowSwitchListener> ofSwitchListener = new CopyOnWriteArraySet<>();

    protected Multimap<Integer, PacketListener> ofPacketListener =
            ArrayListMultimap.create();

    protected Set<OpenFlowEventListener> ofEventListener = new CopyOnWriteArraySet<>();

    protected Multimap<Dpid, OFFlowStatsEntry> fullFlowStats =
            ArrayListMultimap.create();

    protected Multimap<Dpid, OFGroupStatsEntry> fullGroupStats =
            ArrayListMultimap.create();

    protected Multimap<Dpid, OFGroupDescStatsEntry> fullGroupDescStats =
            ArrayListMultimap.create();

    protected Multimap<Dpid, OFPortStatsEntry> fullPortStats =
            ArrayListMultimap.create();

    private final Controller ctrl = new Controller();

    @Activate
    public void activate(ComponentContext context) {
        cfgService.registerProperties(getClass());
        Map<String, String> properties = readComponentConfiguration(context);
        ctrl.setConfigParams(properties);
        ctrl.start(agent, driverService);
    }

    @Deactivate
    public void deactivate() {
        cfgService.unregisterProperties(getClass(), false);
        ctrl.stop();
    }

    /**
     * Extracts properties from the component configuration context.
     *
     * @param context the component context
     */
    private Map<String, String> readComponentConfiguration(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();
        Map<String, String> outProperties = new HashMap<>();

        String port = get(properties, "openflowPort");
        if (!Strings.isNullOrEmpty(port)) {
            outProperties.put("openflowport", port);
        }

        String thread = get(properties, "workerThreads");
        if (!Strings.isNullOrEmpty(thread)) {
            outProperties.put("workerthreads", thread);
        }

        return outProperties;
    }

    @Modified
    public void modified(ComponentContext context) {
        Map<String, String> properties = readComponentConfiguration(context);
        ctrl.stop();
        ctrl.setConfigParams(properties);
        ctrl.start(agent, driverService);
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
    public void processPacket(Dpid dpid, OFMessage msg) {
        Collection<OFFlowStatsEntry> flowStats;
        Collection<OFGroupStatsEntry> groupStats;
        Collection<OFGroupDescStatsEntry> groupDescStats;
        Collection<OFPortStatsEntry> portStats;

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
            OpenFlowPacketContext pktCtx = DefaultOpenFlowPacketContext
            .packetContextFromPacketIn(this.getSwitch(dpid),
                    (OFPacketIn) msg);
            for (PacketListener p : ofPacketListener.values()) {
                p.handlePacket(pktCtx);
            }
            break;
        // TODO: Consider using separate threadpool for sensitive messages.
        //    ie. Back to back error could cause us to starve.
        case FLOW_REMOVED:
        case ERROR:
            executorMsgs.submit(new OFMessageHandler(dpid, msg));
            break;
        case STATS_REPLY:
            OFStatsReply reply = (OFStatsReply) msg;
            switch (reply.getStatsType()) {
                case PORT_DESC:
                    for (OpenFlowSwitchListener l : ofSwitchListener) {
                        l.switchChanged(dpid);
                    }
                    break;
                case FLOW:
                    flowStats = publishFlowStats(dpid, (OFFlowStatsReply) reply);
                    if (flowStats != null) {
                        OFFlowStatsReply.Builder rep =
                                OFFactories.getFactory(msg.getVersion()).buildFlowStatsReply();
                        rep.setEntries(Lists.newLinkedList(flowStats));
                        executorMsgs.submit(new OFMessageHandler(dpid, rep.build()));
                    }
                    break;
                case GROUP:
                    groupStats = publishGroupStats(dpid, (OFGroupStatsReply) reply);
                    if (groupStats != null) {
                        OFGroupStatsReply.Builder rep =
                                OFFactories.getFactory(msg.getVersion()).buildGroupStatsReply();
                        rep.setEntries(Lists.newLinkedList(groupStats));
                        rep.setXid(reply.getXid());
                        executorMsgs.submit(new OFMessageHandler(dpid, rep.build()));
                    }
                    break;
                case GROUP_DESC:
                    groupDescStats = publishGroupDescStats(dpid,
                            (OFGroupDescStatsReply) reply);
                    if (groupDescStats != null) {
                        OFGroupDescStatsReply.Builder rep =
                                OFFactories.getFactory(msg.getVersion()).buildGroupDescStatsReply();
                        rep.setEntries(Lists.newLinkedList(groupDescStats));
                        rep.setXid(reply.getXid());
                        executorMsgs.submit(new OFMessageHandler(dpid, rep.build()));
                    }
                    break;
                case PORT:
                    executorMsgs.submit(new OFMessageHandler(dpid, reply));
                    break;
                case METER:
                    executorMsgs.submit(new OFMessageHandler(dpid, reply));
                    break;
                case EXPERIMENTER:
                    if (reply instanceof OFCalientFlowStatsReply) {
                        // Convert Calient flow statistics to regular flow stats
                        // TODO: parse remaining fields such as power levels etc. when we have proper monitoring API
                        OFFlowStatsReply.Builder fsr = getSwitch(dpid).factory().buildFlowStatsReply();
                        List<OFFlowStatsEntry> entries = new LinkedList<>();
                        for (OFCalientFlowStatsEntry entry : ((OFCalientFlowStatsReply) msg).getEntries()) {

                            // Single instruction, i.e., output to port
                            OFActionOutput action = OFFactories
                                    .getFactory(msg.getVersion())
                                    .actions()
                                    .buildOutput()
                                    .setPort(entry.getOutPort())
                                    .build();
                            OFInstruction instruction = OFFactories
                                    .getFactory(msg.getVersion())
                                    .instructions()
                                    .applyActions(Collections.singletonList(action));
                            OFFlowStatsEntry fs = getSwitch(dpid).factory().buildFlowStatsEntry()
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
                                    OFFactories.getFactory(msg.getVersion()).buildFlowStatsReply();
                            rep.setEntries(Lists.newLinkedList(flowStats));
                            executorMsgs.submit(new OFMessageHandler(dpid, rep.build()));
                        }
                    } else {
                        executorMsgs.submit(new OFMessageHandler(dpid, reply));
                    }
                    break;
                default:
                    log.warn("Discarding unknown stats reply type {}", reply.getStatsType());
                    break;
            }
            break;
        case BARRIER_REPLY:
            executorBarrier.submit(new OFMessageHandler(dpid, msg));
            break;
        case EXPERIMENTER:
            long experimenter = ((OFExperimenter) msg).getExperimenter();
            if (experimenter == 0x748771) {
                // LINC-OE port stats
                OFCircuitPortStatus circuitPortStatus = (OFCircuitPortStatus) msg;
                OFPortStatus.Builder portStatus = this.getSwitch(dpid).factory().buildPortStatus();
                OFPortDesc.Builder portDesc = this.getSwitch(dpid).factory().buildPortDesc();
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

    private synchronized Collection<OFFlowStatsEntry> publishFlowStats(Dpid dpid,
                                                                       OFFlowStatsReply reply) {
        //TODO: Get rid of synchronized
        fullFlowStats.putAll(dpid, reply.getEntries());
        if (!reply.getFlags().contains(OFStatsReplyFlags.REPLY_MORE)) {
            return fullFlowStats.removeAll(dpid);
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

    private synchronized Collection<OFPortStatsEntry> publishPortStats(Dpid dpid,
                                                                 OFPortStatsReply reply) {
        fullPortStats.putAll(dpid, reply.getEntries());
        if (!reply.getFlags().contains(OFStatsReplyFlags.REPLY_MORE)) {
            return fullPortStats.removeAll(dpid);
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
                        + "Found in activeEqual: {}. Aborting ..", new Object[]{
                                dpid,
                                (activeMasterSwitches.get(dpid) == null) ? 'N' : 'Y',
                                        (activeEqualSwitches.get(dpid) == null) ? 'N' : 'Y'});
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
            } finally {
                switchLock.unlock();
            }
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
        public void processMessage(Dpid dpid, OFMessage m) {
            processPacket(dpid, m);
        }

        @Override
        public void returnRoleReply(Dpid dpid, RoleState requested, RoleState response) {
            for (OpenFlowSwitchListener l : ofSwitchListener) {
                l.receivedRoleReply(dpid, requested, response);
            }
        }
    }

    private final class OFMessageHandler implements Runnable {

        private final OFMessage msg;
        private final Dpid dpid;

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

}
