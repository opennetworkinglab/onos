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
package org.onosproject.packetthrottle;

import com.google.common.collect.ImmutableMap;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.net.packet.PacketInFilter;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.packet.packetfilter.DefaultPacketInFilter;
import org.onosproject.net.packet.packetfilter.ArpPacketClassifier;
import org.onosproject.net.packet.packetfilter.Dhcp6IndirectPacketClassifier;
import org.onosproject.net.packet.packetfilter.Dhcp6DirectPacketClassifier;
import org.onosproject.net.packet.packetfilter.DhcpPacketClassifier;
import org.onosproject.net.packet.packetfilter.NAPacketClassifier;
import org.onosproject.net.packet.packetfilter.NSPacketClassifier;
import org.onosproject.net.packet.packetfilter.IcmpPacketClassifier;
import org.onosproject.net.packet.packetfilter.Icmp6PacketClassifier;
import org.onosproject.packetthrottle.api.PacketThrottleService;


import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onlab.util.Tools.get;

/**
 * Manage the packet throttle for various type of packets.
 */
@Component(immediate = true)
@Service
public class PacketThrottleManager implements PacketThrottleService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    protected static final String ARP_FILTER = "arpFilter";
    protected static final String DHCP_FILTER = "dhcpFilter";
    protected static final String NS_FILTER = "nsFilter";
    protected static final String NA_FILTER = "naFilter";
    protected static final String DHCP6_DIRECT_FILTER = "dhcp6DirectFilter";
    protected static final String DHCP6_INDIRECT_FILTER = "dhcp6IndirectFilter";
    protected static final String ICMP_FILTER = "icmpFilter";
    protected static final String ICMP6_FILTER = "icmp6Filter";


    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService configService;

    private static final int PPS_ARP = 100;
    private static final int PPS_DHCP = 100;
    private static final int PPS_NS = 100;
    private static final int PPS_NA = 100;
    private static final int PPS_DHCP6_DIRECT = 100;
    private static final int PPS_DHCP6_INDIRECT = 100;
    private static final int PPS_ICMP = 100;
    private static final int PPS_ICMP6 = 100;

    private static final int WIN_SIZE_ARP_MS = 500;
    private static final int WIN_SIZE_DHCP_MS = 500;
    private static final int WIN_SIZE_NS_MS = 500;
    private static final int WIN_SIZE_NA_MS = 500;
    private static final int WIN_SIZE_DHCP6_DIRECT_MS = 500;
    private static final int WIN_SIZE_DHCP6_INDIRECT_MS = 500;
    private static final int WIN_SIZE_ICMP_MS = 500;
    private static final int WIN_SIZE_ICMP6_MS = 500;

    private static final int GUARD_TIME_ARP_SEC = 10;
    private static final int GUARD_TIME_DHCP_SEC = 10;
    private static final int GUARD_TIME_NS_SEC = 10;
    private static final int GUARD_TIME_NA_SEC = 10;
    private static final int GUARD_TIME_DHCP6_DIRECT_SEC = 10;
    private static final int GUARD_TIME_DHCP6_INDIRECT_SEC = 10;
    private static final int GUARD_TIME_ICMP_SEC = 10;
    private static final int GUARD_TIME_ICMP6_SEC = 10;

    private static final int WIN_THRES_ARP = 10;
    private static final int WIN_THRES_DHCP = 10;
    private static final int WIN_THRES_NS = 10;
    private static final int WIN_THRES_NA = 10;
    private static final int WIN_THRES_DHCP6_DIRECT = 10;
    private static final int WIN_THRES_DHCP6_INDIRECT = 10;
    private static final int WIN_THRES_ICMP = 10;
    private static final int WIN_THRES_ICMP6 = 10;

    /**
     * Parameter to set packet per second rate for all filter types.
     */
    @Property(name = "ppsArp", intValue = PPS_ARP,
            label = "Packet per second for the ARP packet")
    private int ppsArp = PPS_ARP;

    @Property(name = "ppsDhcp", intValue = PPS_DHCP,
            label = "Packet per second for the DHCP packet")
    private int ppsDhcp = PPS_DHCP;

    @Property(name = "ppsNs", intValue = PPS_NS,
            label = "Packet per second for the NS packet")
    private int ppsNs = PPS_NS;

    @Property(name = "ppsNa", intValue = PPS_NA,
            label = "Packet per second for the NA packet")
    private int ppsNa = PPS_NA;

    @Property(name = "ppsDhcp6Direct", intValue = PPS_DHCP6_DIRECT,
            label = "Packet per second for the DHCP6 Direct message")
    private int ppsDhcp6Direct = PPS_DHCP6_DIRECT;

    @Property(name = "ppsDhcp6Indirect", intValue = PPS_DHCP6_INDIRECT,
            label = "Packet per second for the DHCP6 indirect message")
    private int ppsDhcp6Indirect = PPS_DHCP6_INDIRECT;

    @Property(name = "ppsIcmp", intValue = PPS_ICMP,
            label = "Packet per second for the ICMP message")
    private int ppsIcmp = PPS_ICMP;

    @Property(name = "ppsIcmp6", intValue = PPS_ICMP6,
            label = "Packet per second for the ICMP6 message")
    private int ppsIcmp6 = PPS_ICMP6;


    /**
     * Parameter to set window size in milli seconds to check overflow of packets.
     */

    @Property(name = "winSizeArp", intValue = WIN_SIZE_ARP_MS,
            label = "Window size for the ARP packet in milliseconds")
    private int winSizeArp = WIN_SIZE_ARP_MS;

    @Property(name = "winSizeDhcp", intValue = WIN_SIZE_DHCP_MS,
            label = "Window size for the DHCP packet in milliseconds")
    private int winSizeDhcp = WIN_SIZE_DHCP_MS;

    @Property(name = "winSizeNs", intValue = WIN_SIZE_NS_MS,
            label = "Window size for the NS packet in milliseconds")
    private int winSizeNs = WIN_SIZE_NS_MS;

    @Property(name = "winSizeNa", intValue = WIN_SIZE_NA_MS,
            label = "Window size for the NA packet in milliseconds")
    private int winSizeNa = WIN_SIZE_NA_MS;

    @Property(name = "winSizeDhcp6Direct", intValue = WIN_SIZE_DHCP6_DIRECT_MS,
            label = "Window size for the DHCP6 Direct message in milliseconds")
    private int winSizeDhcp6Direct = WIN_SIZE_DHCP6_DIRECT_MS;

    @Property(name = "winSizeDhcp6Indirect", intValue = WIN_SIZE_DHCP6_INDIRECT_MS,
            label = "Window size for the DHCP6 indirect message in milliseconds")
    private int winSizeDhcp6Indirect = WIN_SIZE_DHCP6_INDIRECT_MS;

    @Property(name = "winSizeIcmp", intValue = WIN_SIZE_ICMP_MS,
            label = "Window size for the ICMP message in milliseconds")
    private int winSizeIcmp = WIN_SIZE_ICMP_MS;

    @Property(name = "winSizeIcmp6", intValue = WIN_SIZE_ICMP6_MS,
            label = "Window size for the ICMP6 message in milliseconds")
    private int winSizeIcmp6 = WIN_SIZE_ICMP6_MS;

    /**
     * Time duration for which no more packets will be processed for a given filter type
     * provided consecutive overflow windows happens.
     */

    @Property(name = "guardTimeArp", intValue = GUARD_TIME_ARP_SEC,
            label = "Guard time for the ARP packet in seconds")
    private int guardTimeArp = GUARD_TIME_ARP_SEC;

    @Property(name = "guardTimeDhcp", intValue = GUARD_TIME_DHCP_SEC,
            label = "Guard time for the DHCP packet in seconds")
    private int guardTimeDhcp = GUARD_TIME_DHCP_SEC;

    @Property(name = "guardTimeNs", intValue = GUARD_TIME_NS_SEC,
            label = "Guard time for the NS packet in seconds")
    private int guardTimeNs = GUARD_TIME_NS_SEC;

    @Property(name = "guardTimeNa", intValue = GUARD_TIME_NA_SEC,
            label = "Guard time for the NA packet in seconds")
    private int guardTimeNa = GUARD_TIME_NA_SEC;

    @Property(name = "guardTimeDhcp6Direct", intValue = GUARD_TIME_DHCP6_DIRECT_SEC,
            label = "Guard time for the DHCP6 Direct message in seconds")
    private int guardTimeDhcp6Direct = GUARD_TIME_DHCP6_DIRECT_SEC;

    @Property(name = "guardTimeDhcp6Indirect", intValue = GUARD_TIME_DHCP6_INDIRECT_SEC,
            label = "Guard time for the DHCP6 indirect message in seconds")
    private int guardTimeDhcp6Indirect = GUARD_TIME_DHCP6_INDIRECT_SEC;

    @Property(name = "guardTimeIcmp", intValue = GUARD_TIME_ICMP_SEC,
            label = "Guard time for the ICMP message in seconds")
    private int guardTimeIcmp = GUARD_TIME_ICMP_SEC;

    @Property(name = "guardTimeIcmp6", intValue = GUARD_TIME_ICMP6_SEC,
            label = "Guard time for the ICMP6 message in seconds")
    private int guardTimeIcmp6 = GUARD_TIME_ICMP6_SEC;

    /**
     * Consecutive overflow window threshold.
     */

    @Property(name = "winThresArp", intValue = WIN_THRES_ARP,
            label = "Window drop threshold for the ARP packet")
    private int winThresArp = WIN_THRES_ARP;

    @Property(name = "winThresDhcp", intValue = WIN_THRES_DHCP,
            label = "Window drop threshold for the DHCP packet")
    private int winThresDhcp = WIN_THRES_DHCP;

    @Property(name = "winThresNs", intValue = WIN_THRES_NS,
            label = "Window drop threshold for the NS packet")
    private int winThresNs = WIN_THRES_NS;

    @Property(name = "winThresNa", intValue = WIN_THRES_NA,
            label = "Window drop threshold for the NA packet")
    private int winThresNa = WIN_THRES_NA;

    @Property(name = "winThresDhcp6Direct", intValue = WIN_THRES_DHCP6_DIRECT,
            label = "Window drop threshold for the DHCP6 Direct message")
    private int winThresDhcp6Direct = WIN_THRES_DHCP6_DIRECT;

    @Property(name = "winThresDhcp6Indirect", intValue = WIN_THRES_DHCP6_INDIRECT,
            label = "Window drop threshold for the DHCP6 indirect message")
    private int winThresDhcp6Indirect = WIN_THRES_DHCP6_INDIRECT;

    @Property(name = "winThresIcmp", intValue = WIN_THRES_ICMP,
            label = "Window drop threshold for the ICMP message")
    private int winThresIcmp = WIN_THRES_ICMP;

    @Property(name = "winThresIcmp6", intValue = WIN_THRES_ICMP6,
            label = "Window drop threshold for the ICMP6 message")
    private int winThresIcmp6 = WIN_THRES_ICMP6;




    private Map<String, PacketInFilter> mapCounterFilter = new HashMap<>();

    @Activate
    protected void activate() {
        log.info("Started");
        configService.registerProperties(getClass());
        createAllFilters();
    }

    @Deactivate
    protected void deactivate() {
        configService.unregisterProperties(getClass(), false);
        removeAllFilters();
        log.info("Stopped");
    }

    private void checkChangeInPps(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();
        int newPpsArp, newPpsDhcp, newPpsNs, newPpsNa, newPpsDhcp6Direct;
        int newPpsDhcp6Indirect, newPpsIcmp, newPpsIcmp6;
        try {
            String s = get(properties, "ppsArp");
            newPpsArp = isNullOrEmpty(s) ? ppsArp : Integer.parseInt(s.trim());

            s = get(properties, "ppsDhcp");
            newPpsDhcp = isNullOrEmpty(s) ? ppsDhcp : Integer.parseInt(s.trim());

            s = get(properties, "ppsNs");
            newPpsNs = isNullOrEmpty(s) ? ppsNs : Integer.parseInt(s.trim());

            s = get(properties, "ppsNa");
            newPpsNa = isNullOrEmpty(s) ? ppsNa : Integer.parseInt(s.trim());

            s = get(properties, "ppsDhcp6Direct");
            newPpsDhcp6Direct = isNullOrEmpty(s) ? ppsDhcp6Direct : Integer.parseInt(s.trim());

            s = get(properties, "ppsDhcp6Indirect");
            newPpsDhcp6Indirect = isNullOrEmpty(s) ? ppsDhcp6Indirect : Integer.parseInt(s.trim());

            s = get(properties, "ppsIcmp");
            newPpsIcmp = isNullOrEmpty(s) ? ppsIcmp : Integer.parseInt(s.trim());

            s = get(properties, "ppsIcmp6");
            newPpsIcmp6 = isNullOrEmpty(s) ? ppsIcmp6 : Integer.parseInt(s.trim());
        } catch (NumberFormatException | ClassCastException e) {
            newPpsArp = PPS_ARP;
            newPpsDhcp = PPS_DHCP;
            newPpsNs = PPS_NS;
            newPpsNa = PPS_NA;
            newPpsDhcp6Direct = PPS_DHCP6_DIRECT;
            newPpsDhcp6Indirect = PPS_DHCP6_INDIRECT;
            newPpsIcmp = PPS_ICMP;
            newPpsIcmp6 = PPS_ICMP6;
        }
        if (newPpsArp != ppsArp) {
            ppsArp = newPpsArp;
            mapCounterFilter.get(ARP_FILTER).setPps(ppsArp);
        }
        if (newPpsDhcp != ppsDhcp) {
            ppsDhcp = newPpsDhcp;
            mapCounterFilter.get(DHCP_FILTER).setPps(ppsDhcp);
        }
        if (newPpsNs != ppsNs) {
            ppsNs = newPpsNs;
            mapCounterFilter.get(NS_FILTER).setPps(ppsNs);
        }
        if (newPpsNa != ppsNa) {
            ppsNa = newPpsNa;
            mapCounterFilter.get(NA_FILTER).setPps(ppsNa);
        }
        if (newPpsDhcp6Direct != ppsDhcp6Direct) {
            ppsDhcp6Direct = newPpsDhcp6Direct;
            mapCounterFilter.get(DHCP6_DIRECT_FILTER).setPps(ppsDhcp6Direct);
        }
        if (newPpsDhcp6Indirect != ppsDhcp6Indirect) {
            ppsDhcp6Indirect = newPpsDhcp6Indirect;
            mapCounterFilter.get(DHCP6_INDIRECT_FILTER).setPps(ppsDhcp6Indirect);
        }
        if (newPpsIcmp != ppsIcmp) {
            ppsIcmp = newPpsIcmp;
            mapCounterFilter.get(ICMP_FILTER).setPps(ppsIcmp);
        }
        if (newPpsIcmp6 != ppsIcmp6) {
            ppsIcmp6 = newPpsIcmp6;
            mapCounterFilter.get(ICMP6_FILTER).setPps(ppsIcmp6);
        }
    }

    private void checkChangeInWinSize(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();

        int newWinSizeArp, newWinSizeDhcp, newWinSizeNs, newWinSizeNa;
        int newWinSizeDhcp6Direct, newWinSizeDhcp6Indirect, newWinSizeIcmp, newWinSizeIcmp6;
        try {
            String s = get(properties, "winSizeArp");
            newWinSizeArp = isNullOrEmpty(s) ? winSizeArp : Integer.parseInt(s.trim());

            s = get(properties, "winSizeDhcp");
            newWinSizeDhcp = isNullOrEmpty(s) ? winSizeDhcp : Integer.parseInt(s.trim());

            s = get(properties, "winSizeNs");
            newWinSizeNs = isNullOrEmpty(s) ? winSizeNs : Integer.parseInt(s.trim());

            s = get(properties, "winSizeNa");
            newWinSizeNa = isNullOrEmpty(s) ? winSizeNa : Integer.parseInt(s.trim());

            s = get(properties, "winSizeDhcp6Direct");
            newWinSizeDhcp6Direct = isNullOrEmpty(s) ? winSizeDhcp6Direct : Integer.parseInt(s.trim());

            s = get(properties, "winSizeDhcp6Indirect");
            newWinSizeDhcp6Indirect = isNullOrEmpty(s) ? winSizeDhcp6Indirect : Integer.parseInt(s.trim());

            s = get(properties, "winSizeIcmp");
            newWinSizeIcmp = isNullOrEmpty(s) ? winSizeIcmp : Integer.parseInt(s.trim());

            s = get(properties, "winSizeIcmp6");
            newWinSizeIcmp6 = isNullOrEmpty(s) ? winSizeIcmp6 : Integer.parseInt(s.trim());
        } catch (NumberFormatException | ClassCastException e) {
            newWinSizeArp = WIN_SIZE_ARP_MS;
            newWinSizeDhcp = WIN_SIZE_DHCP_MS;
            newWinSizeNs = WIN_SIZE_NS_MS;
            newWinSizeNa = WIN_SIZE_NA_MS;
            newWinSizeDhcp6Direct = WIN_SIZE_DHCP6_DIRECT_MS;
            newWinSizeDhcp6Indirect = WIN_SIZE_DHCP6_INDIRECT_MS;
            newWinSizeIcmp = WIN_SIZE_ICMP_MS;
            newWinSizeIcmp6 = WIN_SIZE_ICMP6_MS;
        }
        if (newWinSizeArp != winSizeArp) {
            winSizeArp = newWinSizeArp;
            mapCounterFilter.get(ARP_FILTER).setWinSize(winSizeArp);
        }
        if (newWinSizeDhcp != winSizeDhcp) {
            winSizeDhcp = newWinSizeDhcp;
            mapCounterFilter.get(DHCP_FILTER).setWinSize(winSizeDhcp);
        }
        if (newWinSizeNs != winSizeNs) {
            winSizeNs = newWinSizeNs;
            mapCounterFilter.get(NS_FILTER).setWinSize(winSizeNs);
        }
        if (newWinSizeNa != winSizeNa) {
            winSizeNa = newWinSizeNa;
            mapCounterFilter.get(NA_FILTER).setWinSize(winSizeNa);
        }
        if (newWinSizeDhcp6Direct != winSizeDhcp6Direct) {
            winSizeDhcp6Direct = newWinSizeDhcp6Direct;
            mapCounterFilter.get(DHCP6_DIRECT_FILTER).setWinSize(winSizeDhcp6Direct);
        }
        if (newWinSizeDhcp6Indirect != winSizeDhcp6Indirect) {
            winSizeDhcp6Indirect = newWinSizeDhcp6Indirect;
            mapCounterFilter.get(DHCP6_INDIRECT_FILTER).setWinSize(winSizeDhcp6Indirect);
        }
        if (newWinSizeIcmp != winSizeIcmp) {
            winSizeIcmp = newWinSizeIcmp;
            mapCounterFilter.get(ICMP_FILTER).setWinSize(winSizeIcmp);
        }
        if (newWinSizeIcmp6 != winSizeIcmp6) {
            winSizeIcmp6 = newWinSizeIcmp6;
            mapCounterFilter.get(ICMP6_FILTER).setWinSize(winSizeIcmp6);
        }

    }

    private void checkChangeInGuardTime(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();
        int newGuardTimeArp, newGuardTimeDhcp, newGuardTimeNs, newGuardTimeNa;
        int newGuardTimeDhcp6Direct, newGuardTimeDhcp6Indirect, newGuardTimeIcmp, newGuardTimeIcmp6;
        try {
            String s = get(properties, "guardTimeArp");
            newGuardTimeArp = isNullOrEmpty(s) ? guardTimeArp : Integer.parseInt(s.trim());

            s = get(properties, "guardTimeDhcp");
            newGuardTimeDhcp = isNullOrEmpty(s) ? guardTimeDhcp : Integer.parseInt(s.trim());

            s = get(properties, "guardTimeNs");
            newGuardTimeNs = isNullOrEmpty(s) ? guardTimeNs : Integer.parseInt(s.trim());

            s = get(properties, "guardTimeNa");
            newGuardTimeNa = isNullOrEmpty(s) ? guardTimeNa : Integer.parseInt(s.trim());

            s = get(properties, "guardTimeDhcp6Direct");
            newGuardTimeDhcp6Direct = isNullOrEmpty(s) ? guardTimeDhcp6Direct : Integer.parseInt(s.trim());

            s = get(properties, "guardTimeDhcp6Indirect");
            newGuardTimeDhcp6Indirect = isNullOrEmpty(s) ? guardTimeDhcp6Indirect : Integer.parseInt(s.trim());

            s = get(properties, "guardTimeIcmp");
            newGuardTimeIcmp = isNullOrEmpty(s) ? guardTimeIcmp : Integer.parseInt(s.trim());

            s = get(properties, "guardTimeIcmp6");
            newGuardTimeIcmp6 = isNullOrEmpty(s) ? guardTimeIcmp6 : Integer.parseInt(s.trim());
        } catch (NumberFormatException | ClassCastException e) {

            newGuardTimeArp = GUARD_TIME_ARP_SEC;
            newGuardTimeDhcp = GUARD_TIME_DHCP_SEC;
            newGuardTimeNs = GUARD_TIME_NS_SEC;
            newGuardTimeNa = GUARD_TIME_NA_SEC;
            newGuardTimeDhcp6Direct = GUARD_TIME_DHCP6_DIRECT_SEC;
            newGuardTimeDhcp6Indirect = GUARD_TIME_DHCP6_INDIRECT_SEC;
            newGuardTimeIcmp = GUARD_TIME_ICMP_SEC;
            newGuardTimeIcmp6 = GUARD_TIME_ICMP6_SEC;
        }
        if (newGuardTimeArp != guardTimeArp) {
            guardTimeArp = newGuardTimeArp;
            mapCounterFilter.get(ARP_FILTER).setGuardTime(guardTimeArp);
        }
        if (newGuardTimeDhcp != guardTimeDhcp) {
            guardTimeDhcp = newGuardTimeDhcp;
            mapCounterFilter.get(DHCP_FILTER).setGuardTime(guardTimeDhcp);
        }
        if (newGuardTimeNs != guardTimeNs) {
            guardTimeNs = newGuardTimeNs;
            mapCounterFilter.get(NS_FILTER).setGuardTime(guardTimeNs);
        }
        if (newGuardTimeNa != guardTimeNa) {
            guardTimeNa = newGuardTimeNa;
            mapCounterFilter.get(NA_FILTER).setGuardTime(guardTimeNa);
        }
        if (newGuardTimeDhcp6Direct != guardTimeDhcp6Direct) {
            guardTimeDhcp6Direct = newGuardTimeDhcp6Direct;
            mapCounterFilter.get(DHCP6_DIRECT_FILTER).setGuardTime(guardTimeDhcp6Direct);
        }
        if (newGuardTimeDhcp6Indirect != guardTimeDhcp6Indirect) {
            guardTimeDhcp6Indirect = newGuardTimeDhcp6Indirect;
            mapCounterFilter.get(DHCP6_INDIRECT_FILTER).setGuardTime(guardTimeDhcp6Indirect);
        }
        if (newGuardTimeIcmp != guardTimeIcmp) {
            guardTimeIcmp = newGuardTimeIcmp;
            mapCounterFilter.get(ICMP_FILTER).setGuardTime(guardTimeIcmp);
        }
        if (newGuardTimeIcmp6 != guardTimeIcmp6) {
            guardTimeIcmp6 = newGuardTimeIcmp6;
            mapCounterFilter.get(ICMP6_FILTER).setGuardTime(guardTimeIcmp6);
        }


    }

    private void checkChangeInWinThres(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();

        int newWinThresArp, newWinThresDhcp, newWinThresNs, newWinThresNa;
        int newWinThresDhcp6Direct, newWinThresDhcp6Indirect, newWinThresIcmp, newWinThresIcmp6;
        try {

            String s = get(properties, "winThresArp");
            newWinThresArp = isNullOrEmpty(s) ? winThresArp : Integer.parseInt(s.trim());

            s = get(properties, "winThresDhcp");
            newWinThresDhcp = isNullOrEmpty(s) ? winThresDhcp : Integer.parseInt(s.trim());

            s = get(properties, "winThresNs");
            newWinThresNs = isNullOrEmpty(s) ? winThresNs : Integer.parseInt(s.trim());

            s = get(properties, "winThresNa");
            newWinThresNa = isNullOrEmpty(s) ? winThresNa : Integer.parseInt(s.trim());

            s = get(properties, "winThresDhcp6Direct");
            newWinThresDhcp6Direct = isNullOrEmpty(s) ? winThresDhcp6Direct : Integer.parseInt(s.trim());

            s = get(properties, "winThresDhcp6Indirect");
            newWinThresDhcp6Indirect = isNullOrEmpty(s) ? winThresDhcp6Indirect : Integer.parseInt(s.trim());

            s = get(properties, "winThresIcmp");
            newWinThresIcmp = isNullOrEmpty(s) ? winThresIcmp : Integer.parseInt(s.trim());

            s = get(properties, "winThresIcmp6");
            newWinThresIcmp6 = isNullOrEmpty(s) ? winThresIcmp6 : Integer.parseInt(s.trim());

        } catch (NumberFormatException | ClassCastException e) {
            newWinThresArp = WIN_THRES_ARP;
            newWinThresDhcp = WIN_THRES_DHCP;
            newWinThresNs = WIN_THRES_NS;
            newWinThresNa = WIN_THRES_NA;
            newWinThresDhcp6Direct = WIN_THRES_DHCP6_DIRECT;
            newWinThresDhcp6Indirect = WIN_THRES_DHCP6_INDIRECT;
            newWinThresIcmp = WIN_THRES_ICMP;
            newWinThresIcmp6 = WIN_THRES_ICMP6;

        }

        if (newWinThresArp != winThresArp) {
            winThresArp = newWinThresArp;
            mapCounterFilter.get(ARP_FILTER).setWinThres(winThresArp);
        }
        if (newWinThresDhcp != winThresDhcp) {
            winThresDhcp = newWinThresDhcp;
            mapCounterFilter.get(DHCP_FILTER).setWinThres(winThresDhcp);
        }
        if (newWinThresNs != winThresNs) {
            winThresNs = newWinThresNs;
            mapCounterFilter.get(NS_FILTER).setWinThres(winThresNs);
        }
        if (newWinThresNa != winThresNa) {
            winThresNa = newWinThresNa;
            mapCounterFilter.get(NA_FILTER).setWinThres(winThresNa);
        }
        if (newWinThresDhcp6Direct != winThresDhcp6Direct) {
            winThresDhcp6Direct = newWinThresDhcp6Direct;
            mapCounterFilter.get(DHCP6_DIRECT_FILTER).setWinThres(winThresDhcp6Direct);
        }
        if (newWinThresDhcp6Indirect != winThresDhcp6Indirect) {
            winThresDhcp6Indirect = newWinThresDhcp6Indirect;
            mapCounterFilter.get(DHCP6_INDIRECT_FILTER).setWinThres(winThresDhcp6Indirect);
        }
        if (newWinThresIcmp != winThresIcmp) {
            winThresIcmp = newWinThresIcmp;
            mapCounterFilter.get(ICMP_FILTER).setWinThres(winThresIcmp);
        }
        if (newWinThresIcmp6 != winThresIcmp6) {
            winThresIcmp6 = newWinThresIcmp6;
            mapCounterFilter.get(ICMP6_FILTER).setWinThres(winThresIcmp6);
        }

    }

    @Modified
    private void modified(ComponentContext context) {
        if (context == null) {
            log.info("Default config");
            return;
        }

        checkChangeInPps(context);
        checkChangeInWinSize(context);
        checkChangeInGuardTime(context);
        checkChangeInWinThres(context);

        log.info("Reconfigured ppsArp: {} ppsDhcp: {} ppsNs: {} ppsNa: {} " +
                "ppsDhcp6Direct: {} ppsDhcp6Indirect: {} ppsIcmp: {} ppsIcmp6: {}",
                 ppsArp, ppsDhcp, ppsNs, ppsNa, ppsDhcp6Direct, ppsDhcp6Indirect,
                 ppsIcmp, ppsIcmp6);

        log.info("Reconfigured winSizeArp: {} winSizeDhcp: {} winSizeNs: {} winSizeNa: {} " +
                 "winSizeDhcp6Direct: {} winSizeDhcp6Indirect: {} winSizeIcmp: {} winSizeIcmp6: {}",
                 winSizeArp, winSizeDhcp, winSizeNs, winSizeNa, winSizeDhcp6Direct,
                 winSizeDhcp6Indirect, winSizeIcmp, winSizeIcmp6);

        log.info("Reconfigured guardTimeArp: {} guardTimeDhcp: {} guardTimeNs: {} guardTimeNa: {} " +
                 "guardTimeDhcp6Direct: {} guardTimeDhcp6Indirect: {} guardTimeIcmp: {} guardTimeIcmp6: {}",
                 guardTimeArp, guardTimeDhcp, guardTimeNs, guardTimeNa, guardTimeDhcp6Direct,
                 guardTimeDhcp6Indirect, guardTimeIcmp, guardTimeIcmp6);

        log.info("Reconfigured winThresArp: {} winThresDhcp: {} winThresNs: {} winThresNa: {} " +
                 "winThresDhcp6Direct: {} winThresDhcp6Indirect: {} winThresIcmp: {} winThresIcmp6: {}",
                 winThresArp, winThresDhcp, winThresNs, winThresNa, winThresDhcp6Direct,
                 winThresDhcp6Indirect, winThresIcmp, winThresIcmp6);
    }

    /**
     * Create all required filters.
     */
    private void createAllFilters() {
        DefaultPacketInFilter filter;
        ArpPacketClassifier arp = new ArpPacketClassifier();
        filter = new DefaultPacketInFilter(ppsArp, winSizeArp, guardTimeArp, winThresArp, ARP_FILTER, arp);
        packetService.addFilter(filter);
        mapCounterFilter.put(filter.name(), filter);
        DhcpPacketClassifier dhcp4 = new DhcpPacketClassifier();
        filter = new DefaultPacketInFilter(ppsDhcp, winSizeDhcp, guardTimeDhcp, winThresDhcp, DHCP_FILTER, dhcp4);
        packetService.addFilter(filter);
        mapCounterFilter.put(filter.name(), filter);
        Dhcp6DirectPacketClassifier dhcp6Direct = new Dhcp6DirectPacketClassifier();
        filter = new DefaultPacketInFilter(ppsDhcp6Direct, winSizeDhcp6Direct, guardTimeDhcp6Direct,
                                           winThresDhcp6Direct, DHCP6_DIRECT_FILTER, dhcp6Direct);
        packetService.addFilter(filter);
        mapCounterFilter.put(filter.name(), filter);
        Dhcp6IndirectPacketClassifier dhcp6Indirect = new Dhcp6IndirectPacketClassifier();
        filter = new DefaultPacketInFilter(ppsDhcp6Direct, winSizeDhcp6Direct, guardTimeDhcp6Direct,
                                           winThresDhcp6Direct, DHCP6_INDIRECT_FILTER, dhcp6Indirect);
        packetService.addFilter(filter);
        mapCounterFilter.put(filter.name(), filter);
        NAPacketClassifier na = new NAPacketClassifier();
        filter = new DefaultPacketInFilter(ppsNa, winSizeNa, guardTimeNa, winThresNa, NA_FILTER, na);
        packetService.addFilter(filter);
        mapCounterFilter.put(filter.name(), filter);
        NSPacketClassifier ns = new NSPacketClassifier();
        filter = new DefaultPacketInFilter(ppsNs, winSizeNs, guardTimeNs, winThresNs, NS_FILTER, ns);
        packetService.addFilter(filter);
        mapCounterFilter.put(filter.name(), filter);
        IcmpPacketClassifier icmp = new IcmpPacketClassifier();
        filter = new DefaultPacketInFilter(ppsIcmp, winSizeIcmp, guardTimeIcmp, winThresIcmp, ICMP_FILTER, icmp);
        packetService.addFilter(filter);
        mapCounterFilter.put(filter.name(), filter);
        Icmp6PacketClassifier icmp6 = new Icmp6PacketClassifier();
        filter = new DefaultPacketInFilter(ppsIcmp6, winSizeIcmp6, guardTimeIcmp6, winThresIcmp6, ICMP6_FILTER, icmp6);
        packetService.addFilter(filter);
        mapCounterFilter.put(filter.name(), filter);
    }

    /**
     * Delete all the filters.
     */
    private void removeAllFilters() {
        packetService.clearFilters();
        mapCounterFilter.clear();
    }

    @Override
    public Map<String, PacketInFilter> filterMap() {
        return ImmutableMap.copyOf(mapCounterFilter);
    }



}
