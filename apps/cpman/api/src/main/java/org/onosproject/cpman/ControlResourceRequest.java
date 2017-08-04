/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.cpman;

import com.google.common.base.MoreObjects;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * A container class that is used to request available resource of remote node.
 */
public class ControlResourceRequest {
    private final ControlResource.Type type;

    /**
     * Instantiates a new control resource request of the control resource type.
     *
     * @param type control resource type
     */
    public ControlResourceRequest(ControlResource.Type type) {
        this.type = type;
    }

    /**
     * Obtains control resource type.
     *
     * @return control resource type
     */
    public ControlResource.Type getType() {
        return type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ControlResourceRequest) {
            final ControlResourceRequest other = (ControlResourceRequest) obj;
            return Objects.equals(this.type, other.type);
        }
        return false;
    }

    @Override
    public String toString() {
        MoreObjects.ToStringHelper helper;
        helper = toStringHelper(this)
                .add("type", type);
        return helper.toString();
    }
}
