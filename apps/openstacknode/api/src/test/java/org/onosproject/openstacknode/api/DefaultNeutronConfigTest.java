/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.openstacknode.api;

import com.google.common.testing.EqualsTester;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Unit tests for DefaultNeutronConfig.
 */
public class DefaultNeutronConfigTest {

    private static final boolean USE_METADATA_PROXY_1 = true;
    private static final boolean USE_METADATA_PROXY_2 = false;

    private static final String METADATA_PROXY_SECRET_1 = "onos";
    private static final String METADATA_PROXY_SECRET_2 = "cord";

    private static final String NOVA_METADATA_IP_1 = "10.10.10.1";
    private static final String NOVA_METADATA_IP_2 = "20.20.20.2";

    private static final Integer NOVA_METADATA_PORT_1 = 8775;
    private static final Integer NOVA_METADATA_PORT_2 = 8776;

    private NeutronConfig config1;
    private NeutronConfig sameAsConfig1;
    private NeutronConfig config2;

    /**
     * Tests class immutability.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(DefaultNeutronConfig.class);
    }

    /**
     * Initial setup for this unit test.
     */
    @Before
    public void setUp() {
        config1 = DefaultNeutronConfig.builder()
                        .useMetadataProxy(USE_METADATA_PROXY_1)
                        .metadataProxySecret(METADATA_PROXY_SECRET_1)
                        .novaMetadataIp(NOVA_METADATA_IP_1)
                        .novaMetadataPort(NOVA_METADATA_PORT_1)
                        .build();

        sameAsConfig1 = DefaultNeutronConfig.builder()
                        .useMetadataProxy(USE_METADATA_PROXY_1)
                        .metadataProxySecret(METADATA_PROXY_SECRET_1)
                        .novaMetadataIp(NOVA_METADATA_IP_1)
                        .novaMetadataPort(NOVA_METADATA_PORT_1)
                        .build();

        config2 = DefaultNeutronConfig.builder()
                        .useMetadataProxy(USE_METADATA_PROXY_2)
                        .metadataProxySecret(METADATA_PROXY_SECRET_2)
                        .novaMetadataIp(NOVA_METADATA_IP_2)
                        .novaMetadataPort(NOVA_METADATA_PORT_2)
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
        NeutronConfig config = config1;

        assertEquals(config.useMetadataProxy(), USE_METADATA_PROXY_1);
        assertEquals(config.metadataProxySecret(), METADATA_PROXY_SECRET_1);
        assertEquals(config.novaMetadataIp(), NOVA_METADATA_IP_1);
        assertEquals(config.novaMetadataPort(), NOVA_METADATA_PORT_1);
    }
}
