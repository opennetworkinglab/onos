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

/**
 * @deprecated in 1.10.0 use TargetConfiguration instead
 * According to NETCONF RFC,
 * various additional configuration datastores may be defined by capabilities.
 */
@Deprecated
public enum TargetConfig {
    RUNNING("running"),
    CANDIDATE("candidate"),
    STARTUP("startup");

    private String name;

    TargetConfig(String name) {
        this.name = name;
    }

    public static TargetConfig toTargetConfig(String targetConfig) {
        return valueOf(targetConfig.toUpperCase());
    }

    public static DatastoreId toDatastoreId(String cfg) {
        return toDatastoreId(toTargetConfig(cfg));
    }

    public static DatastoreId toDatastoreId(TargetConfig cfg) {
        switch (cfg) {
        case CANDIDATE:
            return DatastoreId.CANDIDATE;
        case RUNNING:
            return DatastoreId.RUNNING;
        case STARTUP:
            return DatastoreId.STARTUP;
        default:
            return DatastoreId.datastore(cfg.name);
        }
    }

    @Override
    public String toString() {
        return this.name;
    }

}
