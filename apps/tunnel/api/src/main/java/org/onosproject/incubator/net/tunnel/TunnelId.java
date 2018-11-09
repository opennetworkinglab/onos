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

package org.onosproject.incubator.net.tunnel;

import com.google.common.annotations.Beta;
import org.onlab.util.Identifier;

/**
 * Representation of a Tunnel Id.
 */
@Beta
public final class TunnelId extends Identifier<String> {
    /**
     * Creates an tunnel identifier from the specified tunnel.
     *
     * @param value string value
     * @return tunnel identifier
     */
    public static TunnelId valueOf(String value) {
        return new TunnelId(value);
    }

    /**
     * Constructor for serializer.
     */
    TunnelId() {
        super("0");
    }

    /**
     * Constructs the ID corresponding to a given string value.
     *
     * @param value the underlying value of this ID
     */
    TunnelId(String value) {
        super(value);
    }

    @Override
    public String toString() {
        return id();
    }
}
