/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.cpman.codec;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.cpman.ControlLoadSnapshot;

/**
 * Control load snapshot codec.
 */
public final class ControlLoadSnapshotCodec extends JsonCodec<ControlLoadSnapshot> {

    private static final String TIME = "time";
    private static final String LATEST = "latest";
    private static final String AVERAGE = "average";

    @Override
    public ObjectNode encode(ControlLoadSnapshot controlLoadSnapshot, CodecContext context) {
        return context.mapper().createObjectNode()
                .put(TIME, controlLoadSnapshot.time())
                .put(LATEST, controlLoadSnapshot.latest())
                .put(AVERAGE, controlLoadSnapshot.average());
    }
}
