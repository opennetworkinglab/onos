/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.openstacktelemetry.impl;

import org.onosproject.openstacktelemetry.api.FlowInfo;
import org.onosproject.openstacktelemetry.api.OpenstackTelemetryService;
import org.onosproject.openstacktelemetry.api.PrometheusTelemetryAdminService;
import org.onosproject.openstacktelemetry.api.PrometheusTelemetryService;
import org.onosproject.openstacktelemetry.api.config.PrometheusTelemetryConfig;
import org.onosproject.openstacktelemetry.api.config.TelemetryConfig;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.prometheus.client.Counter;
import io.prometheus.client.exporter.MetricsServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import java.util.Set;

/**
 * Prometheus telemetry manager.
 */
@Component(immediate = true, service = PrometheusTelemetryService.class)
public class PrometheusTelemetryManager implements PrometheusTelemetryAdminService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private Server prometheusExporter;

    private static final String BYTE_VM2VM = "byte_vm2vm";
    private static final String BYTE_DEVICE = "byte_device";
    private static final String BYTE_SRC_IP = "byte_src_ip";
    private static final String BYTE_DST_IP = "byte_dst_ip";

    private static final String PKT_VM2VM = "pkt_vm2vm";
    private static final String PKT_DEVICE = "pkt_device";
    private static final String PKT_SRC_IP = "pkt_src_ip";
    private static final String PKT_DST_IP = "pkt_dst_ip";

    private static final String PKT_ERROR = "pkt_error";
    private static final String PKT_DROP = "pkt_drop";

    private static final String LABEL_IP_5_TUPLE = "IP_5_TUPLE";
    private static final String LABEL_DEV_ID = "DEVICE_ID";
    private static final String LABEL_SRC_IP = "SOURCE_IP";
    private static final String LABEL_DST_IP = "DESTINATION_IP";

    private static final String HELP_MSG = "SONA Flow statistics";

    private static Counter byteVM2VM = Counter.build().name(BYTE_VM2VM)
                                    .help(HELP_MSG)
                                    .labelNames(LABEL_IP_5_TUPLE).register();

    private static Counter byteDevice = Counter.build().name(BYTE_DEVICE)
                                    .help(HELP_MSG)
                                    .labelNames(LABEL_DEV_ID).register();

    private static Counter byteSrcIp = Counter.build().name(BYTE_SRC_IP)
                                    .help(HELP_MSG)
                                    .labelNames(LABEL_SRC_IP).register();

    private static Counter byteDstIp = Counter.build().name(BYTE_DST_IP)
                                    .help(HELP_MSG)
                                    .labelNames(LABEL_DST_IP).register();

    private static Counter pktVM2VM = Counter.build().name(PKT_VM2VM)
                                    .help(HELP_MSG)
                                    .labelNames(LABEL_IP_5_TUPLE).register();

    private static Counter pktDevice = Counter.build().name(PKT_DEVICE)
                                    .help(HELP_MSG)
                                    .labelNames(LABEL_DEV_ID).register();

    private static Counter pktSrcIp = Counter.build().name(PKT_SRC_IP)
                                    .help(HELP_MSG)
                                    .labelNames(LABEL_SRC_IP).register();

    private static Counter pktDstIp = Counter.build().name(PKT_DST_IP)
                                    .help(HELP_MSG)
                                    .labelNames(LABEL_DST_IP).register();

    private static Counter pktError = Counter.build().name(PKT_ERROR)
                                    .help(HELP_MSG)
                                    .register();
    private static Counter pktDrop = Counter.build().name(PKT_DROP)
                                    .help(HELP_MSG)
                                    .register();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OpenstackTelemetryService openstackTelemetryService;

    @Activate
    protected void activate() {
        openstackTelemetryService.addTelemetryService(this);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        stop();
        openstackTelemetryService.removeTelemetryService(this);
        log.info("Stopped");
    }

    @Override
    public void start(TelemetryConfig config) {
        log.info("Prometheus exporter starts.");

        PrometheusTelemetryConfig prometheusConfig = (PrometheusTelemetryConfig) config;

        try {
            // TODO  Offer a 'Authentication'
            prometheusExporter = new Server(prometheusConfig.port());
            ServletContextHandler context = new ServletContextHandler();
            context.setContextPath("/");
            prometheusExporter.setHandler(context);
            context.addServlet(new ServletHolder(new MetricsServlet()), "/metrics");

            log.info("Prometeus server start");

            prometheusExporter.start();
        } catch (Exception ex) {
            log.warn("Exception: {}", ex.toString());
        }
    }

    @Override
    public void stop() {
        try {
            prometheusExporter.stop();
        } catch (Exception ex) {
            log.warn("Exception: {}", ex.toString());
        }
        log.info("Prometheus exporter has stopped");
    }

    @Override
    public void restart(TelemetryConfig config) {
        stop();
        start(config);
    }

    @Override
    public void publish(Set<FlowInfo> flowInfos) {
        if (flowInfos.size() == 0) {
            log.debug("No record to publish");
            return;
        }

        long flowByte;
        int flowPkt;
        for (FlowInfo flowInfo: flowInfos) {
            flowByte = flowInfo.statsInfo().currAccBytes() - flowInfo.statsInfo().prevAccBytes();
            flowPkt = flowInfo.statsInfo().currAccPkts() - flowInfo.statsInfo().prevAccPkts();

            byteVM2VM.labels(flowInfo.uniqueFlowInfoKey()).inc(flowByte);
            byteDevice.labels(flowInfo.deviceId().toString()).inc(flowByte);
            byteSrcIp.labels(flowInfo.srcIp().toString()).inc(flowByte);
            byteDstIp.labels(flowInfo.dstIp().toString()).inc(flowByte);

            pktVM2VM.labels(flowInfo.uniqueFlowInfoKey()).inc(flowPkt);
            pktDevice.labels(flowInfo.deviceId().toString()).inc(flowPkt);
            pktSrcIp.labels(flowInfo.srcIp().toString()).inc(flowPkt);
            pktDstIp.labels(flowInfo.dstIp().toString()).inc(flowPkt);

            pktError.inc(flowInfo.statsInfo().errorPkts());
            pktDrop.inc(flowInfo.statsInfo().dropPkts());
        }
    }

    @Override
    public boolean isRunning() {
        log.info("Prometheus Exporter State: {}", prometheusExporter.isRunning());
        return prometheusExporter.isRunning();
    }
}
