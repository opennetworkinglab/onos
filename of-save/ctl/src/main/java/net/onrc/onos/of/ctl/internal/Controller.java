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

package org.onlab.onos.of.controller.impl.internal;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

import net.onrc.onos.of.ctl.IOFSwitchManager;
import net.onrc.onos.of.ctl.Role;
import org.onlab.onos.of.controller.impl.annotations.LogMessageDoc;
import org.onlab.onos.of.controller.impl.annotations.LogMessageDocs;
import org.onlab.onos.of.controller.impl.debugcounter.DebugCounter;
import org.onlab.onos.of.controller.impl.debugcounter.IDebugCounter;
import org.onlab.onos.of.controller.impl.debugcounter.IDebugCounterService;
import org.onlab.onos.of.controller.impl.debugcounter.IDebugCounterService.CounterException;
import org.onlab.onos.of.controller.impl.debugcounter.IDebugCounterService.CounterType;
import org.onlab.onos.of.controller.impl.internal.OFChannelHandler.RoleRecvStatus;
import org.onlab.onos.of.controller.impl.registry.IControllerRegistry;
import org.onlab.onos.of.controller.impl.registry.RegistryException;
import org.onlab.onos.of.controller.impl.registry.IControllerRegistry.ControlChangeCallback;
import org.onlab.onos.of.controller.impl.util.Dpid;
import org.onlab.onos.of.controller.impl.util.DummySwitchForTesting;
import org.onlab.onos.of.controller.impl.util.InstanceId;
import net.onrc.onos.of.ctl.IOFSwitch;
import net.onrc.onos.of.ctl.IOFSwitch.PortChangeType;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.projectfloodlight.openflow.protocol.OFDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.util.HexString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The main controller class.  Handles all setup and network listeners
 * - Distributed ownership control of switch through IControllerRegistryService
 */
@Component(immediate = true)
public class Controller {

    protected static final Logger log = LoggerFactory.getLogger(Controller.class);
    static final String ERROR_DATABASE =
            "The controller could not communicate with the system database.";
    protected static final OFFactory FACTORY13 = OFFactories.getFactory(OFVersion.OF_13);
    protected static final OFFactory FACTORY10 = OFFactories.getFactory(OFVersion.OF_10);

    // connectedSwitches cache contains all connected switch's channelHandlers
    // including ones where this controller is a master/equal/slave controller
    // as well as ones that have not been activated yet
    protected ConcurrentHashMap<Long, OFChannelHandler> connectedSwitches;
    // These caches contains only those switches that are active
    protected ConcurrentHashMap<Long, IOFSwitch> activeMasterSwitches;
    protected ConcurrentHashMap<Long, IOFSwitch> activeEqualSwitches;
    // lock to synchronize on, when manipulating multiple caches above
    private Object multiCacheLock;

    // The controllerNodeIPsCache maps Controller IDs to their IP address.
    // It's only used by handleControllerNodeIPsChanged
    protected HashMap<String, String> controllerNodeIPsCache;

    // Module dependencies

    protected IControllerRegistry registryService;
    protected IDebugCounterService debugCounters;


    private IOFSwitchManager switchManager;

    // Configuration options
    protected int openFlowPort = 6633;
    protected int workerThreads = 0;

    // defined counters
    private Counters counters;

    // Start time of the controller
    protected long systemStartTime;

    // Flag to always flush flow table on switch reconnect (HA or otherwise)
    protected boolean alwaysClearFlowsOnSwAdd = false;
    private InstanceId instanceId;

    // Perf. related configuration
    protected static final int SEND_BUFFER_SIZE = 4 * 1024 * 1024;
    protected static final int BATCH_MAX_SIZE = 100;
    protected static final boolean ALWAYS_DECODE_ETH = true;

    protected boolean addConnectedSwitch(long dpid, OFChannelHandler h) {
        if (connectedSwitches.get(dpid) != null) {
            log.error("Trying to add connectedSwitch but found a previous "
                    + "value for dpid: {}", dpid);
            return false;
        } else {
            log.error("Added switch {}", dpid);
            connectedSwitches.put(dpid, h);
            return true;
        }
    }

    private boolean validActivation(long dpid) {
        if (connectedSwitches.get(dpid) == null) {
            log.error("Trying to activate switch but is not in "
                    + "connected switches: dpid {}. Aborting ..",
                    HexString.toHexString(dpid));
            return false;
        }
        if (activeMasterSwitches.get(dpid) != null ||
                activeEqualSwitches.get(dpid) != null) {
            log.error("Trying to activate switch but it is already "
                    + "activated: dpid {}. Found in activeMaster: {} "
                    + "Found in activeEqual: {}. Aborting ..", new Object[] {
                            HexString.toHexString(dpid),
                            (activeMasterSwitches.get(dpid) == null) ? 'N' : 'Y',
                            (activeEqualSwitches.get(dpid) == null) ? 'N' : 'Y'});
            counters.switchWithSameDpidActivated.updateCounterWithFlush();
            return false;
        }
        return true;
    }

    /**
     * Called when a switch is activated, with this controller's role as MASTER.
     */
    protected boolean addActivatedMasterSwitch(long dpid, IOFSwitch sw) {
        synchronized (multiCacheLock) {
            if (!validActivation(dpid)) {
                return false;
            }
            activeMasterSwitches.put(dpid, sw);
        }
        //update counters and events
        counters.switchActivated.updateCounterWithFlush();

        return true;
    }

    /**
     * Called when a switch is activated, with this controller's role as EQUAL.
     */
    protected boolean addActivatedEqualSwitch(long dpid, IOFSwitch sw) {
        synchronized (multiCacheLock) {
            if (!validActivation(dpid)) {
                return false;
            }
            activeEqualSwitches.put(dpid, sw);
        }
        //update counters and events
        counters.switchActivated.updateCounterWithFlush();
        return true;
    }

    /**
     * Called when this controller's role for a switch transitions from equal
     * to master. For 1.0 switches, we internally refer to the role 'slave' as
     * 'equal' - so this transition is equivalent to 'addActivatedMasterSwitch'.
     */
    protected void transitionToMasterSwitch(long dpid) {
        synchronized (multiCacheLock) {
            IOFSwitch sw = activeEqualSwitches.remove(dpid);
            if (sw == null) {
                log.error("Transition to master called on sw {}, but switch "
                        + "was not found in controller-cache", dpid);
                return;
            }
            activeMasterSwitches.put(dpid, sw);
        }
    }


    /**
     * Called when this controller's role for a switch transitions to equal.
     * For 1.0 switches, we internally refer to the role 'slave' as
     * 'equal'.
     */
    protected void transitionToEqualSwitch(long dpid) {
        synchronized (multiCacheLock) {
            IOFSwitch sw = activeMasterSwitches.remove(dpid);
            if (sw == null) {
                log.error("Transition to equal called on sw {}, but switch "
                        + "was not found in controller-cache", dpid);
                return;
            }
            activeEqualSwitches.put(dpid, sw);
        }

    }

    /**
     * Clear all state in controller switch maps for a switch that has
     * disconnected from the local controller. Also release control for
     * that switch from the global repository. Notify switch listeners.
     */
    protected void removeConnectedSwitch(long dpid) {
        releaseRegistryControl(dpid);
        connectedSwitches.remove(dpid);
        IOFSwitch sw = activeMasterSwitches.remove(dpid);
        if (sw == null) {
            sw = activeEqualSwitches.remove(dpid);
        }
        if (sw != null) {
            sw.cancelAllStatisticsReplies();
            sw.setConnected(false); // do we need this?
        }
        counters.switchDisconnected.updateCounterWithFlush();

    }

    /**
     * Indicates that ports on the given switch have changed. Enqueue a
     * switch update.
     * @param sw
     */
    protected void notifyPortChanged(long dpid, OFPortDesc port,
            PortChangeType changeType) {
        if (port == null || changeType == null) {
            String msg = String.format("Switch port or changetType must not "
                    + "be null in port change notification");
            throw new NullPointerException(msg);
        }
        if (connectedSwitches.get(dpid) == null || getSwitch(dpid) == null) {
            log.warn("Port change update on switch {} not connected or activated "
                    + "... Aborting.", HexString.toHexString(dpid));
            return;
        }

    }

    // ***************
    // Getters/Setters
    // ***************


    public synchronized void setIOFSwitchManager(IOFSwitchManager swManager) {
        this.switchManager = swManager;
        this.registryService = swManager.getRegistry();
    }


    public void setDebugCounter(IDebugCounterService dcs) {
        this.debugCounters = dcs;
    }

    IDebugCounterService getDebugCounter() {
        return this.debugCounters;
    }

    // **********************
    // Role Handling
    // **********************

    /**
     * created by ONOS - works with registry service.
     */
    protected class RoleChangeCallback implements ControlChangeCallback {
        @Override
        public void controlChanged(long dpidLong, boolean hasControl) {
            Dpid dpid = new Dpid(dpidLong);
            log.info("Role change callback for switch {}, hasControl {}",
                    dpid, hasControl);

            Role role = null;

            /*
             * issue #229
             * Cannot rely on sw.getRole() as it can be behind due to pending
             * role changes in the queue. Just submit it and late the
             * RoleChanger handle duplicates.
             */

            if (hasControl) {
                role = Role.MASTER;
            } else {
                role = Role.EQUAL; // treat the same as Role.SLAVE
            }

            OFChannelHandler swCh = connectedSwitches.get(dpid.value());
            if (swCh == null) {
                log.warn("Switch {} not found in connected switches", dpid);
                return;
            }

            log.debug("Sending role request {} msg to {}", role, dpid);
            swCh.sendRoleRequest(role, RoleRecvStatus.MATCHED_SET_ROLE);
        }
    }

    /**
     * Submit request to the registry service for mastership of the
     * switch.
     * @param dpid this datapath to get role for
     */
    public synchronized void submitRegistryRequest(long dpid) {
        if (registryService == null) {
            /*
             * If we have no registry then simply assign
             * mastership to this controller.
             */
            new RoleChangeCallback().controlChanged(dpid, true);
            return;
        }
        OFChannelHandler h = connectedSwitches.get(dpid);
        if (h == null) {
            log.error("Trying to request registry control for switch {} "
                    + "not in connected switches. Aborting.. ",
                    HexString.toHexString(dpid));
            connectedSwitches.get(dpid).disconnectSwitch();
            return;
        }
        //Request control of the switch from the global registry
        try {
            h.controlRequested = Boolean.TRUE;
            registryService.requestControl(dpid, new RoleChangeCallback());
        } catch (RegistryException e) {
            log.debug("Registry error: {}", e.getMessage());
            h.controlRequested = Boolean.FALSE;
        }
        if (!h.controlRequested) { // XXX what is being attempted here?
            // yield to allow other thread(s) to release control
            // TODO AAS: this is awful and needs to be fixed
            Thread.yield();
            // safer to bounce the switch to reconnect here than proceeding further
            // XXX S why? can't we just try again a little later?
            log.debug("Closing sw:{} because we weren't able to request control " +
                    "successfully" + dpid);
            connectedSwitches.get(dpid).disconnectSwitch();
        }
    }

    /**
     * Relinquish role for the switch.
     * @param dpidLong the controlled datapath
     */
    public synchronized void releaseRegistryControl(long dpidLong) {
        OFChannelHandler h = connectedSwitches.get(dpidLong);
        if (h == null) {
            log.error("Trying to release registry control for switch {} "
                    + "not in connected switches. Aborting.. ",
                    HexString.toHexString(dpidLong));
            return;
        }
        if (registryService != null && h.controlRequested) {
            //TODO the above is not good for testing need to change controlrequest to method call.
            registryService.releaseControl(dpidLong);
        }
    }


    // FIXME: remove this method
    public Map<Long, IOFSwitch> getSwitches() {
        return getMasterSwitches();
    }

    // FIXME: remove this method
    public Map<Long, IOFSwitch> getMasterSwitches() {
        return Collections.unmodifiableMap(activeMasterSwitches);
    }



    public Set<Long> getAllSwitchDpids() {
        Set<Long> dpids = new HashSet<Long>();
        dpids.addAll(activeMasterSwitches.keySet());
        dpids.addAll(activeEqualSwitches.keySet());
        return dpids;
    }


    public Set<Long> getAllMasterSwitchDpids() {
        Set<Long> dpids = new HashSet<Long>();
        dpids.addAll(activeMasterSwitches.keySet());
        return dpids;
    }


    public Set<Long> getAllEqualSwitchDpids() {
        Set<Long> dpids = new HashSet<Long>();
        dpids.addAll(activeEqualSwitches.keySet());
        return dpids;
    }


    public IOFSwitch getSwitch(long dpid) {
        IOFSwitch sw = null;
        sw = activeMasterSwitches.get(dpid);
        if (sw != null) {
            return sw;
        }
        sw = activeEqualSwitches.get(dpid);
        if (sw != null) {
            return sw;
        }
        return sw;
    }


    public IOFSwitch getMasterSwitch(long dpid) {
        return  activeMasterSwitches.get(dpid);
    }


    public IOFSwitch getEqualSwitch(long dpid) {
        return  activeEqualSwitches.get(dpid);
    }





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


    public InstanceId getInstanceId() {
        return instanceId;
    }


    // **************
    // Initialization
    // **************

    /**
     * Tell controller that we're ready to accept switches loop.
     *
     * @throws IOException
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
            final ChannelGroup cg = new DefaultChannelGroup();
            cg.add(bootstrap.bind(sa));

            log.info("Listening for switch connections on {}", sa);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private ServerBootstrap createServerBootStrap() {
        if (workerThreads == 0) {
            return new ServerBootstrap(
                    new NioServerSocketChannelFactory(
                            Executors.newCachedThreadPool(),
                            Executors.newCachedThreadPool()));
        } else {
            return new ServerBootstrap(
                    new NioServerSocketChannelFactory(
                            Executors.newCachedThreadPool(),
                            Executors.newCachedThreadPool(), workerThreads));
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
        String controllerId = configParams.get("controllerid");
        if (controllerId != null) {
            this.instanceId = new InstanceId(controllerId);
        } else {
            //Try to get the hostname of the machine and use that for controller ID
            try {
                String hostname = java.net.InetAddress.getLocalHost().getHostName();
                this.instanceId = new InstanceId(hostname);
            } catch (UnknownHostException e) {
                log.warn("Can't get hostname, using the default");
            }
        }

        log.debug("ControllerId set to {}", this.instanceId);
    }


    /**
     * Initialize internal data structures.
     */
    public void init(Map<String, String> configParams) {
        // These data structures are initialized here because other
        // module's startUp() might be called before ours
        this.activeMasterSwitches = new ConcurrentHashMap<Long, IOFSwitch>();
        this.activeEqualSwitches = new ConcurrentHashMap<Long, IOFSwitch>();
        this.connectedSwitches = new ConcurrentHashMap<Long, OFChannelHandler>();
        this.controllerNodeIPsCache = new HashMap<String, String>();

        setConfigParams(configParams);
        this.systemStartTime = System.currentTimeMillis();
        this.setDebugCounter(new DebugCounter());
        this.counters = new Counters();
        this.multiCacheLock = new Object();

    }

    /**
     * Startup all of the controller's components.
     */
    @LogMessageDoc(message = "Waiting for storage source",
            explanation = "The system database is not yet ready",
            recommendation = "If this message persists, this indicates " +
                    "that the system database has failed to start. " +
                    LogMessageDoc.CHECK_CONTROLLER)
    public synchronized void startupComponents() {
        try {
            if (registryService != null) {
                registryService.registerController(instanceId.toString());
            }
        } catch (RegistryException e) {
            log.warn("Registry service error: {}", e.getMessage());
        }

        // register counters and events
        try {
            this.counters.createCounters(debugCounters);
        } catch (CounterException e) {
            log.warn("Counters unavailable: {}", e.getMessage());
        }
    }

    // **************
    // debugCounter registrations
    // **************

    public static class Counters {
        public static final String PREFIX = "controller";
        public IDebugCounter switchActivated;
        public IDebugCounter switchWithSameDpidActivated; // warn
        public IDebugCounter switchDisconnected;
        public IDebugCounter messageReceived;
        public IDebugCounter switchDisconnectReadTimeout;
        public IDebugCounter switchDisconnectHandshakeTimeout;
        public IDebugCounter switchDisconnectIOError;
        public IDebugCounter switchDisconnectParseError;
        public IDebugCounter switchDisconnectSwitchStateException;
        public IDebugCounter rejectedExecutionException;
        public IDebugCounter switchDisconnectOtherException;
        public IDebugCounter switchConnected;
        public IDebugCounter unhandledMessage;
        public IDebugCounter packetInWhileSwitchIsSlave;
        public IDebugCounter epermErrorWhileSwitchIsMaster;
        public IDebugCounter roleReplyTimeout;
        public IDebugCounter roleReplyReceived; // expected RoleReply received
        public IDebugCounter roleReplyErrorUnsupported;
        public IDebugCounter switchCounterRegistrationFailed;

        void createCounters(IDebugCounterService debugCounters) throws CounterException {

            switchActivated =
                debugCounters.registerCounter(
                            PREFIX, "switch-activated",
                            "A switch connected to this controller is now " +
                            "in MASTER role",
                            CounterType.ALWAYS_COUNT);

            switchWithSameDpidActivated = // warn
                debugCounters.registerCounter(
                            PREFIX, "switch-with-same-dpid-activated",
                            "A switch with the same DPID as another switch " +
                            "connected to the controller. This can be " +
                            "caused by multiple switches configured with " +
                            "the same DPID or by a switch reconnecting very " +
                            "quickly.",
                            CounterType.COUNT_ON_DEMAND,
                            IDebugCounterService.CTR_MDATA_WARN);

            switchDisconnected =
                debugCounters.registerCounter(
                            PREFIX, "switch-disconnected",
                            "FIXME: switch has disconnected",
                            CounterType.ALWAYS_COUNT);

        //------------------------
        // channel handler counters. Factor them out ??
            messageReceived =
                debugCounters.registerCounter(
                            PREFIX, "message-received",
                            "Number of OpenFlow messages received. Some of " +
                            "these might be throttled",
                            CounterType.ALWAYS_COUNT);

            switchDisconnectReadTimeout =
                debugCounters.registerCounter(
                            PREFIX, "switch-disconnect-read-timeout",
                            "Number of times a switch was disconnected due " +
                            "due the switch failing to send OpenFlow " +
                            "messages or responding to OpenFlow ECHOs",
                            CounterType.ALWAYS_COUNT,
                            IDebugCounterService.CTR_MDATA_ERROR);
            switchDisconnectHandshakeTimeout =
                debugCounters.registerCounter(
                            PREFIX, "switch-disconnect-handshake-timeout",
                            "Number of times a switch was disconnected " +
                            "because it failed to complete the handshake " +
                            "in time.",
                            CounterType.ALWAYS_COUNT,
                            IDebugCounterService.CTR_MDATA_ERROR);
            switchDisconnectIOError =
                debugCounters.registerCounter(
                            PREFIX, "switch-disconnect-io-error",
                            "Number of times a switch was disconnected " +
                            "due to IO errors on the switch connection.",
                            CounterType.ALWAYS_COUNT,
                            IDebugCounterService.CTR_MDATA_ERROR);
            switchDisconnectParseError =
                debugCounters.registerCounter(
                            PREFIX, "switch-disconnect-parse-error",
                           "Number of times a switch was disconnected " +
                           "because it sent an invalid packet that could " +
                           "not be parsed",
                           CounterType.ALWAYS_COUNT,
                           IDebugCounterService.CTR_MDATA_ERROR);

            switchDisconnectSwitchStateException =
                debugCounters.registerCounter(
                            PREFIX, "switch-disconnect-switch-state-exception",
                            "Number of times a switch was disconnected " +
                            "because it sent messages that were invalid " +
                            "given the switch connection's state.",
                            CounterType.ALWAYS_COUNT,
                            IDebugCounterService.CTR_MDATA_ERROR);
            rejectedExecutionException =
                debugCounters.registerCounter(
                            PREFIX, "rejected-execution-exception",
                            "TODO",
                            CounterType.ALWAYS_COUNT,
                            IDebugCounterService.CTR_MDATA_ERROR);

            switchDisconnectOtherException =
                debugCounters.registerCounter(
                            PREFIX,  "switch-disconnect-other-exception",
                            "Number of times a switch was disconnected " +
                            "due to an exceptional situation not covered " +
                            "by other counters",
                            CounterType.ALWAYS_COUNT,
                            IDebugCounterService.CTR_MDATA_ERROR);

            switchConnected =
                debugCounters.registerCounter(
                            PREFIX, "switch-connected",
                            "Number of times a new switch connection was " +
                            "established",
                            CounterType.ALWAYS_COUNT);

            unhandledMessage =
                debugCounters.registerCounter(
                            PREFIX, "unhandled-message",
                            "Number of times an OpenFlow message was " +
                            "received that the controller ignored because " +
                            "it was inapproriate given the switch " +
                            "connection's state.",
                            CounterType.ALWAYS_COUNT,
                            IDebugCounterService.CTR_MDATA_WARN);
                            // might be less than warning

            packetInWhileSwitchIsSlave =
                debugCounters.registerCounter(
                            PREFIX, "packet-in-while-switch-is-slave",
                            "Number of times a packet in was received " +
                            "from a switch that was in SLAVE role. " +
                            "Possibly inidicates inconsistent roles.",
                            CounterType.ALWAYS_COUNT);
            epermErrorWhileSwitchIsMaster =
                debugCounters.registerCounter(
                            PREFIX, "eperm-error-while-switch-is-master",
                            "Number of times a permission error was " +
                            "received while the switch was in MASTER role. " +
                            "Possibly inidicates inconsistent roles.",
                            CounterType.ALWAYS_COUNT,
                            IDebugCounterService.CTR_MDATA_WARN);

            roleReplyTimeout =
                debugCounters.registerCounter(
                            PREFIX, "role-reply-timeout",
                            "Number of times a role request message did not " +
                            "receive the expected reply from a switch",
                            CounterType.ALWAYS_COUNT,
                            IDebugCounterService.CTR_MDATA_WARN);

            roleReplyReceived = // expected RoleReply received
                debugCounters.registerCounter(
                            PREFIX, "role-reply-received",
                            "Number of times the controller received the " +
                            "expected role reply message from a switch",
                            CounterType.ALWAYS_COUNT);

            roleReplyErrorUnsupported =
                debugCounters.registerCounter(
                            PREFIX, "role-reply-error-unsupported",
                            "Number of times the controller received an " +
                            "error from a switch in response to a role " +
                            "request indicating that the switch does not " +
                            "support roles.",
                            CounterType.ALWAYS_COUNT);

            switchCounterRegistrationFailed =
                    debugCounters.registerCounter(PREFIX,
                                "switch-counter-registration-failed",
                                "Number of times the controller failed to " +
                                "register per-switch debug counters",
                                CounterType.ALWAYS_COUNT,
                                IDebugCounterService.CTR_MDATA_WARN);


        }
    }

    public Counters getCounters() {
        return this.counters;
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
     * @return
     */
    protected IOFSwitch getOFSwitchInstance(OFDescStatsReply desc, OFVersion ofv) {
        if (switchManager == null) {
            return new DummySwitchForTesting();
        }
        return switchManager.getSwitchImpl(desc.getMfrDesc(), desc.getHwDesc(),
                                            desc.getSwDesc(), ofv);
    }

    @Activate
    public void activate() {
        log.info("Initialising OpenFlow Lib and IO");
        this.init(new HashMap<String, String>());
        this.startupComponents();
        this.run();
    }

}
