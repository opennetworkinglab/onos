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
package org.onosproject.flowapi;

import com.google.common.base.MoreObjects;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Representation of Multi value flow container having custom rules.
 */
public final class ExtFlowContainer {

    private List<ExtFlowTypes> container = new ArrayList<>();
    private String deviceId;

    /**
     * Creates an object of type ExtFlowContainer.
     *
     * @param container flow container
     */
    public ExtFlowContainer(List<ExtFlowTypes> container) {
        this.container = container;
    }

    /**
     * Returns the ExtFlowContainer by setting its value.
     *
     * @param container flow container
     * @return object of ExtFlowContainer
     */
    public static ExtFlowContainer of(List<ExtFlowTypes> container) {
        return new ExtFlowContainer(container);
    }

    /**
     * Returns the list of  ExtFlowTypes value.
     *
     * @return list of ExtFlowTypes
     */
    public List<ExtFlowTypes> container() {
        return container;
    }

    /**
     * Returns the device Id.
     *
     * @return deviceId
     */
    public String deviceId() {
        return deviceId;
    }

    /**
     * Adds the flow type to the container list.
     *
     * @param  obj of ExtFlowTypes type
     */
    public void add(ExtFlowTypes obj) {
        container.add(obj);
    }

    /**
     * Removes the flow type from the container list.
     *
     * @param  obj of ExtFlowTypes type
     */
    public void remove(ExtFlowTypes obj) {
        container.remove(obj);
    }

    /**
     * Sets the device Id to this container.
     *
     * @param deviceId to be set to this container
     */
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(container);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ExtFlowContainer)) {
            return false;
        }
        final ExtFlowContainer other = (ExtFlowContainer) obj;
        return Objects.equals(this.container, other.container)
                && Objects.equals(this.deviceId, other.deviceId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("container", container)
                .add("deviceId", deviceId)
                .toString();
    }
}
