/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.openstacktelemetry.codec.json;

import org.onosproject.codec.CodecService;
import org.onosproject.openstacktelemetry.api.LinkInfo;
import org.onosproject.openstacktelemetry.api.LinkStatsInfo;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of the JSON codec brokering service for OpenstackTelemetry.
 */
@Component(immediate = true)
public class OpenstackJsonCodecRegister {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CodecService codecService;

    @Activate
    protected void activate() {
        codecService.registerCodec(LinkInfo.class, new ThreeDVLinkInfoJsonCodec());
        codecService.registerCodec(LinkStatsInfo.class, new ThreeDVLinkStatsInfoJsonCodec());

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        codecService.unregisterCodec(LinkInfo.class);
        codecService.unregisterCodec(LinkStatsInfo.class);

        log.info("Stopped");
    }
}
