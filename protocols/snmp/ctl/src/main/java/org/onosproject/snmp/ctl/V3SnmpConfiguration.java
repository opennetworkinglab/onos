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
import com.btisystems.pronx.ems.core.snmp.SnmpConfiguration;
import org.onosproject.snmp.SnmpException;
import org.onosproject.snmp.Snmpv3Configuration;
import org.slf4j.Logger;
import org.onlab.packet.IpAddress;

import org.snmp4j.MessageDispatcher;
import org.snmp4j.MessageDispatcherImpl;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.TransportMapping;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.OctetString;
import org.snmp4j.util.MultiThreadedMessageDispatcher;
import org.snmp4j.util.ThreadPool;
import org.snmp4j.UserTarget;
import org.snmp4j.ScopedPDU;
import org.snmp4j.mp.MPv3;

import org.snmp4j.security.PrivDES;
import org.snmp4j.security.AuthMD5;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.security.SecurityModels;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.security.USM;
import org.snmp4j.security.UsmUser;
import org.snmp4j.security.PrivAES128;
import org.snmp4j.security.PrivAES256;
import org.snmp4j.security.Priv3DES;
import org.snmp4j.security.PrivAES192;
import org.snmp4j.security.AuthSHA;
import org.snmp4j.security.nonstandard.PrivAES192With3DESKeyExtension;
import org.snmp4j.security.nonstandard.PrivAES256With3DESKeyExtension;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.util.DefaultPDUFactory;
import org.snmp4j.util.PDUFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of the SNMPv3 configuration.
 */
public class V3SnmpConfiguration extends SnmpConfiguration implements Snmpv3Configuration {

    private IpAddress address;

    private String securityName;

    private SecurityLevel securityLevel;

    private OID authProtocol;

    private String authPassword;

    private OID privacyProtocol;

    private String privacyPassword;

    private String contextName;

    private byte[] authoritativeEngineId;

    private static Snmp snmp;

    private static final USM USM_USER;

    private static final int ENGINE_BOOT = 0;

    private static final int PASS_CODE_LENGTH = 8;

    private static final int DISCOVERY_TIMEOUT = 5000;

    private static final String SLASH = "/";

    private static final byte[] LOCALENGINEID = MPv3.createLocalEngineID();

    private final Logger log = getLogger(V3SnmpConfiguration.class);

    static {
        SecurityProtocols securityProtocols = SecurityProtocols.getInstance();
        securityProtocols.addPrivacyProtocol(new PrivAES128());
        securityProtocols.addPrivacyProtocol(new
                PrivAES192With3DESKeyExtension());
        securityProtocols.addPrivacyProtocol(new
                PrivAES256With3DESKeyExtension());
        securityProtocols.addPrivacyProtocol(new PrivDES());
        securityProtocols.addPrivacyProtocol(new Priv3DES());

        USM_USER = new USM(SecurityProtocols.getInstance(),
                new OctetString(LOCALENGINEID), ENGINE_BOOT);
        USM_USER.setEngineDiscoveryEnabled(true);
        SecurityModels.getInstance().addSecurityModel(USM_USER);
    }

    public V3SnmpConfiguration() {
        this.setVersion(SnmpConstants.version3);
    }

    private V3SnmpConfiguration(Builder builder) {
        this();
        this.address = builder.address;
        this.securityName = builder.securityName;
        this.securityLevel = builder.securityLevel;
        this.authProtocol = builder.authProtocol;
        this.authPassword = builder.authPassword;
        this.privacyProtocol = builder.privacyProtocol;
        this.privacyPassword = builder.privacyPassword;
        this.contextName = builder.contextName;
    }

    @Override
    public Target createTarget(Address address) {
        setUsm();
        UserTarget target = new UserTarget();
        target.setSecurityLevel(this.getSecurityLevel().getSnmpValue());
        target.setSecurityName(new OctetString(this.getSecurityName()));
        target.setVersion(this.getVersion());
        target.setAddress(address);
        target.setRetries(this.getRetries());
        target.setTimeout(this.getTimeout());
        target.setAuthoritativeEngineID(this.getAuthoritativeEngineId());
        return target;
    }

    /**
     * Create snmp session PDU factory.
     *
     * @return session PDU factory
     */
    @Override
    public PDUFactory createPduFactory() {
        DefaultPDUFactory pduFactory = new DefaultPDUFactory(PDU.GETBULK);
        if (!StringUtils.isEmpty(this.getContextName())) {
            pduFactory.setContextName(new OctetString(this.getContextName()));
        }
        return pduFactory;
    }

    @Override
    public PDU createPDU(int type) {
        ScopedPDU pdu = new ScopedPDU();
        pdu.setType(type);
        if (!StringUtils.isEmpty(this.getContextName())) {
            pdu.setContextName(new OctetString(this.getContextName()));
        }
        switch (type) {
            case PDU.GETBULK:
                pdu.setMaxRepetitions(this.getMaxRepetitions());
                pdu.setNonRepeaters(this.getNonRepeaters());
                break;
            default:
                log.debug("Not setting up non-default configuration for PDU type {}.", type);
        }

        return pdu;
    }

    /**
     * Create snmp session to the device.
     *
     * @return snmp session object
     */
    @Override
    public Snmp createSnmpSession(TransportMapping transportMapping) throws IOException {
        synchronized (USM_USER) {
            if (Objects.isNull(snmp)) {
                ThreadPool threadPool = ThreadPool.create("SnmpDispatcherPool", getDispatcherPoolSize());
                MessageDispatcher mtDispatcher = new MultiThreadedMessageDispatcher(
                        threadPool, new MessageDispatcherImpl());

                // Add message processing models
                mtDispatcher.addMessageProcessingModel(new MPv3());

                snmp = new Snmp(mtDispatcher, transportMapping);

                snmp.listen();
            }
        }

        return snmp;
    }

    /**
     * Set User Security Model to the snmpv3 session.
     * SNMPv3 is a security model in which an authentication
     * strategy is set up for a user in which the user resides.
     * Security level is the permitted level of security within a security model.
     * A combination of a security model and a security level determines
     * which security mechanism is used when handling an SNMP packet.
     */
    private void setUsm() {

        OctetString securityName = new OctetString(this.getSecurityName());
        OctetString authPassphrase = StringUtils.isEmpty(this.getAuthenticationPassword()) ?
                null : new OctetString(this.getAuthenticationPassword());
        OctetString privPassphrase = StringUtils.isEmpty(this.getPrivacyPassword()) ?
                null : new OctetString(this.getPrivacyPassword());


        discoverAuthoritativeEngineId();

        if (USM_USER.hasUser(new OctetString(this.getAuthoritativeEngineId()), securityName)) {
            return;
        }

        USM_USER.addUser(securityName,
                new UsmUser(securityName,
                        this.getAuthenticationProtocol(),
                        authPassphrase,
                        this.getPrivacyProtocol(),
                        privPassphrase));

    }

    /**
     * Remove Snmp user security model when close connection to device.
     */
    @Override
    public void removeUsm() {
        OctetString securityName = new OctetString(this.getSecurityName());
        OctetString engineId = new OctetString(this.getAuthoritativeEngineId());
        if (USM_USER.hasUser(engineId, securityName)) {
            USM_USER.removeAllUsers(securityName, engineId);
        }

    }

    /**
     * Discover snmp agent authoritative engineId.
     * The Engine ID is used by SNMPv3 entities to uniquely identify them.
     * An SNMP agent is considered an authoritative SNMP engine.
     * This means that the agent responds to incoming messages (Get, GetNext, GetBulk, Set)
     * and sends trap messages to a manager.
     * The agent's local information is encapsulated in fields in the message.
     * <p>
     * Each SNMP agent maintains local information that is used in SNMPv3 message exchanges.
     * The default SNMP Engine ID is comprised of the enterprise number and the default MAC address.
     * This engine ID must be unique for the administrative domain,
     * so that no two devices in a network have the same engine ID.
     */
    private void discoverAuthoritativeEngineId() {
        this.authoritativeEngineId = Optional.ofNullable(snmp.discoverAuthoritativeEngineID(
                        getTransportAddress(), DISCOVERY_TIMEOUT))
                .orElseThrow(() -> new SnmpException(String.format("Snmp agent %s is not configured with" +
                                " Snmpv3 user or not reachable",
                        getAddress().toString())));
    }

    /**
     * Returns the security level of SNMPv3 device.
     *
     * @return security level
     */
    @Override
    public SecurityLevel getSecurityLevel() {
        return this.securityLevel;
    }

    /**
     * Returns the ip address.
     *
     * @return ip address
     */
    @Override
    public IpAddress getAddress() {
        return this.address;
    }

    /**
     * Returns the security name of SNMPv3 device.
     *
     * @return security name
     */
    @Override
    public String getSecurityName() {
        return this.securityName;
    }

    /**
     * Returns the authentication password of SNMPv3 device.
     *
     * @return authentication password
     */
    @Override
    public String getAuthenticationPassword() {
        return this.authPassword;
    }

    /**
     * Returns the authentication protocol of SNMPv3 device.
     *
     * @return authentication protocol
     */
    @Override
    public OID getAuthenticationProtocol() {
        return this.authProtocol;
    }

    /**
     * Returns the snmpv3 privacy password of the device.
     *
     * @return privacy password
     */
    @Override
    public String getPrivacyPassword() {
        return this.privacyPassword;
    }

    /**
     * Returns the snmpv3 privacy protocol of the device.
     *
     * @return privacy protocol
     */
    @Override
    public OID getPrivacyProtocol() {
        return this.privacyProtocol;
    }

    /**
     * Returns the snmpv3 context name of the device.
     *
     * @return snmpv3 context name
     */
    @Override
    public String getContextName() {
        return this.contextName;
    }

    /**
     * Returns the snmpv3 authoritative engine id of the device.
     *
     * @return authoritative engine id
     */
    @Override
    public byte[] getAuthoritativeEngineId() {
        return this.authoritativeEngineId;
    }


    private Address getTransportAddress() {
        return new UdpAddress(getAddress().toString() + SLASH + getPort());
    }

    /**
     * Creates a new v3snmp configuration builder.
     *
     * @return new v3snmp configuration builder
     */
    public static V3SnmpConfiguration.Builder builder() {
        return new V3SnmpConfiguration.Builder();
    }

    /**
     * Convert the Snmpv3 configuration to string.
     */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("address", address)
                .add("securityName", securityName)
                .add("securityLevel", securityLevel)
                .add("authProtocol", authProtocol)
                .add("authPassword", authPassword)
                .add("privacyProtocol", privacyProtocol)
                .add("privacyPassword", privacyPassword)
                .add("contextName", contextName)
                .add("authoritativeEngineId", Arrays.toString(authoritativeEngineId))
                .toString();
    }

    /**
     * Facility for gradually building snmpv3 configuration.
     */
    public static final class Builder {

        private IpAddress address;

        private String securityName;

        private SecurityLevel securityLevel;

        private OID authProtocol;

        private String authPassword;

        private OID privacyProtocol;

        private String privacyPassword;

        private String contextName;


        // Private construction is forbidden.
        private Builder() {
        }

        /**
         * Set ip address of the device.
         *
         * @param address ip address
         * @return This builder
         */
        public Builder setAddress(String address) {
            this.address = IpAddress.valueOf(address);
            return this;
        }

        /**
         * Set the security level of the SNMPv3 device.
         *
         * @param securityLevel securityLevel of the SNMPv3 device
         * @return This builder
         */
        public Builder setSecurityLevel(String securityLevel) {
            this.securityLevel = getSecurityLevel(securityLevel);
            return this;
        }

        /**
         * Set the security name of the SNMPv3 device.
         *
         * @param securityName securityName of the SNMPv3 device.
         * @return This builder
         */
        public Builder setSecurityName(String securityName) {
            this.securityName = securityName;
            return this;
        }

        /**
         * Set the authentication password of the SNMPv3 device.
         *
         * @param authPassword authPassword of the SNMPv3 device
         * @return This builder
         */
        public Builder setAuthenticationPassword(String authPassword) {
            this.authPassword = StringUtils.isEmpty(authPassword) ? null : authPassword;
            return this;
        }

        /**
         * Set the authentication protocol of the SNMPv3 device.
         *
         * @param authProtocol authProtocol of the SNMPv3 device
         * @return This builder
         */
        public Builder setAuthenticationProtocol(String authProtocol) {
            this.authProtocol = getAuthProtocol(authProtocol);
            return this;
        }

        /**
         * Set the privacy password of the SNMPv3 device.
         *
         * @param privacyPassword privacy protocol of the SNMPv3 device
         * @return This builder
         */
        public Builder setPrivacyPassword(String privacyPassword) {
            this.privacyPassword = StringUtils.isEmpty(privacyPassword) ? null : privacyPassword;
            return this;
        }

        /**
         * Set the privacy protocol of the SNMPv3 device.
         *
         * @param privacyProtocol privacyProtocol of the SNMPv3 device
         * @return This builder
         */
        public Builder setPrivacyProtocol(String privacyProtocol) {
            this.privacyProtocol = getPrivProtocol(privacyProtocol);
            return this;
        }

        /**
         * Set the context name of the SNMPv3 device.
         *
         * @param contextName context name of the SNMPv3 device
         * @return This builder
         */
        public Builder setContextName(String contextName) {
            this.contextName = contextName;
            return this;
        }

        /**
         * Convert privacy protocol string to privacy protocol object identifier.
         *
         * @param priv privacy protocol string
         * @return privacy protocol object identifier
         */
        private OID getPrivProtocol(String priv) {

            switch (priv) {

                case "DES":
                    return PrivDES.ID;

                case "AES":
                case "AES128":
                    return PrivAES128.ID;

                case "AES192":
                    return PrivAES192.ID;

                case "AES256":
                    return PrivAES256.ID;

                case "3DES":
                case "DESEDE":
                    return Priv3DES.ID;

                default:
                    throw new SnmpException("Invalid privacy protocol");

            }
        }

        /**
         * Convert authentication string to authentication protocol object identifier.
         *
         * @param auth privacy protocol string
         * @return authentication protocol object identifier
         */
        private OID getAuthProtocol(String auth) {

            switch (auth) {

                case "MD5":
                    return AuthMD5.ID;

                case "SHA":
                    return AuthSHA.ID;

                default:
                    throw new SnmpException("Invalid Authentication protocol");

            }
        }

        /**
         * Convert security level string to security level number.
         *
         * @param securityLevel snmpv3 security level string
         * @return snmpv3 security level
         */
        private SecurityLevel getSecurityLevel(String securityLevel) {

            switch (securityLevel) {

                case "noAuthNoPriv":
                    return SecurityLevel.noAuthNoPriv;

                case "authNoPriv":
                    return SecurityLevel.authNoPriv;

                case "authPriv":
                    return SecurityLevel.authPriv;

                default:
                    throw new SnmpException("Invalid Security level");
            }
        }

        /**
         * Validate snmpv3 configuration.
         */
        private void validateUsmConfiguration() {

            validateSecurityName();

            switch (securityLevel.getSnmpValue()) {

                case SecurityLevel.AUTH_PRIV:
                    validateAuth();
                    validatePriv();
                    return;

                case SecurityLevel.AUTH_NOPRIV:
                    validateAuth();
                    return;

                case SecurityLevel.NOAUTH_NOPRIV:
                    return;

                default:
                    throw new SnmpException("Invalid security level");

            }
        }

        /**
         * Validate snmpv3 authentication protocol and password.
         */
        private void validateAuth() {
            checkNotNull(authProtocol, "Authentication protocol must be provided");
            checkNotNull(authPassword, "Authentication password must be provided");
            checkState(authPassword.length() >= PASS_CODE_LENGTH,
                    "Invalid authentication password");
        }

        private void validatePriv() {
            checkNotNull(privacyProtocol, "Privacy protocol must be provided");
            checkNotNull(privacyPassword, "Privacy password must be provided");
            checkState(privacyPassword.length() >= PASS_CODE_LENGTH,
                    "Invalid privacy password");
        }

        /**
         * Validate snmpv3 privacy protocol and password.
         */
        private void validateSecurityName() {
            checkNotNull(securityName, "Security name must be provided");
            checkState(!privacyPassword.isEmpty(), "Invalid security name");
        }

        /**
         * Create snmpv3 configuration instance.
         *
         * @return v3snmp configuration object
         */
        public V3SnmpConfiguration build() {
            validateUsmConfiguration();
            return new V3SnmpConfiguration(this);
        }

    }

}
