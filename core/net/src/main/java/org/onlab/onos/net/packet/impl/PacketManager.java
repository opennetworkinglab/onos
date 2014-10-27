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
package org.onlab.onos.net.packet.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.net.Device;
import org.onlab.onos.net.device.DeviceService;
import org.onlab.onos.net.packet.OutboundPacket;
import org.onlab.onos.net.packet.PacketContext;
import org.onlab.onos.net.packet.PacketEvent;
import org.onlab.onos.net.packet.PacketProcessor;
import org.onlab.onos.net.packet.PacketProvider;
import org.onlab.onos.net.packet.PacketProviderRegistry;
import org.onlab.onos.net.packet.PacketProviderService;
import org.onlab.onos.net.packet.PacketService;
import org.onlab.onos.net.packet.PacketStore;
import org.onlab.onos.net.packet.PacketStoreDelegate;
import org.onlab.onos.net.provider.AbstractProviderRegistry;
import org.onlab.onos.net.provider.AbstractProviderService;
import org.slf4j.Logger;

/**
 * Provides a basic implementation of the packet SB &amp; NB APIs.
 */
@Component(immediate = true)
@Service
public class PacketManager
extends AbstractProviderRegistry<PacketProvider, PacketProviderService>
implements PacketService, PacketProviderRegistry {

    private final Logger log = getLogger(getClass());

    private final PacketStoreDelegate delegate = new InternalStoreDelegate();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private PacketStore store;

    private final Map<Integer, PacketProcessor> processors = new ConcurrentHashMap<>();

    @Activate
    public void activate() {
        store.setDelegate(delegate);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        store.unsetDelegate(delegate);
        log.info("Stopped");
    }

    @Override
    public void addProcessor(PacketProcessor processor, int priority) {
        checkNotNull(processor, "Processor cannot be null");
        processors.put(priority, processor);
    }

    @Override
    public void removeProcessor(PacketProcessor processor) {
        checkNotNull(processor, "Processor cannot be null");
        processors.values().remove(processor);
    }

    @Override
    public void emit(OutboundPacket packet) {
        checkNotNull(packet, "Packet cannot be null");

        store.emit(packet);
    }

    private void localEmit(OutboundPacket packet) {
        final Device device = deviceService.getDevice(packet.sendThrough());
        final PacketProvider packetProvider = getProvider(device.providerId());
        if (packetProvider != null) {
            packetProvider.emit(packet);
        }
    }

    @Override
    protected PacketProviderService createProviderService(PacketProvider provider) {
        return new InternalPacketProviderService(provider);
    }

    // Personalized link provider service issued to the supplied provider.
    private class InternalPacketProviderService
    extends AbstractProviderService<PacketProvider>
    implements PacketProviderService {

        protected InternalPacketProviderService(PacketProvider provider) {
            super(provider);
        }

        @Override
        public void processPacket(PacketContext context) {
            for (PacketProcessor processor : processors.values()) {
                processor.process(context);
            }
        }

    }

    /**
     * Internal callback from the packet store.
     */
    private class InternalStoreDelegate
    implements PacketStoreDelegate {
        @Override
        public void notify(PacketEvent event) {
            localEmit(event.subject());
        }
    }

}
