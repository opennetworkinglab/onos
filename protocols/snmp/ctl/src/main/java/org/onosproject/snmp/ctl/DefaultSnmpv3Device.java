/*
 * Copyright 2021-present Open Networking Foundation
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

package org.onosproject.snmp.ctl;

import com.google.common.base.MoreObjects;
import org.apache.commons.lang.StringUtils;
import org.onosproject.snmp.Snmpv3Device;
import org.onosproject.snmp.SnmpDeviceConfig;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkState;

/**
 * This is a logical representation of actual SNMPv3 device.
 * carrying all the necessary information to connect and execute
 * SNMPv3 operations.
 */
public class DefaultSnmpv3Device extends DefaultSnmpDevice implements Snmpv3Device {

    private final String securityLevel;

    private final String securityName;

    private final String authProtocol;

    private final String authPassword;

    private final String privProtocol;

    private final String privPassword;

    private final String contextName;

    /**
     * Create a new DefaultSnmpv3Device.
     *
     * @param config snmp device config
     */
    public DefaultSnmpv3Device(SnmpDeviceConfig config) {
        super(config);
        checkState(!StringUtils.isEmpty(config.securityLevel()),
                "SNMPv3 security level cannot be null or empty");
        checkState(!StringUtils.isEmpty(config.securityName()),
                "SNMPv3 security name cannot be null or empty");
        this.securityLevel = config.securityLevel();
        this.securityName = config.securityName();
        this.authProtocol = config.authProtocol();
        this.authPassword = config.authPassword();
        this.privProtocol = config.privacyProtocol();
        this.privPassword = config.privacyPassword();
        this.contextName = config.contextName();
    }

    /**
     * Retrieves the security name of SNMPv3 device.
     *
     * @return security name
     */
    @Override
    public String getSecurityName() {
        return securityName;
    }

    /**
     * Retrieves the security level of SNMPv3 device.
     *
     * @return security level
     */
    @Override
    public String getSecurityLevel() {
        return securityLevel;
    }

    /**
     * Retrieves the authentication protocol of SNMPv3 device.
     *
     * @return authentication protocol
     */
    @Override
    public String getAuthProtocol() {
        return authProtocol;
    }

    /**
     * Retrieves the authentication password of SNMPv3 device.
     *
     * @return authentication password
     */
    @Override
    public String getAuthPassword() {
        return authPassword;
    }

    /**
     * Retrieves the privacy protocol of SNMPv3 device.
     *
     * @return privacy protocol
     */
    @Override
    public String getPrivProtocol() {
        return privProtocol;
    }

    /**
     * Retrieves the privacy password of SNMPv3 device.
     *
     * @return privacy password
     */
    @Override
    public String getPrivPassword() {
        return privPassword;
    }

    /**
     * Retrieves the context name of SNMPv3 device.
     *
     * @return context name
     */
    @Override
    public String getContextName() {
        return contextName;
    }

    /**
     * Convert the Snmpv3 device to string.
     */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("hostName", getSnmpHost())
                .add("securityLevel", securityLevel)
                .add("securityName", securityName)
                .add("authProtocol", authProtocol)
                .add("authPassword", authPassword)
                .add("privProtocol", privProtocol)
                .add("privPassword", privPassword)
                .add("contextName", contextName)
                .toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        DefaultSnmpv3Device otherv3Device = (DefaultSnmpv3Device) obj;
        return Objects.equals(securityLevel, otherv3Device.securityLevel) &&
                Objects.equals(securityName, otherv3Device.securityName) &&
                Objects.equals(getSnmpHost(), otherv3Device.getSnmpHost());
    }

    @Override
    public int hashCode() {
        return Objects.hash(securityLevel, securityName, getSnmpHost());
    }
}
