package org.onlab.onos.net;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Abstraction of a network connection point expressed as a pair of the
 * network element identifier and port number.
 */
public class ConnectPoint {

    private final ElementId elementId;
    private final PortNumber portNumber;

    /**
     * Creates a new connection point.
     *
     * @param elementId  network element identifier
     * @param portNumber port number
     */
    public ConnectPoint(ElementId elementId, PortNumber portNumber) {
        this.elementId = elementId;
        this.portNumber = portNumber;
    }

    /**
     * Returns the network element identifier.
     *
     * @return element identifier
     */
    public ElementId elementId() {
        return elementId;
    }

    /**
     * Returns the identifier of the infrastructure device if the connection
     * point belongs to a network element which is indeed an infrastructure
     * device.
     *
     * @return network element identifier as a device identifier
     * @throws java.lang.IllegalStateException if connection point is not
     *                                         associated with a device
     */
    @SuppressWarnings("unchecked")
    public DeviceId deviceId() {
        if (elementId instanceof DeviceId) {
            return (DeviceId) elementId;
        }
        throw new IllegalStateException("Connection point not associated " +
                                                "with an infrastructure device");
    }

    /**
     * Returns the connection port number.
     *
     * @return port number
     */
    public PortNumber port() {
        return portNumber;
    }

    @Override
    public int hashCode() {
        return Objects.hash(elementId, portNumber);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ConnectPoint) {
            final ConnectPoint other = (ConnectPoint) obj;
            return Objects.equals(this.elementId, other.elementId) &&
                    Objects.equals(this.portNumber, other.portNumber);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("elementId", elementId)
                .add("portNumber", portNumber)
                .toString();
    }

}
