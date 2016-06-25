/*
 * Copyright 2016-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.dhcprelay;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.config.Config;

import static org.onosproject.net.config.Config.FieldPresence.MANDATORY;
/**
 * DHCP Relay Config class.
 */
public class DhcpRelayConfig extends Config<ApplicationId> {

    private static final String DHCP_CONNECT_POINT = "dhcpserverConnectPoint";

    @Override
    public boolean isValid() {

        return hasOnlyFields(DHCP_CONNECT_POINT) &&
                isConnectPoint(DHCP_CONNECT_POINT, MANDATORY);
    }

    /**
     * Returns the dhcp server connect point.
     *
     * @return dhcp server connect point
     */
    public ConnectPoint getDhcpServerConnectPoint() {
        return ConnectPoint.deviceConnectPoint(object.path(DHCP_CONNECT_POINT).asText());
    }
}
