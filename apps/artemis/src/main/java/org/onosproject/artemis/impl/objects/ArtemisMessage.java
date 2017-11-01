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

package org.onosproject.artemis.impl.objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.io.Serializable;

/**
 * Messages that are exchanged between the two MOAS entities.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "type", "localIp", "localPrefix"
})
public class ArtemisMessage implements Serializable {

    @JsonProperty("type")
    private Type type;

    @JsonProperty("localIp")
    private String localIp;

    @JsonProperty("localPrefix")
    private String localPrefix;

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getLocalIp() {
        return localIp;
    }

    public void setLocalIp(String localIp) {
        this.localIp = localIp;
    }

    public String getLocalPrefix() {
        return localPrefix;
    }

    public void setLocalPrefix(String localPrefix) {
        this.localPrefix = localPrefix;
    }

    @Override
    public String toString() {
        return "ArtemisMessage{" +
                "type=" + type +
                ", localIp=" + localIp +
                ", localPrefix=" + localPrefix +
                '}';
    }

    public enum Type {
        INITIATE_FROM_CLIENT,
        INITIATE_FROM_SERVER
    }
}
