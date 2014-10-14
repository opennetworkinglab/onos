package org.onlab.onos.sdnip.config;

import java.util.List;
import java.util.Objects;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.PortNumber;
import org.onlab.packet.MacAddress;

/**
 * Represents a BGP daemon in SDN network.
 * <p/>
 * Each BGP speaker has a attachment point, which includes a switch DPID and a
 * switch port. Each BGP speaker has one MAC address and several IP addresses,
 * which are used to peer with BGP peers outside the SDN network. For each
 * peer outside the SDN network, we configure a different IP address to BGP
 * speaker inside the SDN network.
 * <p/>
 * Each BGP speaker has a name, which is a unique identifying String that is
 * used to reference this speaker in the configuration.
 */
public class BgpSpeaker {
    private final String speakerName;
    private final ConnectPoint attachmentSwitchPort;
    private final MacAddress macAddress;
    private List<InterfaceAddress> interfaceAddresses;

    /**
     * Class constructor used by the JSON library to create an object.
     *
     * @param speakerName the name of the BGP router inside SDN network
     * @param attachmentDpid the DPID where the BGP router is attached to
     * @param attachmentPort the port where the BGP router is attached to
     * @param macAddress the MAC address of the BGP router
     */
    @JsonCreator
    public BgpSpeaker(@JsonProperty("name") String speakerName,
            @JsonProperty("attachmentDpid") String attachmentDpid,
            @JsonProperty("attachmentPort") int attachmentPort,
            @JsonProperty("macAddress") String macAddress) {

        this.speakerName = speakerName;
        this.macAddress = MacAddress.valueOf(macAddress);
        this.attachmentSwitchPort = new ConnectPoint(
                DeviceId.deviceId(SdnIpConfigReader.dpidToUri(attachmentDpid)),
                PortNumber.portNumber(attachmentPort));
    }

    /**
     * Sets the addresses we configured for the BGP speaker on all virtual
     * {@link Interface}s.
     *
     * @param interfaceAddresses a list of IP addresses of the BGP speaker
     * configured on all virtual interfaces
     */
    @JsonProperty("interfaceAddresses")
    public void setInterfaceAddresses(
            List<InterfaceAddress> interfaceAddresses) {
        this.interfaceAddresses = interfaceAddresses;
    }

    /**
     * Gets the BGP speaker name.
     *
     * @return the BGP speaker name
     */
    public String getSpeakerName() {
        return speakerName;
    }

    /**
     * Gets the switch port where the BGP speaker is attached.
     *
     * @return the switch port where the BGP speaker is attached
     */
    public ConnectPoint getAttachmentSwitchPort() {
        return attachmentSwitchPort;
    }

    /**
     * Gets the MAC address of the BGP speaker.
     *
     * @return the MAC address of the BGP speaker
     */
    public MacAddress getMacAddress() {
        return macAddress;
    }

    /**
     * Gets all IP addresses configured on all {@link Interface}s of the
     * BGP speaker.
     *
     * @return a list of IP addresses of the BGP speaker configured on all
     * virtual interfaces
     */
    public List<InterfaceAddress> getInterfaceAddresses() {
        return interfaceAddresses;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof BgpSpeaker)) {
            return false;
        }

        BgpSpeaker otherBgpSpeaker = (BgpSpeaker) other;

        return  speakerName.equals(otherBgpSpeaker.speakerName) &&
                attachmentSwitchPort.equals(
                        otherBgpSpeaker.attachmentSwitchPort) &&
                macAddress.equals(otherBgpSpeaker.macAddress) &&
                interfaceAddresses.equals(otherBgpSpeaker.interfaceAddresses);
    }

    @Override
    public int hashCode() {
        return Objects.hash(speakerName, attachmentSwitchPort, macAddress,
                interfaceAddresses);

    }
}
