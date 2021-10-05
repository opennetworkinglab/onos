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

package org.onosproject.snmp;


/**
 * Abstraction a Snmpv3 Device.
 */
public interface Snmpv3Device extends SnmpDevice {

    /**
     * Retrieves the security name of SNMPv3 device.
     *
     * @return security name
     */
    String getSecurityName();

    /**
     * Retrieves the security level of SNMPv3 device.
     *
     * @return security level
     */
    String getSecurityLevel();

    /**
     * Retrieves the authentication protocol of SNMPv3 device.
     *
     * @return authentication protocol
     */
    String getAuthProtocol();

    /**
     * Retrieves the authentication password of SNMPv3 device.
     *
     * @return authentication password
     */
    String getAuthPassword();

    /**
     * Retrieves the privacy protocol of SNMPv3 device.
     *
     * @return privacy protocol
     */
    String getPrivProtocol();

    /**
     * Retrieves the privacy password of SNMPv3 device.
     *
     * @return privacy password
     */
    String getPrivPassword();

    /**
     * Retrieves the context name of SNMPv3 device.
     *
     * @return context name
     */
    String getContextName();

}
