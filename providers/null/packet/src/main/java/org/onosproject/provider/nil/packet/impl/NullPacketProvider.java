/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.provider.nil.packet.impl;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onlab.util.Tools.delay;
import static org.onlab.util.Tools.namedThreads;
import static org.slf4j.LoggerFactory.getLogger;

import java.nio.ByteBuffer;
import java.util.Dictionary;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.Ethernet;
import org.onlab.packet.ICMP;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.packet.DefaultInboundPacket;
import org.onosproject.net.packet.DefaultPacketContext;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProvider;
import org.onosproject.net.packet.PacketProviderRegistry;
import org.onosproject.net.packet.PacketProviderService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

/**
 * Provider which 1) intercepts network-bound messages from the core, and 2)
 * generates PacketEvents at some tunable rate. To be used for benchmarking
 * only.
 */
@Component(immediate = true)
public class NullPacketProvider extends AbstractProvider implements
        PacketProvider {

    private final Logger log = getLogger(getClass());

    // Default packetEvent generation rate (in packets/sec)
    // If 0, we are just a sink for network-bound packets
    private static final int DEFAULT_RATE = 5;
    // arbitrary host "destination"
    private static final int DESTHOST = 5;

    private PacketProviderService providerService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketProviderRegistry providerRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    // Rate to generate PacketEvents, per second
    @Property(name = "pktRate", intValue = DEFAULT_RATE,
            label = "Rate of PacketEvent generation")
    private int pktRate = DEFAULT_RATE;

    private ExecutorService packetDriver = Executors.newFixedThreadPool(1,
            namedThreads("onos-null-packet-driver"));

    public NullPacketProvider() {
        super(new ProviderId("null", "org.onosproject.provider.nil"));
    }

    @Activate
    public void activate(ComponentContext context) {
        providerService = providerRegistry.register(this);
        if (!modified(context)) {
            packetDriver.submit(new PacketDriver());
        }
        log.info("started");
    }

    @Deactivate
    public void deactivate(ComponentContext context) {
        try {
            packetDriver.awaitTermination(1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.error("PacketDriver did not terminate");
        }
        packetDriver.shutdownNow();
        providerRegistry.unregister(this);
        log.info("stopped");
    }

    @Modified
    public boolean modified(ComponentContext context) {
        if (context == null) {
            log.info("No configuration change, using defaults: pktRate={}",
                    DEFAULT_RATE);
            return false;
        }
        Dictionary<?, ?> properties = context.getProperties();

        int newRate;
        try {
            String s = String.valueOf(properties.get("pktRate"));
            newRate = isNullOrEmpty(s) ? pktRate : Integer.parseInt(s.trim());
        } catch (Exception e) {
            log.warn(e.getMessage());
            newRate = pktRate;
        }

        if (newRate != pktRate) {
            pktRate = newRate;
            packetDriver.submit(new PacketDriver());
            log.info("Using new settings: pktRate={}", pktRate);
            return true;
        }
        return false;
    }

    @Override
    public void emit(OutboundPacket packet) {
        // We don't have a network to emit to. Keep a counter here, maybe?
    }

    /**
     * Generates packet events at a given rate.
     */
    private class PacketDriver implements Runnable {

        // time between event firing, in milliseconds
        int pktInterval;
        // filler echo request
        ICMP icmp;
        Ethernet eth;

        public PacketDriver() {
            pktInterval = 1000 / pktRate;
            icmp = new ICMP();
            icmp.setIcmpType((byte) 8).setIcmpCode((byte) 0)
                    .setChecksum((short) 0);
            eth = new Ethernet();
            eth.setEtherType(Ethernet.TYPE_IPV4);
            eth.setPayload(icmp);
        }

        @Override
        public void run() {
            log.info("PacketDriver started");
            while (!packetDriver.isShutdown()) {
                for (Device dev : deviceService.getDevices()) {
                    sendEvents(dev);
                }
            }
        }

        private void sendEvents(Device dev) {
            // make it look like things came from ports attached to hosts
            for (int i = 0; i < 4; i++) {
                eth.setSourceMACAddress("00:00:10:00:00:0" + i)
                        .setDestinationMACAddress("00:00:10:00:00:0" + DESTHOST);
                InboundPacket inPkt = new DefaultInboundPacket(
                        new ConnectPoint(dev.id(), PortNumber.portNumber(i)),
                        eth, ByteBuffer.wrap(eth.serialize()));
                PacketContext pctx = new NullPacketContext(
                        System.currentTimeMillis(), inPkt, null, false);
                providerService.processPacket(pctx);
                delay(pktInterval);
            }
        }

    }

    /**
     * Minimal PacketContext to make core + applications happy.
     */
    private class NullPacketContext extends DefaultPacketContext {

        public NullPacketContext(long time, InboundPacket inPkt,
                OutboundPacket outPkt, boolean block) {
            super(time, inPkt, outPkt, block);
        }

        @Override
        public void send() {
            // We don't send anything out.
        }

    }

}
