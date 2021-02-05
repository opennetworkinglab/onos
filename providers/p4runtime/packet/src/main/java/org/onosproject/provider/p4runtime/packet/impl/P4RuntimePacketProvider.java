/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.provider.p4runtime.packet.impl;

import org.onlab.packet.EthType;
import org.onlab.util.PredictableExecutor;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.DefaultPacketContext;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProgrammable;
import org.onosproject.net.packet.PacketProvider;
import org.onosproject.net.packet.PacketProviderRegistry;
import org.onosproject.net.packet.PacketProviderService;
import org.onosproject.net.pi.model.PiPipelineInterpreter;
import org.onosproject.net.pi.runtime.PiPacketOperation;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.p4runtime.api.P4RuntimeController;
import org.onosproject.p4runtime.api.P4RuntimeEvent;
import org.onosproject.p4runtime.api.P4RuntimeEventListener;
import org.onosproject.p4runtime.api.P4RuntimePacketIn;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.Dictionary;
import java.util.concurrent.ExecutorService;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onlab.util.Tools.get;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.flow.DefaultTrafficTreatment.emptyTreatment;
import static org.onosproject.provider.p4runtime.packet.impl.OsgiPropertyConstants.P4RUNTIME_PACKET_PROVIDER_WORKERS;
import static org.onosproject.provider.p4runtime.packet.impl.OsgiPropertyConstants.P4RUNTIME_PACKET_PROVIDER_WORKERS_DEFAULT;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of a packet provider for P4Runtime device.
 */
@Component(immediate = true,
        property = {
                P4RUNTIME_PACKET_PROVIDER_WORKERS + ":Integer=" + P4RUNTIME_PACKET_PROVIDER_WORKERS_DEFAULT,
        })
public class P4RuntimePacketProvider extends AbstractProvider implements PacketProvider {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected P4RuntimeController controller;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PacketProviderRegistry providerRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService cfgService;

    private PacketProviderService providerService;

    private InternalPacketListener packetListener = new InternalPacketListener();

    /** Number of P4Runtime packet provider workers. */
    private int workers = P4RUNTIME_PACKET_PROVIDER_WORKERS_DEFAULT;
    // Predictable executor to stitch the packet processing always to the same thread
    private PredictableExecutor packetWorkers;

    /**
     * Creates a new P4Runtime packet provider.
     */
    public P4RuntimePacketProvider() {
        super(new ProviderId("p4runtime", "org.onosproject.provider.p4runtime.packet"));
    }

    @Activate
    protected void activate(ComponentContext context) {
        cfgService.registerProperties(getClass());
        providerService = providerRegistry.register(this);
        modified(context);
        controller.addListener(packetListener);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        cfgService.unregisterProperties(getClass(), false);
        controller.removeListener(packetListener);
        providerRegistry.unregister(this);
        providerService = null;
        stopWorkersIfNeeded();
        log.info("Stopped");
    }

    @Modified
    protected void modified(ComponentContext context) {
        if (context != null) {
            Dictionary<?, ?> properties = context.getProperties();
            int newWorkers;
            try {
                String s = get(properties, P4RUNTIME_PACKET_PROVIDER_WORKERS);
                newWorkers = isNullOrEmpty(s) ? workers : Integer.parseInt(s.trim());
            } catch (NumberFormatException | ClassCastException e) {
                newWorkers = workers;
            }

            // Stop previous executor and start a new one when there are changes in the config
            // OR during the start up of the service
            if (newWorkers != workers || packetWorkers == null) {
                workers = newWorkers;
                stopWorkersIfNeeded();
                packetWorkers = new PredictableExecutor(workers, groupedThreads("onos/p4rt",
                        "packet-worker-%d", log));
                log.info("Settings: p4RuntimePacketProviderWorkers={}", workers);
            }
        }
    }

    @Override
    public void emit(OutboundPacket packet) {
        if (packet != null) {
            DeviceId deviceId = packet.sendThrough();
            Device device = deviceService.getDevice(deviceId);
            if (device.is(PacketProgrammable.class) && mastershipService.isLocalMaster(deviceId)) {
                PacketProgrammable packetProgrammable = device.as(PacketProgrammable.class);
                packetProgrammable.emit(packet);
            } else {
                log.warn("No PacketProgrammable behavior for device {}", deviceId);
            }
        }
    }

    private void stopWorkersIfNeeded() {
        if (packetWorkers != null) {
            ExecutorService oldWorkerExecutor = packetWorkers;
            oldWorkerExecutor.shutdown();
            packetWorkers = null;
        }
    }

    private EthType.EtherType getEtherType(ByteBuffer data) {
        final short shortEthType = data.getShort(12);
        data.rewind();
        return EthType.EtherType.lookup(shortEthType);
    }

    /**
     * Internal packet context implementation.
     */
    private class P4RuntimePacketContext extends DefaultPacketContext {

        P4RuntimePacketContext(long time, InboundPacket inPkt, OutboundPacket outPkt, boolean block) {
            super(time, inPkt, outPkt, block);
        }

        @Override
        public void send() {

            if (this.block()) {
                log.info("Unable to send, packet context is blocked");
                return;
            }

            DeviceId deviceId = outPacket().sendThrough();
            ByteBuffer rawData = outPacket().data();

            TrafficTreatment treatment;
            if (outPacket().treatment() == null) {
                treatment = (treatmentBuilder() == null) ? emptyTreatment() : treatmentBuilder().build();
            } else {
                treatment = outPacket().treatment();
            }

            OutboundPacket outboundPacket = new DefaultOutboundPacket(deviceId, treatment, rawData);

            emit(outboundPacket);
        }
    }

    private void handleP4RuntimeEvent(P4RuntimeEvent event) {
        //Mastership message is sent to everybody but picked up only by master.
        //FIXME we need the device ID into p4RuntimeEvnetSubject to check for mastsership
        if (!(event.subject() instanceof P4RuntimePacketIn) || event.type() != P4RuntimeEvent.Type.PACKET_IN) {
            log.debug("Unrecognized event type {}, discarding", event.type());
            // Not a packet-in event, ignore it.
            return;
        }
        P4RuntimePacketIn eventSubject = (P4RuntimePacketIn) event.subject();
        DeviceId deviceId = eventSubject.deviceId();

        Device device = deviceService.getDevice(eventSubject.deviceId());
        if (device == null) {
            log.warn("Unable to process packet-in from {}, device is null in the core", deviceId);
            return;
        }

        if (!device.is(PiPipelineInterpreter.class)) {
            log.warn("Unable to process packet-in from {}, device has no PiPipelineInterpreter behaviour",
                    deviceId);
            return;
        }

        PiPacketOperation operation = eventSubject.packetOperation();
        InboundPacket inPkt;
        try {
            inPkt = device.as(PiPipelineInterpreter.class).mapInboundPacket(operation, deviceId);
        } catch (PiPipelineInterpreter.PiInterpreterException e) {
            log.warn("Unable to interpret inbound packet from {}: {}", deviceId, e.getMessage());
            return;
        }

        if (log.isTraceEnabled()) {
            final EthType.EtherType etherType = getEtherType(inPkt.unparsed());
            log.trace("Received PACKET-IN <<< device={} ingress_port={} eth_type={}",
                    inPkt.receivedFrom().deviceId(), inPkt.receivedFrom().port(),
                    etherType.ethType().toString());
        }

        if (inPkt == null) {
            log.debug("Received null inbound packet. Ignoring.");
            return;
        }

        OutboundPacket outPkt = new DefaultOutboundPacket(eventSubject.deviceId(), null,
                operation.data().asReadOnlyBuffer());
        PacketContext pktCtx = new P4RuntimePacketContext(System.currentTimeMillis(), inPkt, outPkt, false);

        // Pushing the packet context up for processing.
        providerService.processPacket(pktCtx);
    }

    /**
     * Internal packet listener to handle packet-in events received from the P4Runtime controller.
     */
    private class InternalPacketListener implements P4RuntimeEventListener {

        @Override
        public void event(P4RuntimeEvent event) {
            // Offload to another executor to prevent the ejection of the listener - it uses
            // the device id to stitch the packets coming from a device always to the same worker
            packetWorkers.execute(() -> handleP4RuntimeEvent(event), event.subject().deviceId().hashCode());
        }
    }
}
