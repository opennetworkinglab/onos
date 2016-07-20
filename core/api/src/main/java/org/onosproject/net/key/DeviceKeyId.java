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

package org.onosproject.net.key;

import org.onlab.util.Identifier;

/**
 * Device key identifier backed by a string value.
 */
public final class DeviceKeyId extends Identifier<String> {

    /**
     * Constructor for serialization.
     */
    private DeviceKeyId() {
        super();
    }

    /**
     * Constructs the ID corresponding to a given string value.
     *
     * @param value the underlying value of this ID
     */
    private DeviceKeyId(String value) {
        super(value);
    }

    /**
     * Creates a new device key identifier.
     *
     * @param id backing identifier value
     * @return device key identifier
     */
    public static DeviceKeyId deviceKeyId(String id) {
        return new DeviceKeyId(id);
    }

}
