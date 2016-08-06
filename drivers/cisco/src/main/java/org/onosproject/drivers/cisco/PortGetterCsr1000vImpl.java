package org.onosproject.drivers.cisco;

import org.onosproject.net.behaviour.PortDiscovery;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Retrieve port description from Cisco CSR1000v router via netconf.
 */
public class PortGetterCsr1000vImpl extends AbstractHandlerBehaviour
        implements PortDiscovery {

    private final Logger log = getLogger(getClass());

    @Override
    public List<PortDescription> getPorts() {
        NetconfController controller = checkNotNull(handler().get(NetconfController.class));
        NetconfSession session = controller.getDevicesMap().get(handler().data().deviceId()).getSession();
        String reply;
        try {
            reply = session.get(getPortsRequestBuilder());
            log.info(reply);
        } catch (IOException e) {
            throw new RuntimeException(new NetconfException("Failed to retrieve configuration.", e));
        }

        /*
         * Interface port numbering is from 1 and up to the number of interfaces supported on CSR1000v.
         * http://www.cisco.com/c/en/us/td/docs/routers/csr1000/software/configuration/csr1000Vswcfg/csroverview.html
         *
         * So GigabitEthernet1 can be seen as port 1. First get name of the interfaces and extract their numbers.
         * Secondly, extract their speed.
         */

        List<PortDescription> descriptions = new ArrayList<>();
        return descriptions;
    }

    /**
     * Builds a request crafted to get the configuration required to create port
     * descriptions for the device.
     * @return The request string.
     */
    private String getPortsRequestBuilder() {
        StringBuilder rpc = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        //Message ID is injected later.
        rpc.append("<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">");
        rpc.append("<get>");
        rpc.append("<filter>");
        rpc.append("<config-format-text-block>");
        /* Use regular expression to extract only name of the interfaces from running-config */
        rpc.append("<text-filter-spec> | include interface </text-filter-spec>");
        rpc.append("</config-format-text-block>");
        rpc.append("<oper-data-format-text-block>");
        /* Use regular expression to extract speed of the interfaces */
        rpc.append("<exec>show interfaces | include BW [0-9]+ Kbit/sec</exec>");
        rpc.append("</oper-data-format-text-block>");
        rpc.append("</filter>");
        rpc.append("</get>");
        rpc.append("</rpc>");
        return rpc.toString();
    }
}
