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
        String reply;

        try {
            reply = session.get(request);
            log.debug("Device {} replies {}", handler().data().deviceId(), reply.replaceAll("\r", ""));
        } catch (IOException e) {
            throw new RuntimeException(new NetconfException("Failed to set configuration via netconf", e));
        }

        return reply;
    }
}
