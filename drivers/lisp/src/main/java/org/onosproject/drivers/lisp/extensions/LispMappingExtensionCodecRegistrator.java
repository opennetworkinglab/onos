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
package org.onosproject.drivers.lisp.extensions;

import org.onosproject.drivers.lisp.extensions.codec.LispAppDataAddressCodec;
import org.onosproject.drivers.lisp.extensions.codec.LispAsAddressCodec;
import org.onosproject.drivers.lisp.extensions.codec.LispGcAddressCodec;
import org.onosproject.drivers.lisp.extensions.codec.LispListAddressCodec;
import org.onosproject.drivers.lisp.extensions.codec.LispMulticastAddressCodec;
import org.onosproject.drivers.lisp.extensions.codec.LispNatAddressCodec;
import org.onosproject.drivers.lisp.extensions.codec.LispNonceAddressCodec;
import org.onosproject.drivers.lisp.extensions.codec.LispSegmentAddressCodec;
import org.onosproject.drivers.lisp.extensions.codec.LispSrcDstAddressCodec;
import org.onosproject.drivers.lisp.extensions.codec.LispTeAddressCodec;
import org.onosproject.drivers.lisp.extensions.codec.LispTeRecordCodec;
import org.onosproject.mapping.MappingCodecRegistrator;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of the JSON codec brokering service for
 * mapping extension primitives.
 */
@Component(immediate = true, service = MappingCodecRegistrator.class)
public class LispMappingExtensionCodecRegistrator extends MappingCodecRegistrator {

    private final Logger log = getLogger(getClass());
    private MappingCodecRegistrator registrator;

    @Activate
    public void activate() {

        registrator = new MappingCodecRegistrator();
        registrator.codecService = codecService;
        registrator.activate();

        codecService.registerCodec(LispAppDataAddress.class, new LispAppDataAddressCodec());
        codecService.registerCodec(LispAsAddress.class, new LispAsAddressCodec());
        codecService.registerCodec(LispGcAddress.class, new LispGcAddressCodec());
        codecService.registerCodec(LispListAddress.class, new LispListAddressCodec());
        codecService.registerCodec(LispMulticastAddress.class, new LispMulticastAddressCodec());
        codecService.registerCodec(LispNatAddress.class, new LispNatAddressCodec());
        codecService.registerCodec(LispNonceAddress.class, new LispNonceAddressCodec());
        codecService.registerCodec(LispSegmentAddress.class, new LispSegmentAddressCodec());
        codecService.registerCodec(LispSrcDstAddress.class, new LispSrcDstAddressCodec());
        codecService.registerCodec(LispTeAddress.class, new LispTeAddressCodec());
        codecService.registerCodec(LispTeAddress.TeRecord.class, new LispTeRecordCodec());

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        codecService.unregisterCodec(LispAppDataAddress.class);
        codecService.unregisterCodec(LispAsAddress.class);
        codecService.unregisterCodec(LispGcAddress.class);
        codecService.unregisterCodec(LispListAddress.class);
        codecService.unregisterCodec(LispMulticastAddress.class);
        codecService.unregisterCodec(LispNatAddress.class);
        codecService.unregisterCodec(LispNonceAddress.class);
        codecService.unregisterCodec(LispSegmentAddress.class);
        codecService.unregisterCodec(LispSrcDstAddress.class);
        codecService.unregisterCodec(LispTeAddress.class);
        codecService.unregisterCodec(LispTeAddress.TeRecord.class);

        registrator.deactivate();
        registrator = null;

        log.info("Stopped");
    }
}
