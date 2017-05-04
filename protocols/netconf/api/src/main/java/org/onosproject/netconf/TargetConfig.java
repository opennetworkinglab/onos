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

// TODO Revisit if we this class should be Enum.
// According to NETCONF RFC,
// various additional configuration datastores may be defined by capabilities.
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

    @Override
    public String toString() {
        return this.name;
    }

}
