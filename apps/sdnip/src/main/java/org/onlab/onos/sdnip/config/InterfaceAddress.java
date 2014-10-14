package org.onlab.onos.sdnip.config;

import java.util.Objects;

import org.codehaus.jackson.annotate.JsonProperty;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.PortNumber;
import org.onlab.packet.IpAddress;

/**
 * Represents an address of a {@link BgpSpeaker} configured on an
 * {@link Interface}.
 * <p/>
 * Each InterfaceAddress includes the interface name and an IP address.
 */
public class InterfaceAddress {
    private final ConnectPoint connectPoint;
    private final IpAddress ipAddress;

    /**
     * Class constructor used by the JSON library to create an object.
     *
     * @param interfaceName the interface name for which an IP address of a BGP
     * router is configured
     * @param ipAddress the IP address of a {@link BgpSpeaker} configured on
     * the interface
     */
    public InterfaceAddress(@JsonProperty("interfaceDpid") String dpid,
                            @JsonProperty("interfacePort") int port,
                            @JsonProperty("ipAddress") String ipAddress) {
        this.connectPoint = new ConnectPoint(
                DeviceId.deviceId(SdnIpConfigReader.dpidToUri(dpid)),
                PortNumber.portNumber(port));
        this.ipAddress = IpAddress.valueOf(ipAddress);
    }

    /**
     * Gets the connection point of the peer.
     *
     * @return the connection point
     */
    public ConnectPoint getConnectPoint() {
        return connectPoint;
    }

    /**
     * Gets the IP address of a BGP speaker configured on an {@link Interface}.
     *
     * @return the IP address
     */
    public IpAddress getIpAddress() {
        return ipAddress;
    }

    @Override
    public int hashCode() {
        return Objects.hash(connectPoint, ipAddress);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof InterfaceAddress)) {
            return false;
        }

        InterfaceAddress that = (InterfaceAddress) obj;
        return Objects.equals(this.connectPoint, that.connectPoint)
                && Objects.equals(this.ipAddress, that.ipAddress);
    }
}
