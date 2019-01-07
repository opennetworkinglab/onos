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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import org.onosproject.openstacktelemetry.api.FlowInfo;
import org.onosproject.openstacktelemetry.api.GrpcTelemetryService;
import org.onosproject.openstacktelemetry.api.InfluxDbTelemetryService;
import org.onosproject.openstacktelemetry.api.KafkaTelemetryService;
import org.onosproject.openstacktelemetry.api.OpenstackTelemetryService;
import org.onosproject.openstacktelemetry.api.PrometheusTelemetryService;
import org.onosproject.openstacktelemetry.api.RestTelemetryService;
import org.onosproject.openstacktelemetry.api.TelemetryAdminService;
import org.onosproject.openstacktelemetry.api.TelemetryConfigAdminService;
import org.onosproject.openstacktelemetry.api.TelemetryConfigEvent;
import org.onosproject.openstacktelemetry.api.TelemetryConfigListener;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

import static org.onosproject.openstacktelemetry.api.Constants.DEFAULT_INFLUXDB_MEASUREMENT;
import static org.onosproject.openstacktelemetry.api.config.TelemetryConfig.Status.PENDING;

/**
 * Openstack telemetry manager.
 */
@Component(immediate = true, service = OpenstackTelemetryService.class)
public class OpenstackTelemetryManager implements OpenstackTelemetryService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected TelemetryConfigAdminService telemetryConfigService;

    private List<TelemetryAdminService> telemetryServices = Lists.newArrayList();
    private InternalTelemetryConfigListener
                        configListener = new InternalTelemetryConfigListener();

    @Activate
    protected void activate() {
        telemetryConfigService.addListener(configListener);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        telemetryConfigService.removeListener(configListener);

        log.info("Stopped");
    }

    @Override
    public void addTelemetryService(TelemetryAdminService telemetryService) {
        telemetryServices.add(telemetryService);
    }

    @Override
    public void removeTelemetryService(TelemetryAdminService telemetryService) {
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

            if (service instanceof PrometheusTelemetryManager) {
                invokePrometheusPublisher((PrometheusTelemetryService) service, flowInfos);
            }

            if (service instanceof KafkaTelemetryManager) {
                invokeKafkaPublisher((KafkaTelemetryService) service, flowInfos);
            }

            if (service instanceof RestTelemetryManager) {
                invokeRestPublisher((RestTelemetryService) service, flowInfos);
            }

            log.trace("Publishing Flow Infos {}", flowInfos);
        });
    }

    @Override
    public Set<TelemetryAdminService> telemetryServices() {
        return ImmutableSet.copyOf(telemetryServices);
    }

    @Override
    public TelemetryAdminService telemetryService(String type) {
        return telemetryServices.stream()
                .filter(s -> s.type().name().equalsIgnoreCase(type))
                .findFirst()
                .orElse(null);
    }

    private void invokeGrpcPublisher(GrpcTelemetryService service,
                                     Set<FlowInfo> flowInfos) {
        // TODO: need provide implementation
    }

    private void invokeInfluxDbPublisher(InfluxDbTelemetryService service,
                                         Set<FlowInfo> flowInfos) {
        DefaultInfluxRecord<String, Set<FlowInfo>> influxRecord
                = new DefaultInfluxRecord<>(DEFAULT_INFLUXDB_MEASUREMENT, flowInfos);
        service.publish(influxRecord);
    }

    private void invokePrometheusPublisher(PrometheusTelemetryService service,
                                           Set<FlowInfo> flowInfos) {
        service.publish(flowInfos);
    }

    private void invokeKafkaPublisher(KafkaTelemetryService service,
                                      Set<FlowInfo> flowInfos) {
        service.publish(flowInfos);
    }

    private void invokeRestPublisher(RestTelemetryService service,
                                     Set<FlowInfo> flowInfos) {
        // TODO: need provide implementation
    }

    private class InternalTelemetryConfigListener implements TelemetryConfigListener {

        @Override
        public void event(TelemetryConfigEvent event) {
            TelemetryAdminService service =
                    telemetryService(event.subject().type().name());

            switch (event.type()) {
                case SERVICE_ENABLED:
                    if (!service.start(event.subject().name())) {
                        // we enforce to make the service in PENDING status,
                        // if we encountered a failure during service start
                        telemetryConfigService.updateTelemetryConfig(
                                event.subject().updateStatus(PENDING));
                    }
                    break;
                case SERVICE_DISABLED:
                    service.stop(event.subject().name());
                    break;
                default:
                    break;
            }
        }
    }
}
