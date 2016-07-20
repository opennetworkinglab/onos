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
package org.onosproject.iptopology.api;

import static com.google.common.base.MoreObjects.toStringHelper;

import java.util.Objects;

import org.onosproject.net.Element;

/**
 * Default Device interface implementation.
 */
public class DefaultDeviceIntf implements DeviceIntf {

    private final Element element;
    private final DeviceInterface deviceInterface;

    /**
     * Constructor to initialize device interface parameters.
     *
     * @param element parent network element
     * @param deviceInterface device interface
     */
    public DefaultDeviceIntf(Element element, DeviceInterface deviceInterface) {
        this.element = element;
        this.deviceInterface = deviceInterface;
    }

    @Override
    public Element element() {
        return element;
    }

    @Override
    public DeviceInterface deviceInterface() {
        return deviceInterface;
    }

    @Override
    public int hashCode() {
        return Objects.hash(element, deviceInterface);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof DefaultDeviceIntf) {
            final DefaultDeviceIntf other = (DefaultDeviceIntf) obj;
            return Objects.equals(this.element.id(), other.element.id())
                    && Objects.equals(this.deviceInterface, other.deviceInterface);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("element", element.id())
                .add("deviceInterface", deviceInterface)
                .toString();
    }
}