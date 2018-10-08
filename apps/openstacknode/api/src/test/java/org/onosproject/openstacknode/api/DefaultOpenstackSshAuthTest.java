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
 * Unit tests for DefaultOpenstackSshAuth.
 */
public class DefaultOpenstackSshAuthTest {

    private static final String ID_1 = "sdn1";
    private static final String ID_2 = "sdn2";
    private static final String PASSWORD_1 = "password1";
    private static final String PASSWORD_2 = "password2";

    private static final OpenstackSshAuth OS_SSH_AUTH_1 =
            createOpenstackSshAuth(ID_1, PASSWORD_1);
    private static final OpenstackSshAuth OS_SSH_AUTH_2 =
            createOpenstackSshAuth(ID_2, PASSWORD_2);
    private static final OpenstackSshAuth OS_SSH_AUTH_3 =
            createOpenstackSshAuth(ID_1, PASSWORD_1);


    private static OpenstackSshAuth createOpenstackSshAuth(String id, String password) {
        return DefaultOpenstackSshAuth.builder()
                .id(id)
                .password(password)
                .build();
    }

    @Test
    public void testEquality() {
        new EqualsTester().addEqualityGroup(OS_SSH_AUTH_1, OS_SSH_AUTH_3)
                .addEqualityGroup(OS_SSH_AUTH_2)
                .testEquals();
    }

    @Test
    public void testConstruction() {
        OpenstackSshAuth auth = OS_SSH_AUTH_1;

        assertThat(auth.id(), is("sdn1"));
        assertThat(auth.password(), is("password1"));
    }
}
