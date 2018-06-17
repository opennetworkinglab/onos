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

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.codec.CodecService;
import org.onosproject.openstacktelemetry.api.FlowInfo;
import org.onosproject.openstacktelemetry.api.StatsFlowRule;
import org.onosproject.openstacktelemetry.api.StatsInfo;
import org.onosproject.openstacktelemetry.codec.FlowInfoJsonCodec;
import org.onosproject.openstacktelemetry.codec.StatsFlowRuleJsonCodec;
import org.onosproject.openstacktelemetry.codec.StatsInfoJsonCodec;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of the JSON codec brokering service for OpenstackTelemetry.
 */
@Component(immediate = true)
public class OpenstackTelemetryCodecRegister {

    private final org.slf4j.Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CodecService codecService;

    @Activate
    protected void activate() {
        codecService.registerCodec(StatsInfo.class, new StatsInfoJsonCodec());
        codecService.registerCodec(FlowInfo.class, new FlowInfoJsonCodec());
        codecService.registerCodec(StatsFlowRule.class, new StatsFlowRuleJsonCodec());

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        codecService.unregisterCodec(StatsInfo.class);
        codecService.unregisterCodec(FlowInfo.class);
        codecService.unregisterCodec(StatsFlowRule.class);

        log.info("Stopped");
    }
}
