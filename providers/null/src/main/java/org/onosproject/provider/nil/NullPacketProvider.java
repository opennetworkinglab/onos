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
package org.onosproject.provider.nil;

import org.onlab.packet.Ethernet;
import org.onlab.packet.ICMP;
import org.onlab.util.Timer;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceAdminService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.packet.DefaultInboundPacket;
import org.onosproject.net.packet.DefaultPacketContext;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketProvider;
import org.onosproject.net.packet.PacketProviderService;
import org.slf4j.Logger;

import io.netty.util.Timeout;
import io.netty.util.TimerTask;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.google.common.collect.ImmutableList.copyOf;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.onosproject.net.MastershipRole.MASTER;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provider which generates simulated packets and acts as a sink for outbound
 * packets. To be used for benchmarking only.
 */
class NullPacketProvider extends NullProviders.AbstractNullProvider
        implements PacketProvider {

    private static final int INITIAL_DELAY = 5;
    private final Logger log = getLogger(getClass());

    // Arbitrary host src/dst
    private static final int SRC_HOST = 2;
    private static final int DST_HOST = 5;

    // Time between event firing, in milliseconds
    private int delay;

    // TODO: use host service to pick legitimate hosts connected to devices
    private HostService hostService;
    private PacketProviderService providerService;

    private List<Device> devices;
    private int currentDevice = 0;

    private Timeout timeout;

    /**
     * Starts the packet generation process.
     *
     * @param packetRate      packets per second
     * @param hostService     host service
     * @param deviceService   device service
     * @param providerService packet provider service
     */
    void start(int packetRate, HostService hostService,
               DeviceAdminService deviceService,
               PacketProviderService providerService) {
        this.hostService = hostService;
        this.providerService = providerService;

        this.devices = copyOf(deviceService.getDevices()).stream()
                .filter(d -> deviceService.getRole(d.id()) == MASTER)
                .collect(Collectors.toList());

        adjustRate(packetRate);
        timeout = Timer.newTimeout(new PacketDriverTask(), INITIAL_DELAY, SECONDS);
    }

    /**
     * Adjusts packet rate.
     *
     * @param packetRate new packet rate
     */
    void adjustRate(int packetRate) {
        boolean needsRestart = delay == 0 && packetRate > 0;
        delay = packetRate > 0 ? 1000 / packetRate : 0;
        if (needsRestart) {
            timeout = Timer.newTimeout(new PacketDriverTask(), 1, MILLISECONDS);
        }
        log.info("Settings: packetRate={}, delay={}", packetRate, delay);
    }

    /**
     * Stops the packet generation process.
     */
    void stop() {
        if (timeout != null) {
            timeout.cancel();
        }
    }

    @Override
    public void emit(OutboundPacket packet) {
        // We don't have a network to emit to. Keep a counter here, maybe?
    }

    /**
     * Generates packet events at a given rate.
     */
    private class PacketDriverTask implements TimerTask {

        // Filler echo request
        ICMP icmp;
        Ethernet eth;

        PacketDriverTask() {
            icmp = new ICMP();
            icmp.setIcmpType((byte) 8).setIcmpCode((byte) 0).setChecksum((short) 0);
            eth = new Ethernet();
            eth.setEtherType(Ethernet.TYPE_IPV4);
            eth.setPayload(icmp);
        }

        @Override
        public void run(Timeout to) {
            if (!devices.isEmpty() && !to.isCancelled() && delay > 0) {
                sendEvent(devices.get(Math.min(currentDevice, devices.size() - 1)));
                currentDevice = (currentDevice + 1) % devices.size();
                timeout = to.timer().newTimeout(to.task(), delay, TimeUnit.MILLISECONDS);
            }
        }

        private void sendEvent(Device device) {
            // Make it look like things came from ports attached to hosts
            eth.setSourceMACAddress("00:00:00:10:00:0" + SRC_HOST)
                    .setDestinationMACAddress("00:00:00:10:00:0" + DST_HOST);
            InboundPacket inPkt = new DefaultInboundPacket(
                    new ConnectPoint(device.id(), PortNumber.portNumber(SRC_HOST)),
                    eth, ByteBuffer.wrap(eth.serialize()));
            providerService.processPacket(new NullPacketContext(inPkt, null));
        }
    }

     // Minimal PacketContext to make core and applications happy.
    private final class NullPacketContext extends DefaultPacketContext {
        private NullPacketContext(InboundPacket inPkt, OutboundPacket outPkt) {
            super(System.currentTimeMillis(), inPkt, outPkt, false);
        }

        @Override
        public void send() {
            // We don't send anything out.
        }
    }

}
