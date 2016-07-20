/*
 * Copyright 2015-present Open Networking Laboratory
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

package org.onosproject.net.flow.criteria;

import org.onosproject.net.DeviceId;

import java.util.Objects;

/**
 * Criterion for implementing selector extensions.
 */
public class ExtensionCriterion implements Criterion {

    private final ExtensionSelector extensionSelector;
    private final DeviceId deviceId;

    /**
     * Constructor.
     *
     * @param extensionSelector extension selector
     * @param deviceId          device identification
     */
    public ExtensionCriterion(ExtensionSelector extensionSelector, DeviceId deviceId) {
        this.extensionSelector = extensionSelector;
        this.deviceId = deviceId;
    }

    /**
     * Returns the extension selector.
     *
     * @return extension selector
     */
    public ExtensionSelector extensionSelector() {
        return extensionSelector;
    }

    /**
     * Returns the device ID.
     *
     * @return device ID
     */
    public DeviceId deviceId() {
        return deviceId;
    }

    @Override
    public Type type() {
        return Type.EXTENSION;
    }

    @Override
    public String toString() {
        return type().toString() + SEPARATOR + deviceId + "/" + extensionSelector;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type().ordinal(), extensionSelector, deviceId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ExtensionCriterion) {
            ExtensionCriterion that = (ExtensionCriterion) obj;
            return Objects.equals(extensionSelector, that.extensionSelector) &&
                    Objects.equals(deviceId, that.deviceId) &&
                    Objects.equals(this.type(), that.type());
        }
        return false;
    }
}
