/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.incubator.net.virtual;

import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultPort;
import org.onosproject.net.Device;
import org.onosproject.net.Element;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Default representation of a virtual port.
 */
public final class DefaultVirtualPort extends DefaultPort implements VirtualPort {


    private final NetworkId networkId;
    private final Port realizedBy;

    public DefaultVirtualPort(NetworkId networkId, Device device, PortNumber portNumber, Port realizedBy) {
        super((Element) device, portNumber, false, DefaultAnnotations.builder().build());
        this.networkId = networkId;
        this.realizedBy = realizedBy;
    }

    public NetworkId networkId() {
        return networkId;
    }

    @Override
    public Port realizedBy() {
        return realizedBy;
    }

    @Override
    public int hashCode() {
        return Objects.hash(networkId, realizedBy);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultVirtualPort) {
            DefaultVirtualPort that = (DefaultVirtualPort) obj;
            return super.equals(that) &&
                    Objects.equals(this.networkId, that.networkId) &&
                    Objects.equals(this.realizedBy, that.realizedBy);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("networkId", networkId).add("realizedBy", realizedBy).toString();
    }

}
