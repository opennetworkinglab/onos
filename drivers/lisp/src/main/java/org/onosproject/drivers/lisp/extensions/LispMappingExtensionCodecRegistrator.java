/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.drivers.lisp.extensions;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.codec.CodecService;
import org.onosproject.drivers.lisp.extensions.codec.LispAppDataAddressCodec;
import org.onosproject.mapping.web.MappingCodecRegistrator;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of the JSON codec brokering service for
 * mapping extension primitives.
 */
@Component(immediate = true)
public class LispMappingExtensionCodecRegistrator extends MappingCodecRegistrator {

    private final Logger log = getLogger(getClass());
    private MappingCodecRegistrator registrator;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    public CodecService codecService;

    @Activate
    public void activate() {

        registrator = new MappingCodecRegistrator();
        registrator.codecService = codecService;
        registrator.activate();

        codecService.registerCodec(LispAppDataAddress.class, new LispAppDataAddressCodec());

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        codecService.unregisterCodec(LispAppDataAddress.class);

        registrator.deactivate();
        registrator = null;

        log.info("Stopped");
    }
}
