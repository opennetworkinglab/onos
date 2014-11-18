/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onlab.onos.sdnip;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.core.ApplicationId;
import org.onlab.onos.core.CoreService;
import org.onlab.onos.net.host.HostService;
import org.onlab.onos.net.intent.IntentService;
import org.onlab.onos.sdnip.bgp.BgpRouteEntry;
import org.onlab.onos.sdnip.bgp.BgpSession;
import org.onlab.onos.sdnip.bgp.BgpSessionManager;
import org.onlab.onos.sdnip.config.SdnIpConfigReader;
import org.onlab.onos.store.service.Lock;
import org.onlab.onos.store.service.LockService;
import org.slf4j.Logger;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * Component for the SDN-IP peering application.
 */
@Component(immediate = true)
@Service
public class SdnIp implements SdnIpService {

    private static final String SDN_IP_APP = "org.onlab.onos.sdnip";
    // NOTE: Must be 5s for now
    private static final int LEASE_DURATION_MS = 5 * 1000;
    private static final int LEASE_EXTEND_RETRY_MAX = 3;

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentService intentService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LockService lockService;

    private IntentSynchronizer intentSynchronizer;
    private SdnIpConfigReader config;
    private PeerConnectivityManager peerConnectivity;
    private Router router;
    private BgpSessionManager bgpSessionManager;

    private ExecutorService leaderElectionExecutor;
    private Lock leaderLock;
    private volatile boolean isShutdown = true;

    @Activate
    protected void activate() {
        log.info("SDN-IP started");
        isShutdown = false;

        ApplicationId appId = coreService.registerApplication(SDN_IP_APP);
        config = new SdnIpConfigReader();
        config.init();

        InterfaceService interfaceService =
            new HostToInterfaceAdaptor(hostService);

        intentSynchronizer = new IntentSynchronizer(appId, intentService);
        intentSynchronizer.start();

        peerConnectivity = new PeerConnectivityManager(appId, config,
                interfaceService, intentService);
        peerConnectivity.start();

        router = new Router(appId, intentSynchronizer, hostService, config,
                            interfaceService);
        router.start();

        leaderLock = lockService.create(SDN_IP_APP + "/sdnIpLeaderLock");
        leaderElectionExecutor = Executors.newSingleThreadExecutor(
                new ThreadFactoryBuilder()
                .setNameFormat("sdnip-leader-election-%d").build());
        leaderElectionExecutor.execute(new Runnable() {
            @Override
            public void run() {
                doLeaderElectionThread();
            }
        });

        // Manually set the instance as the leader to allow testing
        // TODO change this when we get a leader election
        // intentSynchronizer.leaderChanged(true);

        bgpSessionManager = new BgpSessionManager(router);
        // TODO: the local BGP listen port number should be configurable
        bgpSessionManager.start(2000);

        // TODO need to disable link discovery on external ports
    }

    @Deactivate
    protected void deactivate() {
        isShutdown = true;

        bgpSessionManager.stop();
        router.stop();
        peerConnectivity.stop();
        intentSynchronizer.stop();

        // Stop the thread(s)
        leaderElectionExecutor.shutdownNow();

        log.info("Stopped");
    }

    @Override
    public Collection<BgpSession> getBgpSessions() {
        return bgpSessionManager.getBgpSessions();
    }

    @Override
    public Collection<BgpRouteEntry> getBgpRoutes() {
        return bgpSessionManager.getBgpRoutes();
    }

    @Override
    public Collection<RouteEntry> getRoutes() {
        return router.getRoutes();
    }

    @Override
    public void modifyPrimary(boolean isPrimary) {
        intentSynchronizer.leaderChanged(isPrimary);
    }

    static String dpidToUri(String dpid) {
        return "of:" + dpid.replace(":", "");
    }

    /**
     * Performs the leader election.
     */
    private void doLeaderElectionThread() {

        //
        // Try to acquire the lock and keep extending it until the instance
        // is shutdown.
        //
        while (!isShutdown) {
            log.debug("SDN-IP Leader Election begin");

            // Block until it becomes the leader
            try {
                leaderLock.lock(LEASE_DURATION_MS);

                // This instance is the leader
                log.info("SDN-IP Leader Elected");
                intentSynchronizer.leaderChanged(true);

                // Keep extending the expiration until shutdown
                int extensionFailedCountdown = LEASE_EXTEND_RETRY_MAX - 1;

                //
                // Keep periodically extending the lock expiration.
                // If there are multiple back-to-back failures to extend (with
                // extra sleep time between retrials), then release the lock.
                //
                while (!isShutdown) {
                    Thread.sleep(LEASE_DURATION_MS / LEASE_EXTEND_RETRY_MAX);
                    if (leaderLock.extendExpiration(LEASE_DURATION_MS)) {
                        log.trace("SDN-IP Leader Extended");
                        extensionFailedCountdown = LEASE_EXTEND_RETRY_MAX;
                    } else {
                        log.debug("SDN-IP Leader Cannot Extend Election");
                        if (!leaderLock.isLocked()) {
                            log.debug("SDN-IP Leader Lock Lost");
                            intentSynchronizer.leaderChanged(false);
                            break;              // Try again to get the lock
                        }
                        extensionFailedCountdown--;
                        if (extensionFailedCountdown <= 0) {
                            // Failed too many times to extend.
                            // Release the lock.
                            log.debug("SDN-IP Leader Lock Released");
                            intentSynchronizer.leaderChanged(false);
                            leaderLock.unlock();
                            break;              // Try again to get the lock
                        }
                    }
                }
            } catch (InterruptedException e) {
                // Thread interrupted. Time to shutdown
                log.debug("SDN-IP Leader Interrupted");
            }
        }
        // If we reach here, the instance was shutdown
        intentSynchronizer.leaderChanged(false);
        leaderLock.unlock();
    }
}
