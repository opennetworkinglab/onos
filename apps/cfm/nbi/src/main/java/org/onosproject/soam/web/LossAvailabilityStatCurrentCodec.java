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
package org.onosproject.soam.web;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.incubator.net.l2monitoring.soam.loss.LossAvailabilityStatCurrent;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Encode and decode to/from JSON to LossAvailabilityStatCurrent object.
 */
public class LossAvailabilityStatCurrentCodec extends JsonCodec<LossAvailabilityStatCurrent> {

    @Override
    public ObjectNode encode(LossAvailabilityStatCurrent laCurrent, CodecContext context) {
        checkNotNull(laCurrent, "LA current cannot be null");
        ObjectNode result = context.mapper().createObjectNode()
                .put("startTime", laCurrent.startTime().toString())
                .put("elapsedTime", laCurrent.elapsedTime().toString());

        ObjectNode resultAbstract = new LossAvailabilityStatCodec().encode(laCurrent, context);
        result.setAll(resultAbstract);

        return result;
    }
}
