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

import com.google.common.collect.Maps;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.MetricsServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.onlab.packet.TpPort;
import org.onosproject.openstacktelemetry.api.FlowInfo;
import org.onosproject.openstacktelemetry.api.OpenstackTelemetryService;
import org.onosproject.openstacktelemetry.api.PrometheusTelemetryAdminService;
import org.onosproject.openstacktelemetry.api.TelemetryConfigService;
import org.onosproject.openstacktelemetry.api.config.PrometheusTelemetryConfig;
import org.onosproject.openstacktelemetry.api.config.TelemetryConfig;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import static org.onosproject.openstacktelemetry.api.Constants.PROMETHEUS_SCHEME;
import static org.onosproject.openstacktelemetry.api.config.TelemetryConfig.ConfigType.PROMETHEUS;
import static org.onosproject.openstacktelemetry.api.config.TelemetryConfig.Status.ENABLED;
import static org.onosproject.openstacktelemetry.config.DefaultPrometheusTelemetryConfig.fromTelemetryConfig;

/**
 * Prometheus telemetry manager.
 */
@Component(immediate = true, service = PrometheusTelemetryAdminService.class)
public class PrometheusTelemetryManager implements PrometheusTelemetryAdminService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private Map<String, Server> prometheusExporters = Maps.newConcurrentMap();

    private static final String FLOW_TYPE = "flowType";
    private static final String DEVICE_ID = "deviceId";
    private static final String INPUT_INTERFACE_ID = "inputInterfaceId";
    private static final String OUTPUT_INTERFACE_ID = "outputInterfaceId";
    private static final String VLAN_ID = "vlanId";
    private static final String VXLAN_ID = "vxlanId";
    private static final String SRC_IP = "srcIp";
    private static final String DST_IP = "dstIp";
    private static final String SRC_PORT = "srcPort";
    private static final String DST_PORT = "dstPort";
    private static final String PROTOCOL = "protocol";

    private static final String[] LABEL_TAGS = {
            FLOW_TYPE, DEVICE_ID, INPUT_INTERFACE_ID, OUTPUT_INTERFACE_ID,
            VLAN_ID, VXLAN_ID, SRC_IP, DST_IP, SRC_PORT, DST_PORT, PROTOCOL
    };

    private static final String STAT_NAME_VM2VM_BYTE = "vm2vm_byte";
    private static final String STAT_NAME_VM2VM_BYTE_PREV = "vm2vm_byte_prev";
    private static final String STAT_NAME_VM2VM_BYTE_CURR = "vm2vm_byte_curr";
    private static final String STAT_NAME_VM2VM_PKT = "vm2vm_pkt";
    private static final String STAT_NAME_VM2VM_PKT_PREV = "vm2vm_pkt_prev";
    private static final String STAT_NAME_VM2VM_PKT_CURR = "vm2vm_pkt_curr";
    private static final String STAT_NAME_ERROR_PKT = "error_pkt";
    private static final String STAT_NAME_DROP_PKT = "drop_pkt";

    private static final String HELP_MSG_VM2VM_BYTE =
                                    "SONA flow bytes statistics for VM to VM";
    private static final String HELP_MSG_VM2VM_BYTE_PREV =
                                    HELP_MSG_VM2VM_BYTE + " [Accumulated previous byte]";
    private static final String HELP_MSG_VM2VM_BYTE_CURR =
                                    HELP_MSG_VM2VM_BYTE + " [Accumulated current byte]";
    private static final String HELP_MSG_VM2VM_PKT =
                                    "SONA flow packets statistics for VM to VM";
    private static final String HELP_MSG_VM2VM_PKT_PREV =
                                    HELP_MSG_VM2VM_PKT + " [Accumulated previous pkt]";
    private static final String HELP_MSG_VM2VM_PKT_CURR =
                                    HELP_MSG_VM2VM_PKT + " [Accumulated current pkt]";
    private static final String HELP_MSG_ERROR = "SONA error statistics";
    private static final String HELP_MSG_DROP = "SONA drop statistics";

    private static Gauge byteVM2VM = Gauge.build()
                                            .name(STAT_NAME_VM2VM_BYTE)
                                            .help(HELP_MSG_VM2VM_BYTE)
                                            .labelNames(LABEL_TAGS)
                                            .register();
    private static Gauge byteVM2VMPrev = Gauge.build()
                                            .name(STAT_NAME_VM2VM_BYTE_PREV)
                                            .help(HELP_MSG_VM2VM_BYTE_PREV)
                                            .labelNames(LABEL_TAGS)
                                            .register();
    private static Gauge byteVM2VMCurr = Gauge.build()
                                            .name(STAT_NAME_VM2VM_BYTE_CURR)
                                            .help(HELP_MSG_VM2VM_BYTE_CURR)
                                            .labelNames(LABEL_TAGS)
                                            .register();
    private static Gauge pktVM2VM = Gauge.build()
                                            .name(STAT_NAME_VM2VM_PKT)
                                            .help(HELP_MSG_VM2VM_PKT)
                                            .labelNames(LABEL_TAGS)
                                            .register();
    private static Gauge pktVM2VMPrev = Gauge.build()
                                            .name(STAT_NAME_VM2VM_PKT_PREV)
                                            .help(HELP_MSG_VM2VM_PKT_PREV)
                                            .labelNames(LABEL_TAGS)
                                            .register();
    private static Gauge pktVM2VMCurr = Gauge.build()
                                            .name(STAT_NAME_VM2VM_PKT_CURR)
                                            .help(HELP_MSG_VM2VM_PKT_CURR)
                                            .labelNames(LABEL_TAGS)
                                            .register();
    private static Counter pktError = Counter.build()
                                            .name(STAT_NAME_ERROR_PKT)
                                            .help(HELP_MSG_ERROR)
                                            .labelNames(LABEL_TAGS)
                                            .register();
    private static Counter pktDrop = Counter.build()
                                            .name(STAT_NAME_DROP_PKT)
                                            .help(HELP_MSG_DROP)
                                            .labelNames(LABEL_TAGS)
                                            .register();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OpenstackTelemetryService openstackTelemetryService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected TelemetryConfigService telemetryConfigService;

    @Activate
    protected void activate() {
        openstackTelemetryService.addTelemetryService(this);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        stopAll();
        openstackTelemetryService.removeTelemetryService(this);
        log.info("Stopped");
    }

    @Override
    public void startAll() {
        log.info("Prometheus exporter starts.");

        telemetryConfigService.getConfigsByType(PROMETHEUS).forEach(c -> start(c.name()));
    }

    @Override
    public void stopAll() {
        prometheusExporters.values().forEach(pe -> {
            try {
                pe.stop();
            } catch (Exception e) {
                log.warn("Failed to stop prometheus server due to {}", e);
            }
        });
        log.info("Prometheus exporter has stopped");
    }

    @Override
    public void restartAll() {
        stopAll();
        startAll();
    }

    @Override
    public void publish(Set<FlowInfo> flowInfos) {
        if (prometheusExporters == null || prometheusExporters.isEmpty()) {
            log.debug("Prometheus telemetry service has not been enabled!");
            return;
        }

        if (flowInfos.size() == 0) {
            log.debug("No record to publish");
            return;
        }

        log.debug("Publish {} stats records to Prometheus", flowInfos.size());

        long flowByte;
        int flowPkt;
        String[] labelValues;

        for (FlowInfo flowInfo: flowInfos) {
            flowByte = flowInfo.statsInfo().currAccBytes() - flowInfo.statsInfo().prevAccBytes();
            flowPkt = flowInfo.statsInfo().currAccPkts() - flowInfo.statsInfo().prevAccPkts();
            labelValues = getLabelValues(flowInfo);
            byteVM2VM.labels(labelValues).set(flowByte);
            byteVM2VMPrev.labels(labelValues).set(flowInfo.statsInfo().prevAccBytes());
            byteVM2VMCurr.labels(labelValues).set(flowInfo.statsInfo().currAccBytes());
            pktVM2VM.labels(labelValues).set(flowPkt);
            pktVM2VMPrev.labels(labelValues).set(flowInfo.statsInfo().prevAccPkts());
            pktVM2VMCurr.labels(labelValues).set(flowInfo.statsInfo().currAccPkts());

            if (flowInfo.statsInfo().errorPkts() == -1) {
                pktError.labels(labelValues).inc(0);
            } else {
                pktError.labels(labelValues).inc(flowInfo.statsInfo().errorPkts());
            }

            if (flowInfo.statsInfo().dropPkts() == -1) {
                pktDrop.labels(labelValues).inc(0);
            } else {
                pktDrop.labels(labelValues).inc(flowInfo.statsInfo().dropPkts());
            }
        }
    }

    @Override
    public boolean isRunning() {
        return !prometheusExporters.isEmpty();
    }

    @Override
    public boolean start(String name) {
        boolean success = false;
        TelemetryConfig config = telemetryConfigService.getConfig(name);
        PrometheusTelemetryConfig prometheusConfig = fromTelemetryConfig(config);

        if (prometheusConfig != null && !config.name().equals(PROMETHEUS_SCHEME) &&
                config.status() == ENABLED) {
            try {
                // TODO  Offer a 'Authentication'
                Server prometheusExporter = new Server(prometheusConfig.port());
                ServletContextHandler context = new ServletContextHandler();
                context.setContextPath("/");
                prometheusExporter.setHandler(context);
                context.addServlet(new ServletHolder(new MetricsServlet()), "/metrics");

                log.info("Prometheus server start");

                prometheusExporter.start();

                prometheusExporters.put(name, prometheusExporter);

                success = true;

            } catch (Exception ex) {
                log.warn("Failed to start prometheus server due to {}", ex);
            }
        }

        return success;
    }

    @Override
    public void stop(String name) {
        try {
            Server pe = prometheusExporters.get(name);
            if (pe != null) {
                pe.stop();
                prometheusExporters.remove(name);
            }
        } catch (Exception e) {
            log.warn("Failed to stop prometheus server due to {}", e);
        }
    }

    @Override
    public boolean restart(String name) {
        stop(name);
        return start(name);
    }

    private String[] getLabelValues(FlowInfo flowInfo) {
        String[] labelValues = new String[LABEL_TAGS.length];

        labelValues[Arrays.asList(LABEL_TAGS).indexOf(FLOW_TYPE)]
                = String.valueOf(flowInfo.flowType());
        labelValues[Arrays.asList(LABEL_TAGS).indexOf(DEVICE_ID)]
                = flowInfo.deviceId().toString();
        labelValues[Arrays.asList(LABEL_TAGS).indexOf(INPUT_INTERFACE_ID)]
                = String.valueOf(flowInfo.inputInterfaceId());
        labelValues[Arrays.asList(LABEL_TAGS).indexOf(OUTPUT_INTERFACE_ID)]
                = String.valueOf(flowInfo.outputInterfaceId());
        labelValues[Arrays.asList(LABEL_TAGS).indexOf(VXLAN_ID)]
                = String.valueOf(flowInfo.vxlanId());
        labelValues[Arrays.asList(LABEL_TAGS).indexOf(SRC_IP)]
                = flowInfo.srcIp().toString();
        labelValues[Arrays.asList(LABEL_TAGS).indexOf(DST_IP)]
                = flowInfo.dstIp().toString();
        labelValues[Arrays.asList(LABEL_TAGS).indexOf(SRC_PORT)]
                = getTpPort(flowInfo.srcPort());
        labelValues[Arrays.asList(LABEL_TAGS).indexOf(DST_PORT)]
                = getTpPort(flowInfo.dstPort());
        labelValues[Arrays.asList(LABEL_TAGS).indexOf(PROTOCOL)]
                = String.valueOf(flowInfo.protocol());
        if (flowInfo.vlanId() != null) {
            labelValues[Arrays.asList(LABEL_TAGS).indexOf(VLAN_ID)]
                    = flowInfo.vlanId().toString();
        }
        return labelValues;
    }

    private String getTpPort(TpPort tpPort) {
        if (tpPort == null) {
            return "";
        }
        return tpPort.toString();
    }
}
