/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.ovsdb.controller;

import org.onlab.util.Identifier;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The class representing a datapathid.
 * This class is immutable.
 */
public final class OvsdbDatapathId extends Identifier<String> {
    /**
     * Constructor from a String.
     *
     * @param value the datapathid to use
     */
    public OvsdbDatapathId(String value) {
        super(checkNotNull(value, "value is not null"));
    }

    /**
     * Gets the value of datapathid.
     *
     * @return the value of datapathid
     */
    public String value() {
        return identifier;
    }
}
