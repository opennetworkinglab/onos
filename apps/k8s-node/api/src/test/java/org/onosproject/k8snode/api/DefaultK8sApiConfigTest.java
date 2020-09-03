/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.k8snode.api;

import com.google.common.testing.EqualsTester;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onosproject.k8snode.api.K8sApiConfig.Scheme;

import static junit.framework.TestCase.assertEquals;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;
import static org.onosproject.k8snode.api.K8sApiConfig.Mode;
import static org.onosproject.k8snode.api.K8sApiConfig.Mode.NORMAL;
import static org.onosproject.k8snode.api.K8sApiConfig.Mode.PASSTHROUGH;
import static org.onosproject.k8snode.api.K8sApiConfig.Scheme.HTTP;
import static org.onosproject.k8snode.api.K8sApiConfig.Scheme.HTTPS;
import static org.onosproject.k8snode.api.K8sApiConfig.State.CONNECTED;
import static org.onosproject.k8snode.api.K8sApiConfig.State.DISCONNECTED;

/**
 * Unit tests for DefaultK8sApiConfig.
 */
public final class DefaultK8sApiConfigTest {

    private static final String CLUSTER_NAME = "kubernetes";

    private static final int SEGMENT_ID_1 = 1;
    private static final int SEGMENT_ID_2 = 2;

    private static final IpPrefix EXT_NETWORK_CIDR = IpPrefix.valueOf("192.168.200.0/0");

    private static final Scheme SCHEME_1 = HTTP;
    private static final Scheme SCHEME_2 = HTTPS;

    private static final Mode MODE_1 = NORMAL;
    private static final Mode MODE_2 = PASSTHROUGH;

    private static final IpAddress IP_ADDRESS_1 = IpAddress.valueOf("192.168.0.200");
    private static final IpAddress IP_ADDRESS_2 = IpAddress.valueOf("192.168.0.201");

    private static final int PORT_1 = 6443;
    private static final int PORT_2 = 443;

    private static final String TOKEN_1 = "token1";
    private static final String TOKEN_2 = "token2";

    private static final String CA_CERT_DATA_1 = "caCertData1";
    private static final String CA_CERT_DATA_2 = "caCertData2";

    private static final String CLIENT_CERT_DATA_1 = "clientCertData1";
    private static final String CLIENT_CERT_DATA_2 = "clientCertData2";

    private static final String CLIENT_KEY_DATA_1 = "clientKeyData1";
    private static final String CLIENT_KEY_DATA_2 = "clientKeyData2";

    private static final boolean DVR_1 = true;
    private static final boolean DVR_2 = false;

    private K8sApiConfig config1;
    private K8sApiConfig sameAsConfig1;
    private K8sApiConfig config2;

    /**
     * Tests class immutability.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(DefaultK8sApiConfig.class);
    }

    /**
     * Initial setup for this unit test.
     */
    @Before
    public void setUp() {
        config1 = DefaultK8sApiConfig.builder()
                .clusterName(CLUSTER_NAME)
                .segmentId(SEGMENT_ID_1)
                .extNetworkCidr(EXT_NETWORK_CIDR)
                .mode(NORMAL)
                .scheme(SCHEME_1)
                .ipAddress(IP_ADDRESS_1)
                .port(PORT_1)
                .state(CONNECTED)
                .token(TOKEN_1)
                .caCertData(CA_CERT_DATA_1)
                .clientCertData(CLIENT_CERT_DATA_1)
                .clientKeyData(CLIENT_KEY_DATA_1)
                .dvr(DVR_1)
                .build();

        sameAsConfig1 = DefaultK8sApiConfig.builder()
                .clusterName(CLUSTER_NAME)
                .segmentId(SEGMENT_ID_1)
                .extNetworkCidr(EXT_NETWORK_CIDR)
                .mode(NORMAL)
                .scheme(SCHEME_1)
                .ipAddress(IP_ADDRESS_1)
                .port(PORT_1)
                .state(CONNECTED)
                .token(TOKEN_1)
                .caCertData(CA_CERT_DATA_1)
                .clientCertData(CLIENT_CERT_DATA_1)
                .clientKeyData(CLIENT_KEY_DATA_1)
                .dvr(DVR_1)
                .build();

        config2 = DefaultK8sApiConfig.builder()
                .clusterName(CLUSTER_NAME)
                .segmentId(SEGMENT_ID_2)
                .extNetworkCidr(EXT_NETWORK_CIDR)
                .mode(PASSTHROUGH)
                .scheme(SCHEME_2)
                .ipAddress(IP_ADDRESS_2)
                .port(PORT_2)
                .state(DISCONNECTED)
                .token(TOKEN_2)
                .caCertData(CA_CERT_DATA_2)
                .clientCertData(CLIENT_CERT_DATA_2)
                .clientKeyData(CLIENT_KEY_DATA_2)
                .dvr(DVR_2)
                .build();
    }

    /**
     * Tests object equality.
     */
    @Test
    public void testEquality() {
        new EqualsTester().addEqualityGroup(config1, sameAsConfig1)
                .addEqualityGroup(config2)
                .testEquals();
    }

    /**
     * Test object construction.
     */
    @Test
    public void testConstruction() {
        K8sApiConfig config = config1;

        assertEquals(CLUSTER_NAME, config.clusterName());
        assertEquals(SEGMENT_ID_1, config.segmentId());
        assertEquals(EXT_NETWORK_CIDR, config.extNetworkCidr());
        assertEquals(SCHEME_1, config.scheme());
        assertEquals(MODE_1, config.mode());
        assertEquals(IP_ADDRESS_1, config.ipAddress());
        assertEquals(PORT_1, config.port());
        assertEquals(CONNECTED, config.state());
        assertEquals(TOKEN_1, config.token());
        assertEquals(CA_CERT_DATA_1, config.caCertData());
        assertEquals(CLIENT_CERT_DATA_1, config.clientCertData());
        assertEquals(CLIENT_KEY_DATA_1, config.clientKeyData());
        assertEquals(DVR_1, config.dvr());
    }
}
