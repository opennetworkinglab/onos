/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.mapping;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.onosproject.codec.CodecService;
import org.onosproject.mapping.actions.MappingAction;
import org.onosproject.mapping.addresses.MappingAddress;
import org.onosproject.mapping.codec.MappingEntryCodec;
import org.onosproject.mapping.instructions.MappingInstruction;
import org.onosproject.mapping.codec.MappingActionCodec;
import org.onosproject.mapping.codec.MappingAddressCodec;
import org.onosproject.mapping.codec.MappingInstructionCodec;
import org.onosproject.mapping.codec.MappingKeyCodec;
import org.onosproject.mapping.codec.MappingTreatmentCodec;
import org.onosproject.mapping.codec.MappingValueCodec;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of the JSON codec brokering service for mapping primitives.
 */
@Component(immediate = true)
public class MappingCodecRegistrator {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    public CodecService codecService;

    @Activate
    public void activate() {
        codecService.registerCodec(MappingAddress.class, new MappingAddressCodec());
        codecService.registerCodec(MappingInstruction.class, new MappingInstructionCodec());
        codecService.registerCodec(MappingAction.class, new MappingActionCodec());
        codecService.registerCodec(MappingTreatment.class, new MappingTreatmentCodec());
        codecService.registerCodec(MappingKey.class, new MappingKeyCodec());
        codecService.registerCodec(MappingValue.class, new MappingValueCodec());
        codecService.registerCodec(MappingEntry.class, new MappingEntryCodec());

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        codecService.unregisterCodec(MappingAddress.class);
        codecService.unregisterCodec(MappingInstruction.class);
        codecService.unregisterCodec(MappingAction.class);
        codecService.unregisterCodec(MappingTreatment.class);
        codecService.unregisterCodec(MappingKey.class);
        codecService.unregisterCodec(MappingValue.class);
        codecService.unregisterCodec(MappingEntry.class);

        log.info("Stopped");
    }
}
