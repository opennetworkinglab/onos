package org.onosproject.cordconfig.access;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Maps;
import org.onlab.packet.MacAddress;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.Config;

import java.util.Map;
import java.util.Optional;

/**
 * Represents configuration for an OLT agent.
 */
public class AccessAgentConfig extends Config<DeviceId> {

    private static final String OLTS = "olts";
    private static final String AGENT_MAC = "mac";

    // TODO: Remove this, it is only useful as long as XOS doesn't manage this.
    private static final String VTN_LOCATION = "vtn-location";

    /**
     * Gets the access agent configuration for this device.
     *
     * @return access agent configuration
     */
    public AccessAgentData getAgent() {
        JsonNode olts = node.get(OLTS);
        if (!olts.isObject()) {
            throw new IllegalArgumentException(OLTS + " should be an object");
        }
        Map<ConnectPoint, MacAddress> oltMacInfo = Maps.newHashMap();
        olts.fields().forEachRemaining(item -> oltMacInfo.put(
                new ConnectPoint(subject(), PortNumber.fromString(item.getKey())),
                MacAddress.valueOf(item.getValue().asText())));

        MacAddress agentMac = MacAddress.valueOf(node.path(AGENT_MAC).asText());

        JsonNode vtn = node.path(VTN_LOCATION);
        Optional<ConnectPoint> vtnLocation;
        if (vtn.isMissingNode()) {
            vtnLocation = Optional.empty();
        } else {
            vtnLocation = Optional.of(ConnectPoint.deviceConnectPoint(vtn.asText()));
        }

        return new AccessAgentData(subject(), oltMacInfo, agentMac, vtnLocation);
    }

}
