package org.onlab.onos.net.device;

import org.onlab.onos.net.AbstractDescription;
import org.onlab.onos.net.SparseAnnotations;

import java.net.URI;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.onos.net.Device.Type;

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

    /**
     * Creates a device description using the supplied information.
     *
     * @param uri          device URI
     * @param type         device type
     * @param manufacturer device manufacturer
     * @param hwVersion    device HW version
     * @param swVersion    device SW version
     * @param serialNumber device serial number
     * @param annotations  optional key/value annotations map
     */
    public DefaultDeviceDescription(URI uri, Type type, String manufacturer,
                                    String hwVersion, String swVersion,
                                    String serialNumber,
                                    SparseAnnotations... annotations) {
        super(annotations);
        this.uri = checkNotNull(uri, "Device URI cannot be null");
        this.type = checkNotNull(type, "Device type cannot be null");
        this.manufacturer = manufacturer;
        this.hwVersion = hwVersion;
        this.swVersion = swVersion;
        this.serialNumber = serialNumber;
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
             annotations);
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
    public String toString() {
        return toStringHelper(this)
                .add("uri", uri).add("type", type).add("mfr", manufacturer)
                .add("hw", hwVersion).add("sw", swVersion)
                .add("serial", serialNumber)
                .toString();
    }

}
