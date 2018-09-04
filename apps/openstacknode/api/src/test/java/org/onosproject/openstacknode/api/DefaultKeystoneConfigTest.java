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
import static org.onosproject.openstacknode.api.OpenstackAuth.Perspective.PUBLIC;
import static org.onosproject.openstacknode.api.OpenstackAuth.Protocol.HTTP;

/**
 * Unit tests for DefaultKeystoneConfig.
 */
public final class DefaultKeystoneConfigTest {

    private static final String ENDPOINT_1 = "192.168.0.10:35357/v2.0";
    private static final String ENDPOINT_2 = "192.168.0.11:80/v3";

    private static final String USERNAME = "admin";
    private static final String PASSWORD = "nova";
    private static final String PROJECT = "admin";
    private static final String VERSION_2 = "v2.0";
    private static final String VERSION_3 = "v3";

    private static final OpenstackAuth AUTHENTICATION_1 = createAuthv2();
    private static final OpenstackAuth AUTHENTICATION_2 = createAuthv3();

    private KeystoneConfig config1;
    private KeystoneConfig sameAsConfig1;
    private KeystoneConfig config2;

    private static OpenstackAuth createAuthv2() {
        return DefaultOpenstackAuth.builder()
                .username(USERNAME)
                .password(PASSWORD)
                .project(PROJECT)
                .version(VERSION_2)
                .perspective(PUBLIC)
                .protocol(HTTP)
                .build();
    }

    private static OpenstackAuth createAuthv3() {
        return DefaultOpenstackAuth.builder()
                .username(USERNAME)
                .password(PASSWORD)
                .project(PROJECT)
                .version(VERSION_3)
                .perspective(PUBLIC)
                .protocol(HTTP)
                .build();
    }

    /**
     * Tests class immutability.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(DefaultKeystoneConfig.class);
    }

    /**
     * Initial setup for this unit test.
     */
    @Before
    public void setUp() {
        config1 = DefaultKeystoneConfig.builder()
                        .endpoint(ENDPOINT_1)
                        .authentication(AUTHENTICATION_1)
                        .build();

        sameAsConfig1 = DefaultKeystoneConfig.builder()
                        .endpoint(ENDPOINT_1)
                        .authentication(AUTHENTICATION_1)
                        .build();

        config2 = DefaultKeystoneConfig.builder()
                        .endpoint(ENDPOINT_2)
                        .authentication(AUTHENTICATION_2)
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
        KeystoneConfig config = config1;

        assertEquals(config.endpoint(), ENDPOINT_1);
        assertEquals(config.authentication(), AUTHENTICATION_1);
    }
}
