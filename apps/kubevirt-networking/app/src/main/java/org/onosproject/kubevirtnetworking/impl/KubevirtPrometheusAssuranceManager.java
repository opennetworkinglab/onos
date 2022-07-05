/*
 * Copyright 2022-present Open Networking Foundation
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
package org.onosproject.kubevirtnetworking.impl;

import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.MetricsServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.util.SharedScheduledExecutors;
import org.onosproject.core.CoreService;
import org.onosproject.kubevirtnetworking.api.KubevirtFloatingIp;
import org.onosproject.kubevirtnetworking.api.KubevirtPort;
import org.onosproject.kubevirtnetworking.api.KubevirtPortService;
import org.onosproject.kubevirtnetworking.api.KubevirtPrometheusAssuranceService;
import org.onosproject.kubevirtnetworking.api.KubevirtRouter;
import org.onosproject.kubevirtnetworking.api.KubevirtRouterService;
import org.onosproject.kubevirtnode.api.KubevirtNode;
import org.onosproject.kubevirtnode.api.KubevirtNodeService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.IndexTableId;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.criteria.EthCriterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.onosproject.kubevirtnetworking.api.Constants.GW_ENTRY_TABLE;
import static org.onosproject.kubevirtnetworking.api.Constants.PRIORITY_FLOATING_IP_DOWNSTREAM_RULE;
import static org.onosproject.kubevirtnetworking.api.Constants.PRIORITY_FLOATING_IP_UPSTREAM_RULE;
import static org.onosproject.kubevirtnetworking.api.Constants.PRIORITY_STATEFUL_SNAT_RULE;
import static org.onosproject.kubevirtnode.api.KubevirtNode.Type.GATEWAY;
import static org.onosproject.net.flow.criteria.Criterion.Type.ETH_DST;
import static org.onosproject.net.flow.criteria.Criterion.Type.ETH_SRC;
import static org.onosproject.net.flow.criteria.Criterion.Type.IPV4_DST;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of Kubevirt Prometheus Assurance Service.
 */
@Component(immediate = true, service = KubevirtPrometheusAssuranceService.class)
public class KubevirtPrometheusAssuranceManager implements KubevirtPrometheusAssuranceService {
    private final Logger log = getLogger(getClass());

    private static final String APP_ID = "org.onosproject.kubevirtnetwork";


    private static final String SRC_PORT = "srcPort";

    private static final String FIP_ID = "fipId";
    private static final String FIP_ADDRESS = "fipAddress";
    private static final String MER_NAME = "merName";
    private static final String NETWORK_NAME = "networkName";
    private static final String ROUTER_NAME = "routerName";
    private static final String ROUTER_SNAT_IP = "routerSnatIp";
    private static final String VM_NAME = "vmName";
    private static final String RX_BYTE = "rxByte";
    private static final String TX_BYTE = "txByte";
    private static final String RX_PKTS = "rxPkts";
    private static final String TX_PKTS = "txPkts";


    private static final String DST_PORT = "dstPort";
    private static final String PROTOCOL = "protocol";


    private static final long INITIAL_DELAY = 10L;
    private static final long REFRESH_INTERVAL = 10L;
    private static final TimeUnit TIME_UNIT_SECOND = TimeUnit.SECONDS;
    private static final boolean RECOVER_FROM_FAILURE = true;

    private static final String[] FIP_LABEL_TAGS = {FIP_ID, FIP_ADDRESS, MER_NAME, VM_NAME, NETWORK_NAME};
    private static final String[] SNAT_LABEL_TAGS = {ROUTER_NAME, ROUTER_SNAT_IP, MER_NAME};

    private Server prometheusExporter;
    private StatsCollector collector;
    private ScheduledFuture result;

    private static Gauge byteFIPTx = Gauge.build()
            .name("fip_tx_byte")
            .help("fip_tx_byte")
            .labelNames(FIP_LABEL_TAGS)
            .register();
    private static Gauge pktFIPTx = Gauge.build()
            .name("fip_tx_pkts")
            .help("fip_tx_pkts")
            .labelNames(FIP_LABEL_TAGS)
            .register();
    private static Gauge byteFIPRx = Gauge.build()
            .name("fip_rx_byte")
            .help("fip_rx_byte")
            .labelNames(FIP_LABEL_TAGS)
            .register();
    private static Gauge pktFIPRx = Gauge.build()
            .name("fip_rx_pkts")
            .help("fip_rx_pkts")
            .labelNames(FIP_LABEL_TAGS)
            .register();

    private static Gauge byteSNATTx = Gauge.build()
            .name("snat_tx_byte")
            .help("snat_rx_pkts")
            .labelNames(SNAT_LABEL_TAGS)
            .register();
    private static Gauge pktSNATTx = Gauge.build()
            .name("snat_tx_pkts")
            .help("snat_tx_pkts")
            .labelNames(SNAT_LABEL_TAGS)
            .register();

    private static Gauge byteSNATRx = Gauge.build()
            .name("snat_rx_byte")
            .help("snat_rx_byte")
            .labelNames(SNAT_LABEL_TAGS)
            .register();
    private static Gauge pktSNATRx = Gauge.build()
            .name("snat_rx_pkts")
            .help("snat_rx_pkts")
            .labelNames(SNAT_LABEL_TAGS)
            .register();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtRouterService routerService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtNodeService nodeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtPortService portService;

    @Activate
    protected void activate() {
        startPrometheusExporter();
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        stopPrometheusExporter();
        log.info("Stopped");
    }

    @Override
    public void startPrometheusExporter() {
        try {
            prometheusExporter = new Server(9300);
            ServletContextHandler context = new ServletContextHandler();
            context.setContextPath("/");
            prometheusExporter.setHandler(context);
            context.addServlet(new ServletHolder(new MetricsServlet()), "/metrics");

            prometheusExporter.start();

            collector = new StatsCollector();

            result = SharedScheduledExecutors.getSingleThreadExecutor()
                    .scheduleAtFixedRate(collector, INITIAL_DELAY,
                            REFRESH_INTERVAL, TIME_UNIT_SECOND, RECOVER_FROM_FAILURE);

            log.info("Prometheus server start");
        } catch (Exception ex) {
            log.warn("Failed to start prometheus server due to {}", ex);
        }
    }

    @Override
    public void stopPrometheusExporter() {
        try {
            if (prometheusExporter != null) {
                prometheusExporter.stop();
            }
        } catch (Exception e) {
            log.warn("Failed to stop prometheus server due to {}", e);
        }

        result.cancel(true);
        log.info("Prometheus exporter has stopped");
    }

    private void publish() {
        publishFipMetrics();
        publishSnatMetrics();
    }

    private void publishFipMetrics() {
        if (prometheusExporter == null) {
            log.error("Prometheus Server isn't ready.");
            return;
        }

        nodeService.completeNodes(GATEWAY).forEach(node -> {
            flowRuleService.getFlowEntries(node.intgBridge()).forEach(flowEntry -> {


                if (((IndexTableId) flowEntry.table()).id() == GW_ENTRY_TABLE &&
                        flowEntry.priority() == PRIORITY_FLOATING_IP_UPSTREAM_RULE) {

                    KubevirtFloatingIp floatingIp = floatingIpByUpstreamFlowEntry(flowEntry);
                    if (floatingIp == null || floatingIp.vmName() == null) {
                        return;
                    }

                    String[] fipLabelValues = new String[5];

                    fipLabelValues[0] = floatingIp.id();
                    fipLabelValues[1] = floatingIp.floatingIp().toString();
                    fipLabelValues[2] = node.hostname();
                    fipLabelValues[3] = floatingIp.vmName();
                    fipLabelValues[4] = floatingIp.networkName();

                    pktFIPTx.labels(fipLabelValues).set(flowEntry.packets());
                    byteFIPTx.labels(fipLabelValues).set(flowEntry.bytes());

                } else if (((IndexTableId) flowEntry.table()).id() == GW_ENTRY_TABLE &&
                        flowEntry.priority() == PRIORITY_FLOATING_IP_DOWNSTREAM_RULE) {
                    KubevirtFloatingIp floatingIp = floatingIpByDownstreamFlowEntry(flowEntry);
                    if (floatingIp == null || floatingIp.vmName() == null) {
                        return;
                    }

                    String[] fipLabelValues = new String[5];

                    fipLabelValues[0] = floatingIp.id();
                    fipLabelValues[1] = floatingIp.floatingIp().toString();
                    fipLabelValues[2] = node.hostname();
                    fipLabelValues[3] = floatingIp.vmName();
                    fipLabelValues[4] = floatingIp.networkName();

                    pktFIPRx.labels(fipLabelValues).set(flowEntry.packets());
                    byteFIPRx.labels(fipLabelValues).set(flowEntry.bytes());
                }
            });
        });
    }


    private void publishSnatMetrics() {
        if (prometheusExporter == null) {
            log.error("Prometheus Server isn't ready.");
            return;
        }

        routerService.routers().stream().filter(router -> router.enableSnat() &&
                        router.electedGateway() != null &&
                        router.peerRouter() != null &&
                        router.peerRouter().ipAddress() != null &&
                        router.peerRouter().macAddress() != null)
                .forEach(router -> {
                    KubevirtNode gateway = nodeService.node(router.electedGateway());
                    if (gateway == null) {
                        return;
                    }

                    String routerSnatIp = router.external().keySet().stream().findAny().orElse(null);
                    if (routerSnatIp == null) {
                        return;
                    }



                    flowRuleService.getFlowEntries(gateway.intgBridge()).forEach(flowEntry -> {
                        if (((IndexTableId) flowEntry.table()).id() == GW_ENTRY_TABLE &&
                                flowEntry.priority() == PRIORITY_STATEFUL_SNAT_RULE) {

                            if (isSnatUpstreamFlorEntryForRouter(router, flowEntry)) {
                                String[] snatLabelValues = new String[3];
                                snatLabelValues[0] = router.name();
                                snatLabelValues[1] = routerSnatIp;
                                snatLabelValues[2] = gateway.hostname();
                                pktSNATTx.labels(snatLabelValues).set(flowEntry.packets());
                                byteSNATTx.labels(snatLabelValues).set(flowEntry.bytes());

                            } else if (isSnatDownstreamFlorEntryForRouter(routerSnatIp, flowEntry)) {
                                String[] snatLabelValues = new String[3];
                                snatLabelValues[0] = router.name();
                                snatLabelValues[1] = routerSnatIp;
                                snatLabelValues[2] = gateway.hostname();
                                pktSNATRx.labels(snatLabelValues).set(flowEntry.packets());
                                byteSNATRx.labels(snatLabelValues).set(flowEntry.bytes());
                            }
                        }
                    });
                });
    }

    private boolean isSnatUpstreamFlorEntryForRouter(KubevirtRouter router, FlowEntry flowEntry) {
        TrafficSelector selector = flowEntry.selector();

        EthCriterion ethCriterion = (EthCriterion) selector.getCriterion(ETH_DST);
        if (ethCriterion == null) {
            return false;
        }
        MacAddress macAddress = ethCriterion.mac();

        if (router.mac().equals(macAddress)) {
            return true;
        }

        return false;
    }

    private boolean isSnatDownstreamFlorEntryForRouter(String routerSnatIp,  FlowEntry flowEntry) {
        TrafficSelector selector = flowEntry.selector();

        IPCriterion ipCriterion = (IPCriterion) selector.getCriterion(IPV4_DST);
        if (ipCriterion == null) {
            return false;
        }

        IpAddress dstIp = ipCriterion.ip().address();

        if (dstIp.toString().equals(routerSnatIp)) {
            return true;
        }
        return false;
    }

    private KubevirtFloatingIp floatingIpByUpstreamFlowEntry(FlowEntry flowEntry) {
        TrafficSelector selector = flowEntry.selector();

        EthCriterion ethCriterion = (EthCriterion) selector.getCriterion(ETH_SRC);

        if (ethCriterion == null) {
            return null;
        }
        MacAddress macAddress = ethCriterion.mac();

        KubevirtPort port = portService.port(macAddress);

        if (port == null) {
            return null;
        }

        return routerService.floatingIps()
                .stream()
                .filter(ip -> ip.vmName() != null && ip.vmName().equals(port.vmName()))
                .findAny().orElse(null);
    }

    private KubevirtFloatingIp floatingIpByDownstreamFlowEntry(FlowEntry flowEntry) {
        TrafficSelector selector = flowEntry.selector();

        IPCriterion ipCriterion = (IPCriterion) selector.getCriterion(IPV4_DST);
        if (ipCriterion == null) {
            return null;
        }

        IpAddress dstIp = ipCriterion.ip().address();

        return routerService.floatingIps()
                .stream()
                .filter(ip -> ip.floatingIp().equals(dstIp))
                .findAny().orElse(null);
    }

    private class StatsCollector implements Runnable {
        @Override
        public void run() {
            publish();
        }
    }
}
