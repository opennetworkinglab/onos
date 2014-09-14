package org.onlab.onos.net.trivial.packet.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Map;
import java.util.TreeMap;

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
import org.onlab.onos.net.packet.PacketProcessor;
import org.onlab.onos.net.packet.PacketProvider;
import org.onlab.onos.net.packet.PacketProviderRegistry;
import org.onlab.onos.net.packet.PacketProviderService;
import org.onlab.onos.net.packet.PacketService;
import org.onlab.onos.net.provider.AbstractProviderRegistry;
import org.onlab.onos.net.provider.AbstractProviderService;
import org.slf4j.Logger;


/**
 * Provides a basic implementation of the packet SB &amp; NB APIs.
 *
 */
@Component(immediate = true)
@Service
public class SimplePacketManager
extends AbstractProviderRegistry<PacketProvider, PacketProviderService>
implements PacketService, PacketProviderRegistry {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private DeviceService deviceService;

    private final Map<Integer, PacketProcessor> processors = new TreeMap<>();

    private final PacketProcessor reactiveProcessor = new ReactivePacketProcessor();

    @Activate
    public void activate() {
        addProcessor(reactiveProcessor, PacketProcessor.ADVISOR_MAX + 1);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        removeProcessor(reactiveProcessor);
        log.info("Stopped");
    }

    @Override
    public void addProcessor(PacketProcessor processor, int priority) {
        processors.put(priority, processor);
    }

    @Override
    public void removeProcessor(PacketProcessor processor) {
        processors.values().remove(processor);
    }

    @Override
    public void emit(OutboundPacket packet) {
        final Device device = deviceService.getDevice(packet.sendThrough());
        final PacketProvider packetProvider = getProvider(device.providerId());
        packetProvider.emit(packet);
    }

    @Override
    protected PacketProviderService createProviderService(
            PacketProvider provider) {
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
}
