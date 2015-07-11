/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.net.behaviour;

import java.util.Objects;

import org.onosproject.net.AbstractDescription;
import org.onosproject.net.DeviceId;
import org.onosproject.net.SparseAnnotations;

import com.google.common.base.MoreObjects;

/**
 * The default implementation of bridge.
 */
public final class DefaultBridgeDescription extends AbstractDescription
        implements BridgeDescription {

    private final BridgeName name;
    private final DeviceId deviceId;
    private final DeviceId controllerId;

    public DefaultBridgeDescription(BridgeName name, DeviceId controllerId,
                                    DeviceId deviceId,
                                    SparseAnnotations... annotations) {
        super(annotations);
        this.name = name;
        this.deviceId = deviceId;
        this.controllerId = controllerId;
    }

    @Override
    public BridgeName bridgeName() {
        return name;
    }

    @Override
    public DeviceId deviceId() {
        return deviceId;
    }

    @Override
    public DeviceId cotrollerDeviceId() {
        return controllerId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, deviceId, controllerId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultBridgeDescription) {
            final DefaultBridgeDescription that = (DefaultBridgeDescription) obj;
            return this.getClass() == that.getClass()
                    && Objects.equals(this.name, that.name)
                    && Objects.equals(this.deviceId, that.deviceId)
                    && Objects.equals(this.controllerId, that.controllerId);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass()).add("name", name)
                .add("deviceId", deviceId).add("controllerId", controllerId)
                .toString();
    }

}
