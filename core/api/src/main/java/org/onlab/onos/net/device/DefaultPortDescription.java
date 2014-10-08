package org.onlab.onos.net.device;

import org.onlab.onos.net.AbstractDescription;
import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.SparseAnnotations;

/**
 * Default implementation of immutable port description.
 */
public class DefaultPortDescription extends AbstractDescription
        implements PortDescription {

    private final PortNumber number;
    private final boolean isEnabled;

    /**
     * Creates a port description using the supplied information.
     *
     * @param number       port number
     * @param isEnabled    port enabled state
     * @param annotations  optional key/value annotations map
     */
    public DefaultPortDescription(PortNumber number, boolean isEnabled,
                SparseAnnotations... annotations) {
        super(annotations);
        this.number = number;
        this.isEnabled = isEnabled;
    }

    /**
     * Creates a port description using the supplied information.
     *
     * @param base         PortDescription to get basic information from
     * @param annotations  optional key/value annotations map
     */
    public DefaultPortDescription(PortDescription base,
            SparseAnnotations annotations) {
        this(base.portNumber(), base.isEnabled(), annotations);
    }

    @Override
    public PortNumber portNumber() {
        return number;
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    // default constructor for serialization
    private DefaultPortDescription() {
        this.number = null;
        this.isEnabled = false;
    }
}
