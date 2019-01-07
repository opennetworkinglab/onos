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
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.onlab.packet.TpPort;
import org.onosproject.openstacktelemetry.api.FlowInfo;
import org.onosproject.openstacktelemetry.api.InfluxDbTelemetryAdminService;
import org.onosproject.openstacktelemetry.api.InfluxRecord;
import org.onosproject.openstacktelemetry.api.OpenstackTelemetryService;
import org.onosproject.openstacktelemetry.api.TelemetryConfigService;
import org.onosproject.openstacktelemetry.api.config.InfluxDbTelemetryConfig;
import org.onosproject.openstacktelemetry.api.config.TelemetryConfig;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

import static org.onosproject.openstacktelemetry.api.Constants.INFLUXDB_SCHEME;
import static org.onosproject.openstacktelemetry.api.config.TelemetryConfig.ConfigType.INFLUXDB;
import static org.onosproject.openstacktelemetry.api.config.TelemetryConfig.Status.ENABLED;
import static org.onosproject.openstacktelemetry.config.DefaultInfluxDbTelemetryConfig.fromTelemetryConfig;
import static org.onosproject.openstacktelemetry.util.OpenstackTelemetryUtil.testConnectivity;

/**
 * InfluxDB telemetry manager.
 */
@Component(immediate = true, service = InfluxDbTelemetryAdminService.class)
public class InfluxDbTelemetryManager implements InfluxDbTelemetryAdminService {

    private final Logger log = LoggerFactory.getLogger(getClass());

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

    private static final String STARTUP_TIME = "startupTime";
    private static final String FST_PKT_ARR_TIME = "fstPktArrTime";
    private static final String LST_PKT_OFFSET = "lstPktOffset";
    private static final String PREV_ACC_BYTES = "prevAccBytes";
    private static final String PREV_ACC_PKTS = "prevAccPkts";
    private static final String CURR_ACC_BYTES = "currAccBytes";
    private static final String CURR_ACC_PKTS = "currAccPkts";
    private static final String ERROR_PKTS = "errorPkts";
    private static final String DROP_PKTS = "dropPkts";

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OpenstackTelemetryService openstackTelemetryService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected TelemetryConfigService telemetryConfigService;

    private static final String INFLUX_PROTOCOL = "http";
    private Map<String, InfluxDB> producers = Maps.newConcurrentMap();

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
    public void publish(InfluxRecord<String, Set<FlowInfo>> record) {
        if (producers == null || producers.isEmpty()) {
            log.debug("InfluxDB telemetry service has not been enabled!");
            return;
        }

        if (record.flowInfos().size() == 0) {
            log.debug("No record to publish");
            return;
        }

        log.debug("Publish {} stats records to InfluxDB", record.flowInfos().size());

        producers.forEach((k, v) -> {
            TelemetryConfig config = telemetryConfigService.getConfig(k);
            InfluxDbTelemetryConfig influxDbConfig = fromTelemetryConfig(config);

            String database = influxDbConfig.database();
            String measurement = influxDbConfig.measurement();

            BatchPoints batchPoints = BatchPoints.database(database).build();

            for (FlowInfo flowInfo: record.flowInfos()) {
                Point.Builder pointBuilder = Point
                        .measurement((measurement == null) ? record.measurement() : measurement)
                        .tag(FLOW_TYPE, String.valueOf(flowInfo.flowType()))
                        .tag(DEVICE_ID, flowInfo.deviceId().toString())
                        .tag(INPUT_INTERFACE_ID, String.valueOf(flowInfo.inputInterfaceId()))
                        .tag(OUTPUT_INTERFACE_ID, String.valueOf(flowInfo.outputInterfaceId()))
                        .tag(VXLAN_ID, String.valueOf(flowInfo.vxlanId()))
                        .tag(SRC_IP, flowInfo.srcIp().toString())
                        .tag(DST_IP, flowInfo.dstIp().toString())
                        .tag(DST_PORT, getTpPort(flowInfo.dstPort()))
                        .tag(PROTOCOL, String.valueOf(flowInfo.protocol()))
                        .addField(STARTUP_TIME, flowInfo.statsInfo().startupTime())
                        .addField(FST_PKT_ARR_TIME, flowInfo.statsInfo().fstPktArrTime())
                        .addField(LST_PKT_OFFSET, flowInfo.statsInfo().lstPktOffset())
                        .addField(PREV_ACC_BYTES, flowInfo.statsInfo().prevAccBytes())
                        .addField(PREV_ACC_PKTS, flowInfo.statsInfo().prevAccPkts())
                        .addField(CURR_ACC_BYTES, flowInfo.statsInfo().currAccBytes())
                        .addField(CURR_ACC_PKTS, flowInfo.statsInfo().currAccPkts())
                        .addField(ERROR_PKTS, flowInfo.statsInfo().errorPkts())
                        .addField(DROP_PKTS, flowInfo.statsInfo().dropPkts());

                if (flowInfo.vlanId() != null) {
                    pointBuilder.tag(VLAN_ID, flowInfo.vlanId().toString());
                }

                if (flowInfo.srcPort() != null) {
                    pointBuilder.tag(SRC_PORT, getTpPort(flowInfo.srcPort()));
                }

                if (flowInfo.dstPort() != null) {
                    pointBuilder.tag(DST_PORT, getTpPort(flowInfo.dstPort()));
                }

                batchPoints.point(pointBuilder.build());
            }
            v.write(batchPoints);
        });
    }

    @Override
    public boolean isRunning() {
        return !producers.isEmpty();
    }

    @Override
    public boolean start(String name) {
        boolean success = false;
        TelemetryConfig config = telemetryConfigService.getConfig(name);
        InfluxDbTelemetryConfig influxDbConfig = fromTelemetryConfig(config);

        if (influxDbConfig != null && !config.name().equals(INFLUXDB_SCHEME) &&
                config.status() == ENABLED) {
            StringBuilder influxDbServerBuilder = new StringBuilder();
            influxDbServerBuilder.append(INFLUX_PROTOCOL);
            influxDbServerBuilder.append(":");
            influxDbServerBuilder.append("//");
            influxDbServerBuilder.append(influxDbConfig.address());
            influxDbServerBuilder.append(":");
            influxDbServerBuilder.append(influxDbConfig.port());

            if (testConnectivity(influxDbConfig.address(), influxDbConfig.port())) {
                InfluxDB producer = InfluxDBFactory.connect(influxDbServerBuilder.toString(),
                        influxDbConfig.username(), influxDbConfig.password());
                producers.put(name, producer);
                createDB(producer, influxDbConfig.database());
                success = true;
            } else {
                log.warn("Unable to connect to {}:{}, " +
                                "please check the connectivity manually",
                                influxDbConfig.address(), influxDbConfig.port());
            }
        }

        return success;
    }

    @Override
    public void stop(String name) {
        InfluxDB producer = producers.get(name);

        if (producer != null) {
            producer.close();
            producers.remove(name);
        }
    }

    @Override
    public boolean restart(String name) {
        stop(name);
        return start(name);
    }

    @Override
    public void startAll() {
        telemetryConfigService.getConfigsByType(INFLUXDB).forEach(c -> start(c.name()));
        log.info("InfluxDB producer has Started");
    }

    @Override
    public void stopAll() {
        if (producers != null) {
            producers.values().forEach(InfluxDB::close);
        }

        log.info("InfluxDB producer has stopped");
    }

    @Override
    public void restartAll() {
        stopAll();
        startAll();
    }


    private void createDB(InfluxDB producer, String database) {
        if (producer.databaseExists(database)) {
            log.debug("Database {} is already created", database);
        } else {
            producer.createDatabase(database);
            log.debug("Database {} is created", database);
        }
    }

    private String getTpPort(TpPort tpPort) {
        if (tpPort == null) {
            return "";
        }
        return tpPort.toString();
    }
}
