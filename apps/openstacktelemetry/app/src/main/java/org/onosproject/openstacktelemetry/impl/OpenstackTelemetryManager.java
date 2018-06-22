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

import com.google.common.collect.Lists;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.onosproject.openstacktelemetry.api.FlowInfo;
import org.onosproject.openstacktelemetry.api.GrpcTelemetryService;
import org.onosproject.openstacktelemetry.api.InfluxDbTelemetryService;
import org.onosproject.openstacktelemetry.api.KafkaTelemetryService;
import org.onosproject.openstacktelemetry.api.OpenstackTelemetryService;
import org.onosproject.openstacktelemetry.api.RestTelemetryService;
import org.onosproject.openstacktelemetry.api.TelemetryService;
import org.onosproject.openstacktelemetry.codec.TinaMessageByteBufferCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Set;

/**
 * Openstack telemetry manager.
 */
@Component(immediate = true)
@Service
public class OpenstackTelemetryManager implements OpenstackTelemetryService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String KAFKA_TOPIC = "sona.flow";
    private static final String KAFKA_KEY = "flowdata";

    private List<TelemetryService> telemetryServices = Lists.newArrayList();

    @Activate
    protected void activate() {
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        log.info("Stopped");
    }

    @Override
    public void addTelemetryService(TelemetryService telemetryService) {
        telemetryServices.add(telemetryService);
    }

    @Override
    public void removeTelemetryService(TelemetryService telemetryService) {
        telemetryServices.remove(telemetryService);
    }

    @Override
    public void publish(Set<FlowInfo> flowInfos) {
        telemetryServices.forEach(service -> {

            if (service instanceof GrpcTelemetryManager) {
                invokeGrpcPublisher((GrpcTelemetryService) service, flowInfos);
            }

            if (service instanceof InfluxDbTelemetryManager) {
                invokeInfluxDbPublisher((InfluxDbTelemetryService) service, flowInfos);
            }

            if (service instanceof KafkaTelemetryManager) {
                invokeKafkaPublisher((KafkaTelemetryService) service, flowInfos);
            }

            if (service instanceof RestTelemetryManager) {
                invokeRestPublisher((RestTelemetryService) service, flowInfos);
            }
        });
    }

    private void invokeGrpcPublisher(GrpcTelemetryService service, Set<FlowInfo> flowInfos) {
        // TODO: need provide implementation
    }

    private void invokeInfluxDbPublisher(InfluxDbTelemetryService service, Set<FlowInfo> flowInfos) {
        // TODO: need provide implementation
    }

    private void invokeKafkaPublisher(KafkaTelemetryService service, Set<FlowInfo> flowInfos) {
        TinaMessageByteBufferCodec codec = new TinaMessageByteBufferCodec();
        ByteBuffer buffer = codec.encode(flowInfos);
        service.publish(new ProducerRecord<>(KAFKA_TOPIC, KAFKA_KEY, buffer.array()));
    }

    private void invokeRestPublisher(RestTelemetryService service, Set<FlowInfo> flowInfos) {
        // TODO: need provide implementation
    }
}
