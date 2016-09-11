package org.onosproject.drivers.cisco;

import org.onosproject.net.behaviour.ConfigSetter;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.slf4j.Logger;

import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of config setting through netconf.
 * Author: Linqi Guo (linqi@waltznetworks.com)
 */

public class ConfigSetterCsr1000vImpl extends AbstractHandlerBehaviour implements ConfigSetter {

    private final Logger log = getLogger(getClass());

    @Override
    public String setConfiguration(String request) {
        NetconfController controller = checkNotNull(handler().get(NetconfController.class));
        NetconfSession session = controller.getDevicesMap().get(handler().data().deviceId()).getSession();
        String reply, requestNetConfString = new StringBuilder(buildHeadPattern())
                .append(request).append(buildTailPattern()).toString();
        log.info("Sending out netconf request {}", requestNetConfString);

        try {
            reply = session.get(requestNetConfString);
            log.debug("Device {} replies {}", handler().data().deviceId(), reply.replaceAll("\r", ""));
        } catch (IOException e) {
            throw new RuntimeException(new NetconfException("Failed to set configuration via netconf", e));
        }

        return reply;
    }

    private String buildHeadPattern() {
        StringBuilder pattern = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        pattern.append("<rpc>");
        pattern.append("<edit-config>");
        pattern.append("<target>");
        pattern.append("<running/>");
        pattern.append("</target>");
        pattern.append("<config>");
        pattern.append("<cli-config-data>");

        return pattern.toString();
    }

    private String buildTailPattern() {
        StringBuilder pattern = new StringBuilder("</cli-config-data>");
        pattern.append("</config>");
        pattern.append("</edit-config>");
        pattern.append("</rpc>");

        return pattern.toString();
    }
}
