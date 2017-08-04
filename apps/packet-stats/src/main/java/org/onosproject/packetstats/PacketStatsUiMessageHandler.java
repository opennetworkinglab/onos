/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.packetstats;
import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricFilter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import org.onosproject.net.ConnectPoint;
import org.onlab.metrics.MetricsService;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiMessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * ONOS UI Custom-View message handler.
 * <p>
 * This class contains the request handlers that handle the response
 * to each event. In this particular implementation the second message
 * handler creates the patch and the first message handler loads the data.
 */

public class PacketStatsUiMessageHandler extends UiMessageHandler {

    private static final String ARP_REQ = "arpRequest";
    private static final String ARP_RESP = "arpResponse";
    private static final String DHCP_REQ = "dhcpRequest";
    private static final String DHCP_RESP = "dhcpResponse";
    private static final String ICMP_REQ = "icmpRequest";
    private static final String ICMP_RESP = "icmpResponse";
    private static final String LLDP_REQ = "lldpRequest";
    private static final String LLDP_RESP = "lldpResponse";
    private static final String VLAN_REQ = "vlanRequest";
    private static final String VLAN_RESP = "vlanResponse";
    private static final String IGMP_REQ = "igmpRequest";
    private static final String IGMP_RESP = "igmpResponse";
    private static final String PIM_REQ = "pimRequest";
    private static final String PIM_RESP = "pimResponse";
    private static final String BSN_REQ = "bsnRequest";
    private static final String BSN_RESP = "bsnResponse";
    private static final String UNKNOWN_REQ = "unknownRequest";
    private static final String UNKNOWN_RESP = "unknownResponse";
    private static final String MPLS_REQ = "mplsRequest";
    private static final String MPLS_RESP = "mplsResponse";




    private static final String METRIC_NAME = null;
    private static String total = "";

    private List<ConnectPoint> previous = new ArrayList<>();
    private final Logger log = LoggerFactory.getLogger(getClass());
    MetricFilter filter = METRIC_NAME != null ? (name, metric) -> name.equals(METRIC_NAME) : MetricFilter.ALL;

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(
                new ArpRequestHandler(),
                new DhcpRequestHandler(),
                new IcmpRequestHandler(),
                new LldpRequestHandler(),
                new VlanRequestHandler(),
                new IgmpRequestHandler(),
                new PimRequestHandler(),
                new BsnRequestHandler(),
                new UnknownRequestHandler(),
                new MplsRequestHandler()

        );

    }

    //Looking for ARP Packets
    private final class ArpRequestHandler extends RequestHandler {
        private ArpRequestHandler() {
            super(ARP_REQ);
        }

        @Override
        public void process(ObjectNode payload) {
            MetricsService service = get(MetricsService.class);
            Map<String, Counter> counters = service.getCounters(filter);
            Counter arpCounter = counters.get("packetStatisticsComponent.arpFeature.arpPC");
            long arpCount = arpCounter.getCount();
            ObjectNode arpJson = objectNode();
            arpJson.put("ArpCounter", arpCount);
            sendMessage(ARP_RESP, arpJson);
        }
    }

    //Looking for DHCP Packets
    private final class DhcpRequestHandler extends RequestHandler {
        private DhcpRequestHandler() {
            super(DHCP_REQ);
        }

        @Override
        public void process(ObjectNode payload) {
            MetricsService service = get(MetricsService.class);
            Map<String, Counter> counters = service.getCounters(filter);
            Counter dhcpCounter = counters.get("packetStatisticsComponent.dhcpFeature.dhcpPC");
            long dhcpCount = dhcpCounter.getCount();
            ObjectNode dhcpJson = objectNode();
            dhcpJson.put("DhcpCounter", dhcpCount);
            log.info("Received DHCP Request");
            sendMessage(DHCP_RESP, dhcpJson);
        }
    }
    //Looking for ICMP Packets
    private final class IcmpRequestHandler extends RequestHandler {
        private IcmpRequestHandler() {
            super(ICMP_REQ);
        }

        @Override
        public void process(ObjectNode payload) {
            MetricsService service = get(MetricsService.class);
            Map<String, Counter> counters = service.getCounters(filter);
            Counter icmpCounter = counters.get("packetStatisticsComponent.icmpFeature.icmpPC");
            long icmpCount = icmpCounter.getCount();
            ObjectNode icmpJson = objectNode();
            icmpJson.put("IcmpCounter", icmpCount);
            log.info("Received ICMP Request");
            sendMessage(ICMP_RESP, icmpJson);
        }
    }
    //Looking for LLDP Packets
    private final class LldpRequestHandler extends RequestHandler {
        private LldpRequestHandler() {
            super(LLDP_REQ);
        }

        @Override
        public void process(ObjectNode payload) {
            MetricsService service = get(MetricsService.class);
            Map<String, Counter> counters = service.getCounters(filter);
            Counter lldpCounter = counters.get("packetStatisticsComponent.lldpFeature.lldpPC");
            long lldpCount = lldpCounter.getCount();
            ObjectNode lldpJson = objectNode();
            lldpJson.put("LldpCounter", lldpCount);
            log.info("Received LLDP Request");
            sendMessage(LLDP_RESP, lldpJson);
        }
    }
    //Looking for VLAN Packets
    private final class VlanRequestHandler extends RequestHandler {
        private VlanRequestHandler() {
            super(VLAN_REQ);
        }

        @Override
        public void process(ObjectNode payload) {
            MetricsService service = get(MetricsService.class);
            Map<String, Counter> counters = service.getCounters(filter);
            Counter vlanCounter = counters.get("packetStatisticsComponent.vlanFeature.vlanPC");
            long vlanCount = vlanCounter.getCount();
            ObjectNode vlanJson = objectNode();
            vlanJson.put("VlanCounter", vlanCount);
            log.info("Received VLAN Request");
            sendMessage(VLAN_RESP, vlanJson);
        }
    }
    //Looking for IGMP Packets
    private final class IgmpRequestHandler extends RequestHandler {
        private IgmpRequestHandler() {
            super(IGMP_REQ);
        }

        @Override
        public void process(ObjectNode payload) {
            MetricsService service = get(MetricsService.class);
            Map<String, Counter> counters = service.getCounters(filter);
            Counter igmpCounter =  counters.get("packetStatisticsComponent.igmpFeature.igmpPC");
            long igmpCount = igmpCounter.getCount();
            ObjectNode igmpJson = objectNode();
            igmpJson.put("IgmpCounter", igmpCount);
            log.info("Received IGMP Request");
            sendMessage(IGMP_RESP, igmpJson);
        }
    }
    //Looking for PIM Packets
    private final class PimRequestHandler extends RequestHandler {
        private PimRequestHandler() {
            super(PIM_REQ);
        }

        @Override
        public void process(ObjectNode payload) {
            MetricsService service = get(MetricsService.class);
            Map<String, Counter> counters = service.getCounters(filter);
            Counter pimCounter =  counters.get("packetStatisticsComponent.pimFeature.pimPC");
            long pimCount = pimCounter.getCount();
            ObjectNode pimJson = objectNode();
            pimJson.put("PimCounter", pimCount);
            log.info("Received PIM Request");
            sendMessage(PIM_RESP, pimJson);
        }
    }
    //Looking for PIM Packets
    private final class BsnRequestHandler extends RequestHandler {
        private BsnRequestHandler() {
            super(BSN_REQ);
        }

        @Override
        public void process(ObjectNode payload) {
            MetricsService service = get(MetricsService.class);
            Map<String, Counter> counters = service.getCounters(filter);
            Counter bsnCounter =  counters.get("packetStatisticsComponent.bsnFeature.bsnPC");
            long bsnCount = bsnCounter.getCount();
            ObjectNode bsnJson = objectNode();
            bsnJson.put("BsnCounter", bsnCount);
            log.info("Received BSN Request");
            sendMessage(BSN_RESP, bsnJson);
        }
    }
    //Looking for PIM Packets
    private final class UnknownRequestHandler extends RequestHandler {
        private UnknownRequestHandler() {
            super(UNKNOWN_REQ);
        }

        @Override
        public void process(ObjectNode payload) {
            MetricsService service = get(MetricsService.class);
            Map<String, Counter> counters = service.getCounters(filter);
            Counter unknownCounter =  counters.get("packetStatisticsComponent.unknownFeature.unknownPC");
            long unknownCount = unknownCounter.getCount();
            ObjectNode unknownJson = objectNode();
            unknownJson.put("UnknownCounter", unknownCount);
            log.info("Received UNKNOWN Request");
            sendMessage(UNKNOWN_RESP, unknownJson);
        }
    }
    //Looking for PIM Packets
    private final class MplsRequestHandler extends RequestHandler {
        private MplsRequestHandler() {
            super(MPLS_REQ);
        }

        @Override
        public void process(ObjectNode payload) {
            MetricsService service = get(MetricsService.class);
            Map<String, Counter> counters = service.getCounters(filter);
            Counter mplsCounter =  counters.get("packetStatisticsComponent.mplsFeature.mplsPC");
            long mplsCount = mplsCounter.getCount();
            ObjectNode mplsJson = objectNode();
            mplsJson.put("MplsCounter", mplsCount);
            log.info("Received MPLS Request");
            sendMessage(MPLS_RESP, mplsJson);
        }
    }


}