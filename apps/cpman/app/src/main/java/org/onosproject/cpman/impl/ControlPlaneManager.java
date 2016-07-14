/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.cpman.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.cpman.ControlMessage;
import org.onosproject.cpman.ControlMetric;
import org.onosproject.cpman.ControlPlaneMonitorService;
import org.onosproject.cpman.MetricValue;
import org.onosproject.cpman.message.ControlMessageEvent;
import org.onosproject.cpman.message.ControlMessageListener;
import org.onosproject.cpman.message.ControlMessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Set;

import static org.onosproject.cpman.message.ControlMessageEvent.Type.STATS_UPDATE;

/**
 * Skeletal control plane management component.
 */
@Component(immediate = true)
public class ControlPlaneManager {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ControlMessageService messageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ControlPlaneMonitorService monitorService;

    private final ControlMessageListener messageListener =
            new InternalControlMessageListener();

    private ApplicationId appId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication("org.onosproject.cpman");
        messageService.addListener(messageListener);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        messageService.removeListener(messageListener);
        log.info("Stopped");
    }

    private class InternalControlMessageListener implements ControlMessageListener {

        @Override
        public void event(ControlMessageEvent event) {
            Set<ControlMessage> controlMessages = event.subject();

            // TODO: this can be changed to switch-case if we have more than
            // one event type
            if (event.type().equals(STATS_UPDATE)) {
                controlMessages.forEach(c ->
                    monitorService.updateMetric(getControlMetric(c), 1,
                            Optional.of(c.deviceId()))
                );
            }
        }
    }

    private ControlMetric getControlMetric(ControlMessage message) {
        MetricValue mv = new MetricValue.Builder()
                            .load(message.load())
                            .rate(message.rate())
                            .count(message.count())
                            .add();
        return new ControlMetric(ControlMessageMetricMapper
                    .lookupControlMetricType(message.type()), mv);
    }
}