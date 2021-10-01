/*
 * Copyright 2014-present Open Networking Foundation
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
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.Device.Type;
import com.google.common.base.Objects;

/**
 * Default implementation of immutable device description entity.
 */
public class DefaultDeviceDescription extends AbstractDescription
        implements DeviceDescription {

    private static final int MANUFACTURER_MAX_LENGTH = 256;
    private static final int HW_VERSION_MAX_LENGTH = 256;
    private static final int SW_VERSION_MAX_LENGTH = 256;
    private static final int SERIAL_NUMBER_MAX_LENGTH = 256;

    private final URI uri;
    private final Type type;
    private final String manufacturer;
    private final String hwVersion;
    private final String swVersion;
    private final String serialNumber;
    private final ChassisId chassisId;
    private final boolean defaultAvailable;

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
        this(uri, type, manufacturer, hwVersion, swVersion, serialNumber,
             chassis, true, annotations);
    }

    /**
     * Creates a device description using the supplied information.
     *
     * @param uri            device URI
     * @param type           device type
     * @param manufacturer   device manufacturer
     * @param hwVersion      device HW version
     * @param swVersion      device SW version
     * @param serialNumber   device serial number
     * @param chassis        chassis id
     * @param defaultAvailable optional whether device is by default available
     * @param annotations    optional key/value annotations map
     */
    public DefaultDeviceDescription(URI uri, Type type, String manufacturer,
                                    String hwVersion, String swVersion,
                                    String serialNumber, ChassisId chassis,
                                    boolean defaultAvailable,
                                    SparseAnnotations... annotations) {
        super(annotations);
        this.uri = checkNotNull(uri, "Device URI cannot be null");
        this.type = checkNotNull(type, "Device type cannot be null");

        if (hwVersion != null) {
            checkArgument(hwVersion.length() <= HW_VERSION_MAX_LENGTH,
                    "hwVersion exceeds maximum length " + HW_VERSION_MAX_LENGTH);
        }
        if (swVersion != null) {
            checkArgument(swVersion.length() <= SW_VERSION_MAX_LENGTH,
                    "swVersion exceeds maximum length " + SW_VERSION_MAX_LENGTH);
        }
        if (manufacturer != null) {
            checkArgument(manufacturer.length() <= MANUFACTURER_MAX_LENGTH,
                    "manufacturer exceeds maximum length " + MANUFACTURER_MAX_LENGTH);
        }
        if (serialNumber != null) {
            checkArgument(serialNumber.length() <= SERIAL_NUMBER_MAX_LENGTH,
                    "serialNumber exceeds maximum length " + SERIAL_NUMBER_MAX_LENGTH);
        }

        this.manufacturer = manufacturer;
        this.hwVersion = hwVersion;
        this.swVersion = swVersion;
        this.serialNumber = serialNumber;
        //Avoid propagation of null chassisID and substitute it with UNKNOWN
        if (chassis == null) {
            chassis = new ChassisId();
        }
        this.chassisId = chassis;
        this.defaultAvailable = defaultAvailable;
    }

    /**
     * Creates a device description using the supplied information.
     * @param base DeviceDescription to basic information
     * @param annotations Annotations to use.
     */
    public DefaultDeviceDescription(DeviceDescription base,
                                    SparseAnnotations... annotations) {
        this(base.deviceUri(), base.type(), base.manufacturer(),
             base.hwVersion(), base.swVersion(), base.serialNumber(),
             base.chassisId(), base.isDefaultAvailable(), annotations);
    }

    /**
     * Creates a device description using the supplied information.
     * @param base DeviceDescription to basic information (except for type)
     * @param type device type
     * @param annotations Annotations to use.
     */
    public DefaultDeviceDescription(DeviceDescription base, Type type,
                                    SparseAnnotations... annotations) {
        this(base.deviceUri(), type, base.manufacturer(),
                base.hwVersion(), base.swVersion(), base.serialNumber(),
                base.chassisId(), base.isDefaultAvailable(), annotations);
    }

    /**
     * Creates a device description using the supplied information.
     *
     * @param base DeviceDescription to basic information (except for defaultAvailable)
     * @param defaultAvailable whether device should be made available by default
     * @param annotations Annotations to use.
     */
    public DefaultDeviceDescription(DeviceDescription base,
                                    boolean defaultAvailable,
                                    SparseAnnotations... annotations) {
        this(base.deviceUri(), base.type(), base.manufacturer(),
             base.hwVersion(), base.swVersion(), base.serialNumber(),
             base.chassisId(), defaultAvailable, annotations);
    }

    /**
     * Creates a device description using the supplied information.
     *
     * @param base base
     * @param annotations annotations
     * @return device description
     */
    public static DefaultDeviceDescription copyReplacingAnnotation(DeviceDescription base,
                                                                   SparseAnnotations annotations) {
        return new DefaultDeviceDescription(base, annotations);
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
    public boolean isDefaultAvailable() {
        return defaultAvailable;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("uri", uri).add("type", type).add("mfr", manufacturer)
                .add("hw", hwVersion).add("sw", swVersion)
                .add("serial", serialNumber)
                .add("chassisId", chassisId)
                .add("defaultAvailable", defaultAvailable)
                .add("annotations", annotations())
                .toString();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), uri, type, manufacturer,
                                hwVersion, swVersion, serialNumber, chassisId,
                                defaultAvailable);
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof DefaultDeviceDescription) {
            if (!super.equals(object)) {
                return false;
            }
            DefaultDeviceDescription that = (DefaultDeviceDescription) object;
            return Objects.equal(this.uri, that.uri)
                    && Objects.equal(this.type, that.type)
                    && Objects.equal(this.manufacturer, that.manufacturer)
                    && Objects.equal(this.hwVersion, that.hwVersion)
                    && Objects.equal(this.swVersion, that.swVersion)
                    && Objects.equal(this.serialNumber, that.serialNumber)
                    && Objects.equal(this.chassisId, that.chassisId)
                    && Objects.equal(this.defaultAvailable, that.defaultAvailable);
        }
        return false;
    }

    // default constructor for serialization
    DefaultDeviceDescription() {
        this.uri = null;
        this.type = null;
        this.manufacturer = null;
        this.hwVersion = null;
        this.swVersion = null;
        this.serialNumber = null;
        this.chassisId = null;
        this.defaultAvailable = true;
    }
}
