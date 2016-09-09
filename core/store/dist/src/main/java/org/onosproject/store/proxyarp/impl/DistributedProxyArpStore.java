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
package org.onosproject.store.proxyarp.impl;

import com.google.common.collect.Maps;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.KryoNamespace;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.NodeId;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.net.proxyarp.ProxyArpStore;
import org.onosproject.net.proxyarp.ProxyArpStoreDelegate;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.cluster.messaging.MessageSubject;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.serializers.StoreSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import static org.onlab.util.BoundedThreadPool.newFixedThreadPool;
import static org.onlab.util.Tools.groupedThreads;

/**
 * Implementation of proxy ARP distribution mechanism.
 *
 * @deprecated in Hummingbird release. This is no longer necessary as there are
 * other solutions for the problem this was solving.
 */
@Deprecated
@Component(immediate = true)
@Service
public class DistributedProxyArpStore implements ProxyArpStore {

    private Logger log = LoggerFactory.getLogger(getClass());

    private static final MessageSubject ARP_RESPONSE_MESSAGE =
            new MessageSubject("onos-arp-response");

    protected final StoreSerializer serializer = StoreSerializer.using(
            KryoNamespace.newBuilder()
                    .register(KryoNamespaces.API)
                    .nextId(KryoNamespaces.BEGIN_USER_CUSTOM_ID)
                    .register(ArpResponseMessage.class)
                    .register(ByteBuffer.class)
                    .build("ProxyArpStore"));

    private ProxyArpStoreDelegate delegate;

    private Map<HostId, ArpResponseMessage> pendingMessages = Maps.newConcurrentMap();

    private ExecutorService executor =
            newFixedThreadPool(4, groupedThreads("onos/arp", "sender-%d", log));

    private NodeId localNodeId;

    private HostListener hostListener = new InternalHostListener();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterCommunicationService commService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;


    @Activate
    protected void activate() {
        localNodeId = clusterService.getLocalNode().id();
        hostService.addListener(hostListener);
        commService.addSubscriber(ARP_RESPONSE_MESSAGE, serializer::decode,
                                  this::processArpResponse, executor);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        commService.removeSubscriber(ARP_RESPONSE_MESSAGE);
        hostService.removeListener(hostListener);
        log.info("Stopped");
    }

    @Override
    public void forward(ConnectPoint outPort, Host subject, ByteBuffer packet) {
        /*NodeId nodeId = mastershipService.getMasterFor(outPort.deviceId());
        if (nodeId.equals(localNodeId)) {
            if (delegate != null) {
                delegate.emitResponse(outPort, packet);
            }
        } else {
            log.info("Forwarding ARP response from {} to {}", subject.id(), outPort);
            commService.unicast(new ArpResponseMessage(outPort, subject, packet.array()),
                                ARP_RESPONSE_MESSAGE, serializer::encode, nodeId);
        }*/
        //FIXME: Code above may be unnecessary and therefore cluster messaging
        // and pendingMessages could be pruned as well.
        delegate.emitResponse(outPort, packet);
    }

    @Override
    public void setDelegate(ProxyArpStoreDelegate delegate) {
        this.delegate = delegate;
    }

    // Processes the incoming ARP response message.
    private void processArpResponse(ArpResponseMessage msg) {
        pendingMessages.put(msg.subject.id(), msg);
        if (hostService.getHost(msg.subject.id()) != null) {
            checkPendingArps(msg.subject.id());
        }
        // FIXME: figure out pruning so stuff does not build up
    }

    // Checks for pending ARP response message for the specified host.
    // If one exists, emit response via delegate.
    private void checkPendingArps(HostId id) {
        ArpResponseMessage msg = pendingMessages.remove(id);
        if (msg != null && delegate != null) {
            log.info("Emitting ARP response from {} to {}", id, msg.outPort);
            delegate.emitResponse(msg.outPort, ByteBuffer.wrap(msg.packet));
        }
    }

    // Message carrying an ARP response.
    private static class ArpResponseMessage {
        private ConnectPoint outPort;
        private Host subject;
        private byte[] packet;

        public ArpResponseMessage(ConnectPoint outPort, Host subject, byte[] packet) {
            this.outPort = outPort;
            this.subject = subject;
            this.packet = packet;
        }

        private ArpResponseMessage() {
        }
    }

    private class InternalHostListener implements HostListener {
        @Override
        public void event(HostEvent event) {
            checkPendingArps(event.subject().id());
        }
    }
}
