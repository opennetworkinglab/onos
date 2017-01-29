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
package org.onosproject.provider.lisp.mapping.util;

import org.onosproject.lisp.msg.protocols.LispMapNotify;
import org.onosproject.lisp.msg.protocols.LispMapReply;
import org.onosproject.mapping.MappingEntry;
import org.onosproject.net.DeviceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mapping entry builder class.
 */
public class MappingEntryBuilder {
    private static final Logger log = LoggerFactory.getLogger(MappingEntryBuilder.class);

    private final DeviceId deviceId;
    private final LispMapReply mapReply;
    private final LispMapNotify mapNotify;

    /**
     * Default constructor for MappingEntryBuilder.
     *
     * @param deviceId device identifier
     * @param mapReply map reply message
     */
    public MappingEntryBuilder(DeviceId deviceId, LispMapReply mapReply) {
        this.deviceId = deviceId;
        this.mapReply = mapReply;
        this.mapNotify = null;
    }

    /**
     * Default constructor for MappingEntryBuilder.
     *
     * @param deviceId  device identifier
     * @param mapNotify map notify message
     */
    public MappingEntryBuilder(DeviceId deviceId, LispMapNotify mapNotify) {
        this.deviceId = deviceId;
        this.mapNotify = mapNotify;
        this.mapReply = null;
    }

    public MappingEntry build() {
        // TODO: provide a way to build mapping entry from input parameters
        return null;
    }
}
