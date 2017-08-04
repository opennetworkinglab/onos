/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.lisp.msg.authentication;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.onosproject.lisp.msg.authentication.LispAuthenticationKeyEnum.NONE;
import static org.onosproject.lisp.msg.authentication.LispAuthenticationKeyEnum.SHA1;
import static org.onosproject.lisp.msg.authentication.LispAuthenticationKeyEnum.SHA256;
import static org.onosproject.lisp.msg.authentication.LispAuthenticationKeyEnum.UNKNOWN;

/**
 * Test case for LISP authentication.
 */
public class LispAuthenticationTest {

    private LispAuthenticationFactory factory;

    @Before
    public void setup() {
        factory = LispAuthenticationFactory.getInstance();
    }

    @Test
    public void testAuthData() {

        String authKey = "testKey";
        byte[] noneAuthData = factory.createAuthenticationData(NONE, authKey, new byte[0]);
        byte[] unknownAuthData = factory.createAuthenticationData(UNKNOWN, authKey, new byte[0]);
        byte[] sha1AuthData = factory.createAuthenticationData(SHA1, authKey, new byte[0]);
        byte[] sha256AuthData = factory.createAuthenticationData(SHA256, authKey, new byte[0]);

        assertThat(noneAuthData, is(new byte[0]));
        assertThat(unknownAuthData, is(new byte[0]));
        assertThat(sha1AuthData.length, is(20));
        assertThat(sha256AuthData.length, is(32));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidAuthType() {
        LispAuthenticationKeyEnum authType = LispAuthenticationKeyEnum.valueOf((short) 0);
        LispMacAuthentication macAuth = new LispMacAuthentication(authType);

        macAuth.getAuthenticationData("onos", new byte[0]);
    }

    @Test(expected = NullPointerException.class)
    public void testNullAuthKey() {
        LispAuthenticationKeyEnum authType = LispAuthenticationKeyEnum.valueOf((short) 1);

        LispMacAuthentication macAuth = new LispMacAuthentication(authType);
        macAuth.getAuthenticationData(null, new byte[0]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidAuthKey() {
        LispAuthenticationKeyEnum authType = LispAuthenticationKeyEnum.valueOf((short) 1);

        LispMacAuthentication macAuth = new LispMacAuthentication(authType);
        macAuth.getAuthenticationData("", new byte[0]);
    }
}
