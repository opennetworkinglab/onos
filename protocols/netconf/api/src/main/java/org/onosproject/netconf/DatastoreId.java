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
package org.onosproject.netconf;

import static com.google.common.base.Preconditions.checkArgument;

import org.onlab.util.Identifier;

/**
 * Identifier object to specify datastore.
 */
public class DatastoreId extends Identifier<String> {

    /**
     * A configuration datastore holding
     * the complete configuration currently active on the device.  The
     * running configuration datastore always exists.
     */
    public static final DatastoreId RUNNING = datastore("running");

    /**
     * A configuration datastore that
     * can be manipulated without impacting the device's current
     * configuration and that can be committed to the running
     * configuration datastore.  Not all devices support a candidate
     * configuration datastore.
     */
    public static final DatastoreId CANDIDATE = datastore("candidate");

    /**
     * The configuration datastore
     * holding the configuration loaded by the device when it boots.
     * Only present on devices that separate the startup configuration
     * datastore from the running configuration datastore.
     */
    public static final DatastoreId STARTUP = datastore("startup");

    /**
     * Returns datastore identifier object.
     *
     * @param name of the datastore
     * @return identifier
     */
    public static DatastoreId datastore(String name) {
        return new DatastoreId(name);
    }

    DatastoreId(String name) {
        super(name);
        checkArgument(!name.isEmpty());
    }

    /**
     * Returns datastore name as XML tag.
     * @return xml tag
     */
    public String asXml() {
        return "<" + id() + "/>";
    }

    @Override
    public final String toString() {
        return id();
    }

}
