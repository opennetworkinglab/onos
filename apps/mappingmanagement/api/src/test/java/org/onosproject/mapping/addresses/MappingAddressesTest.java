/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.mapping.addresses;

import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;
import static org.onlab.junit.UtilityClassChecker.assertThatClassIsUtility;

/**
 * Unit tests for various mapping address implementation classes.
 */
public class MappingAddressesTest {

    private final String asn1 = "1";
    private final String asn2 = "2";
    private MappingAddress asMa1 = MappingAddresses.asMappingAddress(asn1);
    private MappingAddress sameAsAsMa1 = MappingAddresses.asMappingAddress(asn1);
    private MappingAddress asMa2 = MappingAddresses.asMappingAddress(asn2);

    private final String dn1 = "1";
    private final String dn2 = "2";
    private MappingAddress dnMa1 = MappingAddresses.dnMappingAddress(dn1);
    private MappingAddress sameAsDnMa1 = MappingAddresses.dnMappingAddress(dn1);
    private MappingAddress dnMa2 = MappingAddresses.dnMappingAddress(dn2);

    private static final String MAC1 = "00:00:00:00:00:01";
    private static final String MAC2 = "00:00:00:00:00:02";
    private MacAddress mac1 = MacAddress.valueOf(MAC1);
    private MacAddress mac2 = MacAddress.valueOf(MAC2);
    private MappingAddress ethMa1 = MappingAddresses.ethMappingAddress(mac1);
    private MappingAddress sameAsEthMa1 = MappingAddresses.ethMappingAddress(mac1);
    private MappingAddress ethMa2 = MappingAddresses.ethMappingAddress(mac2);

    private static final String IP1 = "1.2.3.4/24";
    private static final String IP2 = "5.6.7.8/24";
    private static final String IPV61 = "fe80::1/64";
    private static final String IPV62 = "fc80::2/64";
    private IpPrefix ip1 = IpPrefix.valueOf(IP1);
    private IpPrefix ip2 = IpPrefix.valueOf(IP2);
    private IpPrefix ipv61 = IpPrefix.valueOf(IPV61);
    private IpPrefix ipv62 = IpPrefix.valueOf(IPV62);
    private MappingAddress ipMa1 = MappingAddresses.ipv4MappingAddress(ip1);
    private MappingAddress sameAsIpMa1 = MappingAddresses.ipv4MappingAddress(ip1);
    private MappingAddress ipMa2 = MappingAddresses.ipv4MappingAddress(ip2);
    private MappingAddress ipv6Ma1 = MappingAddresses.ipv6MappingAddress(ipv61);
    private MappingAddress sameAsIpv6Ma1 = MappingAddresses.ipv6MappingAddress(ipv61);
    private MappingAddress ipv6Ma2 = MappingAddresses.ipv6MappingAddress(ipv62);

    /**
     * Checks that a MappingAddress object has the proper type, and then converts
     * it to the proper type.
     *
     * @param address MappingAddress object to convert
     * @param type    Enumerated type value for the MappingAddress class
     * @param clazz   Desired MappingAddress class
     * @param <T>     The type the caller wants returned
     * @return converted object
     */
    @SuppressWarnings("unchecked")
    private <T> T checkAndConvert(MappingAddress address, MappingAddress.Type type, Class clazz) {
        assertThat(address, is(notNullValue()));
        assertThat(address.type(), is(equalTo(type)));
        assertThat(address, instanceOf(clazz));
        return (T) address;
    }

    /**
     * Checks the equals() and toString() methods of a MappingAddress class.
     *
     * @param c1      first object to compare
     * @param c1match object that should be equal to the first
     * @param c2      object that should not be equal to the first
     * @param <T>     type of the arguments
     */
    private <T extends MappingAddress> void checkEqualsAndToString(T c1,
                                                                   T c1match,
                                                                   T c2) {
        new EqualsTester()
                .addEqualityGroup(c1, c1match)
                .addEqualityGroup(c2)
                .testEquals();
    }

    /**
     * Checks that the MappingAddresses class is a valid utility class.
     */
    @Test
    public void testMappingAddressesUtility() {
        assertThatClassIsUtility(MappingAddresses.class);
    }

    /**
     * Checks that the mapping address implementations are immutable.
     */
    @Test
    public void testMappingAddressesImmutability() {
        assertThatClassIsImmutable(ASMappingAddress.class);
        assertThatClassIsImmutable(DNMappingAddress.class);
        assertThatClassIsImmutable(EthMappingAddress.class);
        assertThatClassIsImmutable(ExtensionMappingAddressWrapper.class);
        assertThatClassIsImmutable(IPMappingAddress.class);
    }

    /**
     * Tests the asMappingAddress method.
     */
    @Test
    public void testAsMappingAddressMethod() {
        String asn = "1";
        MappingAddress mappingAddress = MappingAddresses.asMappingAddress(asn);
        ASMappingAddress asMappingAddress =
                checkAndConvert(mappingAddress,
                                MappingAddress.Type.AS,
                                ASMappingAddress.class);
        assertThat(asMappingAddress.asNumber(), is(equalTo(asn)));
    }

    /**
     * Tests the equals() method of the ASMappingAddress class.
     */
    @Test
    public void testAsMappingAddressEquals() {
        checkEqualsAndToString(asMa1, sameAsAsMa1, asMa2);
    }

    /**
     * Tests the dnMappingAddress method.
     */
    @Test
    public void testDnMappingAddressMethod() {
        String dn = "1";
        MappingAddress mappingAddress = MappingAddresses.dnMappingAddress(dn);
        DNMappingAddress dnMappingAddress =
                checkAndConvert(mappingAddress,
                                MappingAddress.Type.DN,
                                DNMappingAddress.class);
        assertThat(dnMappingAddress.name(), is(equalTo(dn)));
    }

    /**
     * Tests the equals() method of the DNMappingAddress class.
     */
    @Test
    public void testDnMappingAddressEquals() {
        checkEqualsAndToString(dnMa1, sameAsDnMa1, dnMa2);
    }

    /**
     * Tests the ethMappingAddress method.
     */
    @Test
    public void testEthMappingAddressMethod() {
        MacAddress mac = MacAddress.valueOf("00:00:00:00:00:01");
        MappingAddress mappingAddress = MappingAddresses.ethMappingAddress(mac);
        EthMappingAddress ethMappingAddress =
                checkAndConvert(mappingAddress,
                                MappingAddress.Type.ETH,
                                EthMappingAddress.class);
        assertThat(ethMappingAddress.mac(), is(equalTo(mac)));
    }

    /**
     * Tests the equals() method of the EthMappingAddress class.
     */
    @Test
    public void testEthMappingAddressEquals() {
        checkEqualsAndToString(ethMa1, sameAsEthMa1, ethMa2);
    }

    /**
     * Tests the ipv4MappingAddress method.
     */
    @Test
    public void testIpv4MappingAddressMethod() {
        IpPrefix ip = IpPrefix.valueOf("1.2.3.4/24");
        MappingAddress mappingAddress = MappingAddresses.ipv4MappingAddress(ip);
        IPMappingAddress ipMappingAddress =
                checkAndConvert(mappingAddress,
                                MappingAddress.Type.IPV4,
                                IPMappingAddress.class);
        assertThat(ipMappingAddress.ip(), is(equalTo(ip)));
    }

    /**
     * Tests the equals() method of the IPMappingAddress class.
     */
    @Test
    public void testIpv4MappingAddressEquals() {
        checkEqualsAndToString(ipMa1, sameAsIpMa1, ipMa2);
    }

    /**
     * Tests the ipv6MappingAddress method.
     */
    @Test
    public void testIpv6MappingAddressMethod() {
        IpPrefix ipv6 = IpPrefix.valueOf("fe80::1/64");
        MappingAddress mappingAddress = MappingAddresses.ipv6MappingAddress(ipv6);
        IPMappingAddress ipMappingAddress =
                checkAndConvert(mappingAddress,
                                MappingAddress.Type.IPV6,
                                IPMappingAddress.class);
        assertThat(ipMappingAddress.ip(), is(equalTo(ipv6)));
    }

    /**
     * Tests the equals() method of the IPMappingAddress class.
     */
    @Test
    public void testIpv6MappingAddressEquals() {
        checkEqualsAndToString(ipv6Ma1, sameAsIpv6Ma1, ipv6Ma2);
    }
}
