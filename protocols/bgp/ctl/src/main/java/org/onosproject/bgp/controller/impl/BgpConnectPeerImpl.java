/*
 * Copyright 2015-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.onosproject.bgp.controller.impl;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.onosproject.bgp.controller.BgpCfg;
import org.onosproject.bgp.controller.BgpController;
import org.onosproject.bgp.controller.BgpPeerCfg;
import org.onosproject.bgp.controller.BgpConnectPeer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements connection initiation to peer on peer configuration and manage channel using netty channel handler.
 */
public class BgpConnectPeerImpl implements BgpConnectPeer {
    private static final Logger log = LoggerFactory.getLogger(BgpConnectPeerImpl.class);

    private ScheduledExecutorService connectExecutor = null;
    private final String peerHost;
    private static final int RETRY_INTERVAL = 4;
    private final int peerPort;
    private int connectRetryCounter = 0;
    private int connectRetryTime;
    private ChannelPipelineFactory pfact;
    private ClientBootstrap peerBootstrap;
    public String getPeerHost() {
        return peerHost;
    }
    public static int getRetryInterval() {
        return RETRY_INTERVAL;
    }

    @Override
    public int getPeerPort() {
        return peerPort;
    }
    @Override
    public int getConnectRetryCounter() {
        return connectRetryCounter;
    }

    public void setConnectRetryCounter(int connectRetryCounter) {
        this.connectRetryCounter = connectRetryCounter;
    }

    public void setConnectRetryTime(int connectRetryTime) {
        this.connectRetryTime = connectRetryTime;
    }

    public BgpCfg getBgpconfig() {
        return bgpconfig;
    }

    public void setBgpconfig(BgpCfg bgpconfig) {
        this.bgpconfig = bgpconfig;
    }

    private BgpCfg bgpconfig;

    /**
     * Initialize timer and initiate pipeline factory.
     *
     * @param bgpController parent BGP controller
     * @param remoteHost remote host to connect
     * @param remotePort remote port to connect
     */
    public BgpConnectPeerImpl(BgpController bgpController, String remoteHost, int remotePort) {

        this.bgpconfig = bgpController.getConfig();
        this.pfact = new BgpPipelineFactory(bgpController, false);
        this.peerBootstrap = Controller.peerBootstrap();
        this.peerBootstrap.setPipelineFactory(pfact);
        this.peerHost = remoteHost;
        this.peerPort = remotePort;
        this.connectRetryTime = 0;
    }

    @Override
    public void disconnectPeer() {
        if (connectExecutor != null) {
            connectExecutor.shutdown();
            connectExecutor = null;
        }
    }

    @Override
    public void connectPeer() {
        scheduleConnectionRetry(this.connectRetryTime);
    }

    /**
     * Retry connection with exponential back-off mechanism.
     *
     * @param retryDelay retry delay
     */
    private void scheduleConnectionRetry(long retryDelay) {
        if (this.connectExecutor == null) {
            this.connectExecutor = Executors.newSingleThreadScheduledExecutor();
        }
        this.connectExecutor.schedule(new ConnectionRetry(), retryDelay, TimeUnit.MINUTES);
    }

    /**
     * Implements BGP connection and manages connection to peer with back-off mechanism in case of failure.
     */
    class ConnectionRetry implements Runnable {
        @Override
        public void run() {
            log.debug("Connect to peer {}", peerHost);

            InetSocketAddress connectToSocket = new InetSocketAddress(peerHost, peerPort);

            try {
                bgpconfig.setPeerConnState(peerHost, BgpPeerCfg.State.CONNECT);
                peerBootstrap.connect(connectToSocket).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (!future.isSuccess()) {
                            bgpconfig.setPeerConnState(peerHost, BgpPeerCfg.State.ACTIVE);
                            connectRetryCounter++;
                            log.error("Connection failed, ConnectRetryCounter {} remote host {}", connectRetryCounter,
                                      peerHost);
                            /*
                             * Reconnect to peer on failure is exponential till 4 mins, later on retry after every 4
                             * mins.
                             */
                            if (connectRetryTime < RETRY_INTERVAL) {
                                connectRetryTime = (connectRetryTime != 0) ? connectRetryTime * 2 : 1;
                            }
                            scheduleConnectionRetry(connectRetryTime);
                        } else {

                            connectRetryCounter++;
                            log.debug("Connected to remote host {}, Connect Counter {}", peerHost, connectRetryCounter);
                            disconnectPeer();
                            return;
                        }
                    }
                });
            } catch (Exception e) {
                log.debug("Connect peer exception : " + e.toString());
                disconnectPeer();
            }
        }
    }
}
