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
package org.onosproject.net.device;

import org.onosproject.net.AbstractDescription;
import org.onosproject.net.SparseAnnotations;
import org.onlab.packet.ChassisId;

import java.net.URI;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.Device.Type;

/**
 * Default implementation of immutable device description entity.
 */
public class DefaultDeviceDescription extends AbstractDescription
        implements DeviceDescription {
    private final URI uri;
    private final Type type;
    private final String manufacturer;
    private final String hwVersion;
    private final String swVersion;
    private final String serialNumber;
    private final ChassisId chassisId;

    /**
     * Creates a device description using the supplied information.
     *
     * @param uri          device URI
     * @param type         device type
     * @param manufacturer device manufacturer
     * @param hwVersion    device HW version
     * @param swVersion    device SW version
     * @param serialNumber device serial number
     * @param chassis      chassis id
     * @param annotations  optional key/value annotations map
     */
    public DefaultDeviceDescription(URI uri, Type type, String manufacturer,
                                    String hwVersion, String swVersion,
                                    String serialNumber, ChassisId chassis,
                                    SparseAnnotations... annotations) {
        super(annotations);
        this.uri = checkNotNull(uri, "Device URI cannot be null");
        this.type = checkNotNull(type, "Device type cannot be null");
        this.manufacturer = manufacturer;
        this.hwVersion = hwVersion;
        this.swVersion = swVersion;
        this.serialNumber = serialNumber;
        this.chassisId = chassis;
    }

    /**
     * Creates a device description using the supplied information.
     * @param base DeviceDescription to basic information
     * @param annotations Annotations to use.
     */
    public DefaultDeviceDescription(DeviceDescription base,
                                    SparseAnnotations... annotations) {
        this(base.deviceURI(), base.type(), base.manufacturer(),
             base.hwVersion(), base.swVersion(), base.serialNumber(),
             base.chassisId(), annotations);
    }

    /**
     * Creates a device description using the supplied information.
     * @param base DeviceDescription to basic information (except for type)
     * @param type device type
     * @param annotations Annotations to use.
     */
    public DefaultDeviceDescription(DeviceDescription base, Type type, SparseAnnotations... annotations) {
        this(base.deviceURI(), type, base.manufacturer(),
                base.hwVersion(), base.swVersion(), base.serialNumber(),
                base.chassisId(), annotations);
    }

    @Override
    public URI deviceURI() {
        return uri;
    }

    @Override
    public Type type() {
        return type;
    }

    @Override
    public String manufacturer() {
        return manufacturer;
    }

    @Override
    public String hwVersion() {
        return hwVersion;
    }

    @Override
    public String swVersion() {
        return swVersion;
    }

    @Override
    public String serialNumber() {
        return serialNumber;
    }

    @Override
    public ChassisId chassisId() {
        return chassisId;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("uri", uri).add("type", type).add("mfr", manufacturer)
                .add("hw", hwVersion).add("sw", swVersion)
                .add("serial", serialNumber)
                .toString();
    }

    // default constructor for serialization
    private DefaultDeviceDescription() {
        this.uri = null;
        this.type = null;
        this.manufacturer = null;
        this.hwVersion = null;
        this.swVersion = null;
        this.serialNumber = null;
        this.chassisId = null;
    }
}
