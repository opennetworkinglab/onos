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
package org.onosproject.gluon.rsc;

import com.fasterxml.jackson.databind.JsonNode;
import org.onosproject.net.config.Config;

/**
 * Representation of a Etcd response.
 */
public class GluonConfig extends Config<String> {
    public String action;
    public String key;
    public JsonNode value;
    long modifiedIndex;
    long createdIndex;

    public GluonConfig() {
    }

    /**
     * Gluon configuration data model.
     *
     * @param action operation type
     * @param key    proton key
     * @param value  proton value
     * @param mIndex modified time
     * @param cIndex created time
     */
    public GluonConfig(String action, String key, JsonNode value, long mIndex,
                       long cIndex) {
        this.action = action;
        this.key = key;
        this.value = value;
        this.modifiedIndex = mIndex;
        this.createdIndex = cIndex;
    }

    /**
     * Sets the etcdresponse used by network config.
     *
     * @param gluonConfig Etcdresponse data after parsing
     */
    public void setEtcdResponse(GluonConfig gluonConfig) {
        object.put(gluonConfig.key, gluonConfig.value);
    }

    @Override
    public String toString() {
        return "GluonConfig{" +
                "action='" + action + '\'' +
                ", key='" + key + '\'' +
                ", value=" + value +
                ", modifiedIndex=" + modifiedIndex +
                ", createdIndex=" + createdIndex +
                '}';
    }
}
