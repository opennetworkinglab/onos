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
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests for DefaultOpenstackAuth.
 */
public class DefaultOpenstackAuthTest {

    private static final String USERNAME = "admin";
    private static final String PASSWORD = "nova";
    private static final String PROJECT = "admin";
    private static final String VERSION_1 = "v2.0";
    private static final String VERSION_2 = "v3";
    private static final OpenstackAuth.Protocol PROTOCOL_1 = OpenstackAuth.Protocol.HTTP;
    private static final OpenstackAuth.Protocol PROTOCOL_2 = OpenstackAuth.Protocol.HTTPS;

    private static final OpenstackAuth OS_AUTH_1 =
                         createOpenstackAuth(VERSION_1, PROTOCOL_1);
    private static final OpenstackAuth OS_AUTH_2 =
                         createOpenstackAuth(VERSION_2, PROTOCOL_2);
    private static final OpenstackAuth OS_AUTH_3 =
                         createOpenstackAuth(VERSION_1, PROTOCOL_1);

    private static OpenstackAuth createOpenstackAuth(String version,
                                              OpenstackAuth.Protocol protocol) {
        return DefaultOpenstackAuth.builder()
                .version(version)
                .protocol(protocol)
                .username(USERNAME)
                .password(PASSWORD)
                .project(PROJECT)
                .perspective(OpenstackAuth.Perspective.PUBLIC)
                .build();
    }

    /**
     * Tests object equality.
     */
    @Test
    public void testEquality() {
        new EqualsTester().addEqualityGroup(OS_AUTH_1, OS_AUTH_3)
                .addEqualityGroup(OS_AUTH_2)
                .testEquals();
    }

    /**
     * Test object construction.
     */
    @Test
    public void testConstruction() {
        OpenstackAuth auth = OS_AUTH_1;

        assertThat(auth.version(), is("v2.0"));
        assertThat(auth.protocol(), is(OpenstackAuth.Protocol.HTTP));
        assertThat(auth.username(), is("admin"));
        assertThat(auth.password(), is("nova"));
        assertThat(auth.project(), is("admin"));
        assertThat(auth.perspective(), is(OpenstackAuth.Perspective.PUBLIC));
    }
}
