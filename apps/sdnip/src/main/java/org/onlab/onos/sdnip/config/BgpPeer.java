package org.onlab.onos.sdnip.config;

import java.util.Objects;

import org.codehaus.jackson.annotate.JsonProperty;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.PortNumber;
import org.onlab.packet.IpAddress;

import com.google.common.base.MoreObjects;

/**
 * Configuration details for a BGP peer.
 */
public class BgpPeer {
    private final ConnectPoint connectPoint;
    private final IpAddress ipAddress;

    /**
     * Creates a new BgpPeer.
     *
     * @param dpid the DPID of the switch the peer is attached at, as a String
     * @param port the port the peer is attached at
     * @param ipAddress the IP address of the peer as a String
     */
    public BgpPeer(@JsonProperty("attachmentDpid") String dpid,
                   @JsonProperty("attachmentPort") int port,
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
    public ConnectPoint connectPoint() {
        return connectPoint;
    }

    /**
     * Gets the IP address of the peer.
     *
     * @return the IP address
     */
    public IpAddress ipAddress() {
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

        if (!(obj instanceof BgpPeer)) {
            return false;
        }

        BgpPeer that = (BgpPeer) obj;
        return Objects.equals(this.connectPoint, that.connectPoint)
                && Objects.equals(this.ipAddress, that.ipAddress);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("connectPoint", connectPoint)
                .add("ipAddress", ipAddress)
                .toString();
    }
}
