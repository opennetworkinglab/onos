package org.onlab.onos.sdnip.config;

import java.util.Objects;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.PortNumber;
import org.onlab.packet.IpPrefix;

/**
 * Represents an interface, which is an external-facing switch port that
 * connects to another network.
 * <p/>
 * SDN-IP treats external-facing ports similarly to router ports. Logically, it
 * assigns an IP subnetwork prefix and several IP addresses to each port which
 * are used for communication with the BGP peers located in other networks, for
 * example, the BGP peering sessions. The peers in other networks will be
 * configured to peer with the IP addresses (logically) assigned to the
 * interface. The logical {@code Interface} construct maps on to a physical
 * port in the data plane, which of course has no notion of IP addresses.
 * <p/>
 * Each interface has a name, which is a unique identifying String that is used
 * to reference this interface in the configuration (for example, to map
 * {@link BgpPeer}s to {@code Interfaces}.
 */
public class Interface {
    private final String name;
    private final ConnectPoint switchPort;
    private final IpPrefix ip4Prefix;

    /**
     * Class constructor used by the JSON library to create an object.
     *
     * @param name the name of the interface
     * @param dpid the dpid of the switch
     * @param port the port on the switch
     * @param prefixAddress the network prefix address logically assigned to the
     * interface
     * @param prefixLength the length of the network prefix of the IP address
     */
    @JsonCreator
    public Interface(@JsonProperty("name") String name,
                     @JsonProperty("dpid") String dpid,
                     @JsonProperty("port") int port,
                     @JsonProperty("ipAddress") String prefixAddress,
                     @JsonProperty("prefixLength") short prefixLength) {
        this.name = name;
        this.switchPort = new ConnectPoint(
                DeviceId.deviceId(SdnIpConfigReader.dpidToUri(dpid)),
                PortNumber.portNumber(port));
        this.ip4Prefix = IpPrefix.valueOf(prefixAddress + "/" + prefixLength);
    }

    /**
     * Gets the name of the interface.
     *
     * @return the name of the interface
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the {@link SwitchPort} that this interface maps to.
     *
     * @return the switch port
     */
    public ConnectPoint getSwitchPort() {
        return switchPort;
    }

    /**
     * Gets the IP prefix of the subnetwork which is logically assigned
     * to the switch port.
     *
     * @return the IP prefix
     */
   public IpPrefix getIp4Prefix() {
        return ip4Prefix;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Interface)) {
            return false;
        }

        Interface otherInterface = (Interface) other;

        return  name.equals(otherInterface.name) &&
                switchPort.equals(otherInterface.switchPort) &&
                ip4Prefix.equals(otherInterface.ip4Prefix);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, switchPort, ip4Prefix);
    }
}
