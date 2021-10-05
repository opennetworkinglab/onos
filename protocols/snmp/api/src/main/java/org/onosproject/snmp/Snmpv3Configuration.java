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

import com.btisystems.pronx.ems.core.snmp.ISnmpConfiguration;
import org.onlab.packet.IpAddress;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.smi.OID;
import org.snmp4j.util.PDUFactory;

import java.io.Serializable;

/**
 * Abstraction of SNMPv3 configuration.
 */
public interface Snmpv3Configuration extends Serializable, ISnmpConfiguration {

    /**
     * Returns the ip address.
     *
     * @return ip address
     */
    IpAddress getAddress();

    /**
     * Returns the snmpv3 security name of the device.
     * the SNMPv3 username.
     *
     * @return security name
     */
    String getSecurityName();

    /**
     * Returns the snmpv3 security level of the device.
     * the security level is noAuthNoPriv or authNoPriv or authPriv
     *
     * @return security level
     */
    SecurityLevel getSecurityLevel();

    /**
     * Returns the snmpv3 authentication protocol of the device.
     * the authentication method (either MD5 or SHA)
     *
     * @return authentication protocol
     */
    OID getAuthenticationProtocol();

    /**
     * Returns the snmpv3 authentication password of the device.
     * the authentication password must be at least eight characters long
     *
     * @return authentication password
     */
    String getAuthenticationPassword();

    /**
     * Returns the snmpv3 privacy protocol of the device.
     * the privacy method (either AES or DES)
     *
     * @return privacy protocol
     */
    OID getPrivacyProtocol();

    /**
     * Returns the snmpv3 privacy password of the device.
     * the privacy password must be at least eight characters long
     *
     * @return privacy password
     */
    String getPrivacyPassword();

    /**
     * Returns the snmpv3 authoritative engine id of the device.
     *
     * @return authoritative engine id
     */
    byte[] getAuthoritativeEngineId();

    /**
     * Returns the snmpv3 context name of the device.
     * An SNMP context name or "context" in short,
     * is a collection of management information accessible by an SNMP entity.
     * An item of management information may exist in more than one context.
     * An SNMP entity potentially has access to many contexts. In other words,
     * if a management information has been defined under certain context by an SNMPv3 entity,
     * then any management application can access that information by giving that context name.
     * The "context name" is an octet string, which has at least one management information
     *
     * @return snmpv3 context name
     */
    String getContextName();

    /**
     * Create snmp session PDU factory.
     *
     * @return session PDU factory
     */
    PDUFactory createPduFactory();

    /**
     * Remove Snmp user security model when close connection to device.
     */
    void removeUsm();

}
