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
package org.onosproject.iptopology.api.device;

import org.onosproject.iptopology.api.DeviceTed;
import org.onosproject.iptopology.api.IpDeviceIdentifier;
import org.onosproject.net.AbstractDescription;
import org.onosproject.net.SparseAnnotations;

import java.net.URI;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.iptopology.api.IpDevice.Type;

/**
 * Default implementation of immutable device description entity.
 */
public class DefaultIpDeviceDescription extends AbstractDescription
        implements IpDeviceDescription {
    private final URI uri;
    private final Type type;
    private final IpDeviceIdentifier deviceIdentifier;
    private final DeviceTed deviceTed;

    /**
     * Creates an ip device description using the supplied information.
     *
     * @param uri               device URI
     * @param type              device type
     * @param deviceIdentifier  device manufacturer
     * @param deviceTed         device Traffic Engineering parameters
     * @param annotations  optional key/value annotations map
     */
    public DefaultIpDeviceDescription(URI uri, Type type, IpDeviceIdentifier deviceIdentifier,
                                    DeviceTed deviceTed, SparseAnnotations... annotations) {
        super(annotations);
        this.uri = checkNotNull(uri, "Device URI cannot be null");
        this.type = checkNotNull(type, "Device type cannot be null");
        this.deviceIdentifier = deviceIdentifier;
        this.deviceTed = deviceTed;
    }

    /**
     * Creates an ip device description using the supplied information.
     * @param base IpDeviceDescription to basic information
     * @param annotations Annotations to use.
     */
    public DefaultIpDeviceDescription(IpDeviceDescription base, SparseAnnotations... annotations) {
        this(base.deviceUri(), base.type(), base.deviceIdentifier(),
             base.deviceTed(), annotations);
    }

    /**
     * Creates an ip device description using the supplied information.
     * @param base IpDeviceDescription to basic information (except for type)
     * @param type device type
     * @param annotations Annotations to use.
     */
    public DefaultIpDeviceDescription(IpDeviceDescription base, Type type, SparseAnnotations... annotations) {
        this(base.deviceUri(), type, base.deviceIdentifier(),
                base.deviceTed(), annotations);
    }

    @Override
    public URI deviceUri() {
        return uri;
    }

    @Override
    public Type type() {
        return type;
    }

    @Override
    public IpDeviceIdentifier deviceIdentifier() {
        return deviceIdentifier;
    }

    @Override
    public DeviceTed deviceTed() {
        return deviceTed;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("uri", uri)
                .add("type", type)
                .add("devid", deviceIdentifier)
                .add("devTed", deviceTed)
                .toString();
    }

    /**
     * Default constructor for serialization.
     */
    private DefaultIpDeviceDescription() {
        this.uri = null;
        this.type = null;
        this.deviceIdentifier = null;
        this.deviceTed = null;
    }
}
