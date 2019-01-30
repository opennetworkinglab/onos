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
package org.onosproject.openstacktelemetry.web;

import org.onosproject.codec.CodecService;
import org.onosproject.openstacktelemetry.api.FlowInfo;
import org.onosproject.openstacktelemetry.api.StatsFlowRule;
import org.onosproject.openstacktelemetry.api.StatsInfo;
import org.onosproject.openstacktelemetry.api.config.TelemetryConfig;
import org.onosproject.openstacktelemetry.codec.rest.FlowInfoJsonCodec;
import org.onosproject.openstacktelemetry.codec.rest.StatsFlowRuleJsonCodec;
import org.onosproject.openstacktelemetry.codec.rest.StatsInfoJsonCodec;
import org.onosproject.openstacktelemetry.codec.rest.TelemetryConfigJsonCodec;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of the JSON codec brokering service for OpenstackTelemetry.
 */
@Component(immediate = true)
public class OpenstackRestCodecRegister {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CodecService codecService;

    @Activate
    protected void activate() {
        codecService.registerCodec(StatsInfo.class, new StatsInfoJsonCodec());
        codecService.registerCodec(FlowInfo.class, new FlowInfoJsonCodec());
        codecService.registerCodec(StatsFlowRule.class, new StatsFlowRuleJsonCodec());
        codecService.registerCodec(TelemetryConfig.class, new TelemetryConfigJsonCodec());

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        codecService.unregisterCodec(StatsInfo.class);
        codecService.unregisterCodec(FlowInfo.class);
        codecService.unregisterCodec(StatsFlowRule.class);
        codecService.unregisterCodec(TelemetryConfig.class);

        log.info("Stopped");
    }
}
