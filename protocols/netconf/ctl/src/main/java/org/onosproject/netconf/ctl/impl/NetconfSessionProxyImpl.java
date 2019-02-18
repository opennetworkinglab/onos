/*
 * Copyright 2019-present Open Networking Foundation
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

package org.onosproject.netconf.ctl.impl;

import org.onosproject.cluster.NodeId;
import org.onosproject.netconf.AbstractNetconfSession;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfDeviceInfo;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfProxyMessage;
import org.onosproject.netconf.NetconfSession;
import org.onosproject.netconf.NetconfSessionFactory;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.slf4j.LoggerFactory.getLogger;

public class NetconfSessionProxyImpl extends AbstractNetconfSession {
    protected final NetconfDeviceInfo deviceInfo;
    protected final NetconfController netconfController;
    protected final NodeId sessionNodeId;
    private static final Logger log = getLogger(NetconfSessionMinaImpl.class);

    private static final int CONFIGURE_REPLY_TIMEOUT_SEC = 5;

    public NetconfSessionProxyImpl(NetconfDeviceInfo deviceInfo,
                                   NetconfController controller,
                                   NodeId nodeId) {
        this.deviceInfo = deviceInfo;
        this.netconfController = controller;
        this.sessionNodeId = nodeId;
    }

    private <T> CompletableFuture<T> executeAtMasterCompletableFuture(
            NetconfProxyMessage proxyMessage)
            throws NetconfException {
        CompletableFuture<T> reply = netconfController.executeAtMaster(proxyMessage);
        return reply;
    }

    private <T> T executeAtMaster(NetconfProxyMessage proxyMessage) throws NetconfException {
        return executeAtMaster(proxyMessage, CONFIGURE_REPLY_TIMEOUT_SEC);
    }

    private <T> T executeAtMaster(NetconfProxyMessage proxyMessage, int timeout) throws NetconfException {
        CompletableFuture<T> reply = executeAtMasterCompletableFuture(proxyMessage);
        try {
            return reply.get(timeout, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NetconfException(e.getMessage(), e.getCause());
        } catch (ExecutionException e) {
            throw new NetconfException(e.getMessage(), e.getCause());
        } catch (TimeoutException e) {
            throw new NetconfException(e.getMessage(), e.getCause());
        }

    }

    protected NetconfProxyMessage makeProxyMessage(NetconfProxyMessage.SubjectType subjectType, String request) {
        return new DefaultNetconfProxyMessage(subjectType,
                                              deviceInfo.getDeviceId(),
                                              new ArrayList<>(Arrays.asList(request)),
                                              sessionNodeId);
    }

    @Override
    public CompletableFuture<String> rpc(String request)
            throws NetconfException {
        NetconfProxyMessage proxyMessage = makeProxyMessage(NetconfProxyMessage.SubjectType.RPC,
                                                            request);

        return executeAtMasterCompletableFuture(proxyMessage);
    }

    @Override
    public CompletableFuture<String> request(String request)
            throws NetconfException {
        NetconfProxyMessage proxyMessage = makeProxyMessage(NetconfProxyMessage.SubjectType.REQUEST,
                                                            request);

        return executeAtMasterCompletableFuture(proxyMessage);
    }

    @Override
    public String requestSync(String request, int timeout)
            throws NetconfException {
        NetconfProxyMessage proxyMessage = makeProxyMessage(NetconfProxyMessage.SubjectType.REQUEST_SYNC,
                                                            request);

        return executeAtMaster(proxyMessage, timeout);
    }

    @Override
    public String requestSync(String request)
            throws NetconfException {

        return requestSync(request, CONFIGURE_REPLY_TIMEOUT_SEC);
    }

    @Override
    public void startSubscription(String filterSchema) throws NetconfException {
        NetconfProxyMessage proxyMessage = makeProxyMessage(NetconfProxyMessage.SubjectType.START_SUBSCRIPTION,
                                                            filterSchema);

        executeAtMaster(proxyMessage);
    }

    @Override
    public void endSubscription() throws NetconfException {
        NetconfProxyMessage proxyMessage = makeProxyMessage(NetconfProxyMessage.SubjectType.END_SUBSCRIPTION,
                                                            "");

        executeAtMaster(proxyMessage);
    }

    @Override
    public String getSessionId() {
        NetconfProxyMessage proxyMessage = makeProxyMessage(NetconfProxyMessage.SubjectType.GET_SESSION_ID,
                                                            "");
        try {
            return executeAtMaster(proxyMessage);
        } catch (NetconfException e) {
            log.error("Cannot get session id : {}", e);
            return String.valueOf(-1);
        }

    }

    @Override
    public Set<String> getDeviceCapabilitiesSet() {
        NetconfProxyMessage proxyMessage = makeProxyMessage(NetconfProxyMessage.SubjectType.GET_DEVICE_CAPABILITIES_SET,
                                                            "");
        try {
            return executeAtMaster(proxyMessage);
        } catch (NetconfException e) {
            log.error("Could not get device capabilities : {}", e);
            return null;
        }
    }


    @Override
    public void setOnosCapabilities(Iterable<String> capabilities) {
        ArrayList<String> capabilitiesList = new ArrayList<>();
        capabilities.spliterator().forEachRemaining(c -> capabilitiesList.add(c));

        NetconfProxyMessage proxyMessage =
                new DefaultNetconfProxyMessage(
                        NetconfProxyMessage.SubjectType.SET_ONOS_CAPABILITIES,
                        deviceInfo.getDeviceId(), capabilitiesList,
                        sessionNodeId);
        try {
            executeAtMaster(proxyMessage);
        } catch (NetconfException e) {
            log.error("Could not set onos capabilities : {}", e);
        }
    }


    public static class ProxyNetconfSessionFactory implements NetconfSessionFactory {

        @Override
        public NetconfSession createNetconfSession(NetconfDeviceInfo netconfDeviceInfo,
                                                   NetconfController netconfController) {
            return new NetconfSessionProxyImpl(netconfDeviceInfo,
                                               netconfController,
                                               netconfController.getLocalNodeId());
        }
    }
}
