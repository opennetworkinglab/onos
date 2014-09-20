package org.onlab.onos.net.packet.impl;

import static com.google.common.base.Preconditions.checkNotNull;
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
 */
@Component(immediate = true)
@Service
public class PacketManager
extends AbstractProviderRegistry<PacketProvider, PacketProviderService>
implements PacketService, PacketProviderRegistry {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private DeviceService deviceService;

    private final Map<Integer, PacketProcessor> processors = new TreeMap<>();

    @Activate
    public void activate() {
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
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
        final Device device = deviceService.getDevice(packet.sendThrough());
        final PacketProvider packetProvider = getProvider(device.providerId());
        packetProvider.emit(packet);
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
}
