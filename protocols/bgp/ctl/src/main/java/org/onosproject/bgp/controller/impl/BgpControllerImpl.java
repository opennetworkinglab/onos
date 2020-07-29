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

package org.onosproject.bgp.controller.impl;

import org.onosproject.bgp.controller.BgpCfg;
import org.onosproject.bgp.controller.BgpController;
import org.onosproject.bgp.controller.BgpId;
import org.onosproject.bgp.controller.BgpLinkListener;
import org.onosproject.bgp.controller.BgpLocalRib;
import org.onosproject.bgp.controller.BgpNodeListener;
import org.onosproject.bgp.controller.BgpPeer;
import org.onosproject.bgp.controller.BgpPeerManager;
import org.onosproject.bgp.controller.BgpPrefixListener;
import org.onosproject.bgp.controller.BgpRouteListener;
import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.protocol.BgpMessage;
import org.onosproject.bgpio.protocol.BgpUpdateMsg;
import org.onosproject.bgpio.types.BgpValueType;
import org.onosproject.bgpio.types.MpReachNlri;
import org.onosproject.bgpio.types.MpUnReachNlri;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.onlab.util.Tools.groupedThreads;

@Component(immediate = true, service = BgpController.class)
public class BgpControllerImpl implements BgpController {

    private static final Logger log = LoggerFactory.getLogger(BgpControllerImpl.class);
    final Controller ctrl = new Controller(this);
    protected ConcurrentHashMap<BgpId, BgpPeer> connectedPeers = new ConcurrentHashMap<BgpId, BgpPeer>();

    protected BgpPeerManagerImpl peerManager = new BgpPeerManagerImpl();

    private BgpLocalRib bgplocalRib = new BgpLocalRibImpl(this);
    private BgpLocalRib bgplocalRibVpn = new BgpLocalRibImpl(this);

    protected Set<BgpNodeListener> bgpNodeListener = new CopyOnWriteArraySet<>();
    protected Set<BgpLinkListener> bgpLinkListener = new CopyOnWriteArraySet<>();
    protected Set<BgpPrefixListener> bgpPrefixListener = new CopyOnWriteArraySet<>();
    protected BgpController bgpController;
    private BgpConfig bgpconfig = new BgpConfig(this);
    private List<String> activeExceptionList = new LinkedList();
    private LinkedList<String> closedExceptionList = new LinkedList<String>();
    private Map<String, List<String>> activeSessionExceptionMap = new TreeMap<>();
    private Map<String, List<String>> closedSessionExceptionMap = new TreeMap<>();
    protected Set<BgpRouteListener> bgpRouteListener = new CopyOnWriteArraySet<>();

    //IDs for timers
    private static final int PERIODIC_TIMER = 1001;
    private static final int WARMUP_TIMER = 1002;
    private static final int COOLDOWN_TIMER = 1003;

    private static final int POOL_SIZE = 3; //Current pool size is 3
    private ScheduledExecutorService executor;
    private ScheduledFuture<?> cooldownFuture;
    private ScheduledFuture<?> periodicFuture;
    private ScheduledFuture<?> warmupFuture;

    private AtomicBoolean hasTopologyChanged = new AtomicBoolean(false);

    @Override
    public void activeSessionExceptionAdd(String peerId, String exception) {
        if (peerId != null) {
            activeExceptionList.add(exception);
            activeSessionExceptionMap.put(peerId, activeExceptionList);
        } else {
            log.debug("Peer Id is null");
        }
        if (activeExceptionList.size() > 10) {
            activeExceptionList.clear();
            activeExceptionList.add(exception);
            activeSessionExceptionMap.put(peerId, activeExceptionList);
        }
    }


    @Override
    public void closedSessionExceptionAdd(String peerId, String exception) {
        if (peerId != null) {
            closedExceptionList.add(exception);
            closedSessionExceptionMap.put(peerId, closedExceptionList);
         } else {
            log.debug("Peer Id is null");
        }
        if (closedExceptionList.size() > 10) {
            closedExceptionList.clear();
            closedExceptionList.add(exception);
            closedSessionExceptionMap.put(peerId, closedExceptionList);
        }
    }

    @Override
    public Map<String, List<String>> activeSessionMap() {
        return activeSessionExceptionMap;
    }

    @Override
    public Map<String, List<String>> closedSessionMap() {
        return closedSessionExceptionMap;
    }

    @Override
    public void addRouteListener(BgpRouteListener listener) {
        this.bgpRouteListener.add(listener);
    }

    @Override
    public void removeRouteListener(BgpRouteListener listener) {
        this.bgpRouteListener.remove(listener);
    }

    @Override
    public Set<BgpRouteListener> routeListener() {
        return bgpRouteListener;
    }

    @Activate
    public void activate() {
        this.ctrl.start();
        executor = Executors.newScheduledThreadPool(
                        POOL_SIZE,
                        groupedThreads("onos/apps/bgpcontroller", "bgp-rr-timer"));
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        activeSessionExceptionMap.clear();
        closedSessionExceptionMap.clear();
        // Close all connected peers
        closeConnectedPeers();
        this.ctrl.stop();
        executor.shutdown();
        log.info("Stopped");
    }

    @Override
    public Iterable<BgpPeer> getPeers() {
        return this.connectedPeers.values();
    }

    @Override
    public BgpPeer getPeer(BgpId bgpId) {
        return this.connectedPeers.get(bgpId);
    }

    @Override
    public void addListener(BgpNodeListener listener) {
        this.bgpNodeListener.add(listener);
    }

    @Override
    public void removeListener(BgpNodeListener listener) {
        this.bgpNodeListener.remove(listener);
    }

    @Override
    public Set<BgpNodeListener> listener() {
        return bgpNodeListener;
    }

    @Override
    public void writeMsg(BgpId bgpId, BgpMessage msg) {
        this.getPeer(bgpId).sendMessage(msg);
    }

    @Override
    public void processBgpPacket(BgpId bgpId, BgpMessage msg) throws BgpParseException {

        BgpPeer peer = getPeer(bgpId);

        switch (msg.getType()) {
            case OPEN:
                // TODO: Process Open message
                break;
            case KEEP_ALIVE:
                // TODO: Process keepalive message
                break;
            case NOTIFICATION:
                // TODO: Process notificatoin message
                break;
            case UPDATE:
                BgpUpdateMsg updateMsg = (BgpUpdateMsg) msg;
                List<BgpValueType> pathAttr = updateMsg.bgpPathAttributes().pathAttributes();
                if (pathAttr == null) {
                    log.debug("llPathAttr is null, cannot process update message");
                    break;
                }
                Iterator<BgpValueType> listIterator = pathAttr.iterator();
                boolean isLinkstate = false;
                boolean isEvpn = false;

                while (listIterator.hasNext()) {
                    BgpValueType attr = listIterator.next();
                    if (attr instanceof MpReachNlri) {
                        MpReachNlri mpReach = (MpReachNlri) attr;
                        if (mpReach.bgpFlowSpecNlri() == null
                                && mpReach.bgpEvpnNlri() == null) {
                            isLinkstate = true;
                        }
                        if (mpReach.bgpEvpnNlri() != null) {
                            isEvpn = true;
                        }
                    } else if (attr instanceof MpUnReachNlri) {
                        MpUnReachNlri mpUnReach = (MpUnReachNlri) attr;
                        if (mpUnReach.bgpFlowSpecNlri() == null
                                && mpUnReach.bgpEvpnNlri() == null) {
                            isLinkstate = true;
                        }
                        if (mpUnReach.bgpEvpnNlri() != null) {
                            isEvpn = true;
                        }
                    }
                }
                if (isLinkstate) {
                    peer.buildAdjRibIn(pathAttr);
                }
                if (isEvpn) {
                    for (BgpRouteListener listener : bgpRouteListener) {
                        listener.processRoute(bgpId, updateMsg);
                    }
                }
                break;
            default:
                // TODO: Process other message
                break;
        }
    }

    @Override
    public void closeConnectedPeers() {
        BgpPeer bgpPeer;
        for (BgpId id : this.connectedPeers.keySet()) {
            bgpPeer = getPeer(id);
            bgpPeer.disconnectPeer();
        }
    }

    /**
     * Implementation of an BGP Peer which is responsible for keeping track of connected peers and the state in which
     * they are.
     */
    public class BgpPeerManagerImpl implements BgpPeerManager {

        private final Logger log = LoggerFactory.getLogger(BgpPeerManagerImpl.class);
        private final Lock peerLock = new ReentrantLock();

        @Override
        public boolean addConnectedPeer(BgpId bgpId, BgpPeer bgpPeer) {

            if (connectedPeers.get(bgpId) != null) {
                this.log.error("Trying to add connectedPeer but found previous " + "value for bgp ip: {}",
                               bgpId.toString());
                return false;
            } else {
                this.log.debug("Added Peer {}", bgpId.toString());
                connectedPeers.put(bgpId, bgpPeer);

                //If all timers are stopped, start periodic timer
                this.log.info("Start periodic timer");
                if (bgpconfig.isRouteRefreshEnabled()
                        && (periodicFuture == null || periodicFuture.isCancelled())
                        && (cooldownFuture == null || cooldownFuture.isCancelled())
                        && (warmupFuture == null || warmupFuture.isCancelled())) {
                        periodicFuture = executor.schedule(periodicTimerTask,
                            bgpconfig.getRouteRefreshPeriodicTimer(), TimeUnit.SECONDS);
                }

                return true;
            }
        }

        @Override
        public boolean isPeerConnected(BgpId bgpId) {
            if (connectedPeers.get(bgpId) == null) {
                this.log.error("Is peer connected: bgpIp {}.", bgpId.toString());
                return false;
            }

            return true;
        }

        @Override
        public void removeConnectedPeer(BgpId bgpId) {
            connectedPeers.remove(bgpId);
        }

        @Override
        public BgpPeer getPeer(BgpId bgpId) {
            return connectedPeers.get(bgpId);
        }

        /**
         * Gets bgp peer instance.
         *
         * @param bgpController controller instance.
         * @param sessionInfo bgp session info.
         * @param pktStats packet statistics.
         * @return BGPPeer peer instance.
         */
        public BgpPeer getBgpPeerInstance(BgpController bgpController, BgpSessionInfoImpl sessionInfo,
                                          BgpPacketStatsImpl pktStats) {
            BgpPeer bgpPeer = new BgpPeerImpl(bgpController, sessionInfo, pktStats);
            return bgpPeer;
        }

    }

    /**
     * Returns controller.
     *
     * @return controller
     */
    public Controller controller() {
        return this.ctrl;
    }

    @Override
    public ConcurrentHashMap<BgpId, BgpPeer> connectedPeers() {
        return connectedPeers;
    }

    @Override
    public BgpPeerManagerImpl peerManager() {
        return peerManager;
    }

    @Override
    public BgpCfg getConfig() {
        return this.bgpconfig;
    }

    @Override
    public int connectedPeerCount() {
        return connectedPeers.size();
    }

    /**
     * Gets the BGP local RIB.
     *
     * @return bgplocalRIB BGP local RIB.
     */
    @Override
    public BgpLocalRib bgpLocalRib() {
        return bgplocalRib;
    }

    /**
     * Gets the BGP local RIB with VPN.
     *
     * @return bgplocalRIBVpn BGP VPN local RIB .
     */
    @Override
    public BgpLocalRib bgpLocalRibVpn() {
        return bgplocalRibVpn;
    }

    @Override
    public void addLinkListener(BgpLinkListener listener) {
        this.bgpLinkListener.add(listener);
    }

    @Override
    public void removeLinkListener(BgpLinkListener listener) {
        this.bgpLinkListener.remove(listener);
    }

    @Override
    public Set<BgpLinkListener> linkListener() {
        return bgpLinkListener;
    }

    @Override
    public void addPrefixListener(BgpPrefixListener listener) {
        bgpPrefixListener.add(listener);
    }

    @Override
    public void removePrefixListener(BgpPrefixListener listener) {
        bgpPrefixListener.remove(listener);
    }

    @Override
    public Set<BgpPrefixListener> prefixListener() {
        return bgpPrefixListener;
    }

    @Override
    public void notifyTopologyChange() {
        log.info("Topology change received");

        hasTopologyChanged.set(true);

        //If cooldown timer is running, do nothing further because routeRefresh will be sent when it expires
        if (cooldownFuture != null && !cooldownFuture.isCancelled()) {
            log.debug("Do nothing : Cooldown timer running");
            return;
        }

        //If warmup timer is running, refresh it. If not, start it
        if (warmupFuture != null && !warmupFuture.isCancelled()) {
            warmupFuture.cancel(true);
            warmupFuture = null;

            warmupFuture = executor.schedule(warmupTimerTask,
                    bgpconfig.getRouteRefreshWarmupTimer(), TimeUnit.SECONDS);

            log.debug("Warmup timer running. Re-started warmup timer");
            return;
        } else {
            warmupFuture = executor.schedule(warmupTimerTask,
                    bgpconfig.getRouteRefreshWarmupTimer(), TimeUnit.SECONDS);
            log.debug("Warmup timer started");
            return;
        }
    }

    protected void resetTimers() {
        if (periodicFuture != null && !periodicFuture.isCancelled()) {
            periodicFuture.cancel(true);
            periodicFuture = null;
        }

        if (warmupFuture != null && !warmupFuture.isCancelled()) {
            warmupFuture.cancel(true);
            warmupFuture = null;
        }

        if (cooldownFuture != null && !cooldownFuture.isCancelled()) {
            cooldownFuture.cancel(true);
            cooldownFuture = null;
        }
    }

    protected synchronized void timerCallback(int timerId) {
        switch (timerId) {
            case PERIODIC_TIMER:
                //Cancel periodic timer and run cooldown timer
                periodicFuture.cancel(true);
                periodicFuture = null;

                sendRouteRefreshToPeers();

                //Cancel warmup timer if it is running
                if (warmupFuture != null && !warmupFuture.isCancelled()) {
                    warmupFuture.cancel(true);
                    warmupFuture = null;
                }

                cooldownFuture = executor.schedule(cooldownTimerTask,
                        bgpconfig.getRouteRefreshCooldownTimer(), TimeUnit.SECONDS);
                log.debug("Cooldown timer started");
                break;
            case WARMUP_TIMER:
                //Send route refresh and start cooldown timer
                warmupFuture.cancel(true);
                warmupFuture = null;

                sendRouteRefreshToPeers();

                cooldownFuture = executor.schedule(cooldownTimerTask,
                        bgpconfig.getRouteRefreshCooldownTimer(), TimeUnit.SECONDS);
                //Cancel periodic timer, if it is running
                if (periodicFuture != null && !periodicFuture.isCancelled()) {
                    periodicFuture.cancel(true);
                    periodicFuture = null;
                }
                log.debug("Cooldown timer started");
                break;
            case COOLDOWN_TIMER:
                //If hasTopologyChanged is true, we need to restart cooldown timer.
                //Otherwise, start periodic timer
                boolean hasTopologyChangedValue = hasTopologyChanged.get();

                cooldownFuture.cancel(true);
                cooldownFuture = null;

                if (hasTopologyChangedValue) {
                    sendRouteRefreshToPeers();
                    cooldownFuture = executor.schedule(cooldownTimerTask,
                            bgpconfig.getRouteRefreshCooldownTimer(), TimeUnit.SECONDS);
                    log.debug("Cooldown timer started");
                } else {
                    periodicFuture = executor.schedule(periodicTimerTask,
                            bgpconfig.getRouteRefreshPeriodicTimer(), TimeUnit.SECONDS);
                    log.debug("Periodic timer started");
                }
                break;
            default:
                log.error("Invalid timerId in callback");
        }

    }

    private synchronized void sendRouteRefreshToPeers() {
        //Iterate over peers and send route refresh
        connectedPeers.forEach((k, v) -> v.sendRouteRefreshMessage());

        //Refresh hasTopologyChanged variable
        hasTopologyChanged.set(false);
    }

    private Runnable periodicTimerTask = new Runnable() {
        @Override
        public void run() {
            log.debug("Periodic Timer Expired");
            timerCallback(PERIODIC_TIMER);
        }
    };

    private Runnable cooldownTimerTask = new Runnable() {
        @Override
        public void run() {
            log.info("Cooldown Timer Expired");
            timerCallback(COOLDOWN_TIMER);
        }
    };

    private Runnable warmupTimerTask = new Runnable() {
        @Override
        public void run() {
            log.debug("Warmup Timer Expired");
            timerCallback(WARMUP_TIMER);
        }
    };
}
