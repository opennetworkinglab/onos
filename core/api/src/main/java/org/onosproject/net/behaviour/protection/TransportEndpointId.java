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
package org.onosproject.net.behaviour.protection;

import javax.annotation.concurrent.Immutable;

import org.onlab.util.Identifier;

import com.google.common.annotations.Beta;

/**
 * Identifier for a transport entity endpoint.
 * <p>
 * Identifier will be assigned by the implementation of this Behaviour.
 * It must be unique within the Device.
 */
@Beta
@Immutable
public class TransportEndpointId extends Identifier<String> {

    /**
     * Creates a {@link TransportEndpointId} from given String.
     *
     * @param id identifier expressed in String
     * @return {@link TransportEndpointId}
     */
    public static TransportEndpointId of(String id) {
        return new TransportEndpointId(id);
    }

    protected TransportEndpointId(String id) {
        super(id);
    }

    /*
     * Constructor for serialization.
     */
    protected TransportEndpointId() {
    }
}
